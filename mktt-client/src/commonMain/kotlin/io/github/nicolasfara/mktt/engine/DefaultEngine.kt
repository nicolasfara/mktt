package io.github.nicolasfara.mktt.engine

import io.github.nicolasfara.mktt.core.ConnectionException
import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.github.nicolasfara.mktt.core.packet.Packet
import io.github.nicolasfara.mktt.core.packet.readPacket
import io.github.nicolasfara.mktt.core.packet.write
import io.github.nicolasfara.mktt.core.util.Logger
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.ClosedWriteChannelException
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.io.EOFException

/**
 * @property config the engine config
 * @param socketHandler use a [io.github.nicolasfara.mktt.client.SocketHandler] other than
 *   the default one, mainly used for testing
 * @param replay the size of the replay cache for [packetResults], mainly used for testing
 */
internal class DefaultEngine(
    private val config: DefaultEngineConfig,
    override val dispatcher: CoroutineDispatcher,
    socketHandler: SocketHandler? = null,
    replay: Int = 0,
) : MqttEngine {

    private val _packetResults = MutableSharedFlow<Result<Packet>>(replay = replay)
    override val packetResults = _packetResults.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    override val connected = _connected.asStateFlow()

    private val socketHandler = socketHandler ?: SocketHandlerImpl(dispatcher)

    private val writeMutex = Mutex()

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private var sendChannel: ByteWriteChannel? = null

    private var socket: Closeable? = null

    private var receiverJob: Job? = null

    override suspend fun start(): Result<Unit> = try {
        socket = scope.async {
            val socket = withTimeout(config.connectionTimeout) {
                socketHandler.openSocket(config)
            }
            _connected.value = true
            socket
        }.await().also { socket ->
            sendChannel = socket.openWriteChannel()
            // Open the read channel before launching the reader job to surface setup failures.
            val readChannel = socket.openReadChannel()
            receiverJob = scope.launch {
                readChannel.incomingMessageLoop()
            }
        }
        Result.success(Unit)
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: ClosedWriteChannelException) {
        Result.failure(
            ConnectionException(
                "Cannot connect to ${config.host}:${config.port}",
                ex,
            ),
        )
    } catch (ex: IllegalStateException) {
        Result.failure(
            ConnectionException(
                "Cannot connect to ${config.host}:${config.port}",
                ex,
            ),
        )
    }

    override suspend fun send(packet: Packet): Result<Unit> = sendChannel?.doSend(packet)
        ?: Result.failure(
            ConnectionException(
                "Not connected to ${config.host}:${config.port}",
            ),
        )

    override suspend fun disconnect() {
        socket?.let {
            socket = null
            it.close()
        }
        disconnected()
    }

    override fun close() {
        socketHandler.close()
        scope.cancel()
    }

    override fun toString(): String = "DefaultMqttEngine[${config.host}:${config.port}]"

    // --- Private methods ---------------------------------------------------------------------------------------------

    private suspend fun ByteReadChannel.incomingMessageLoop() {
        try {
            while (!isClosedForRead) {
                val shouldTerminate = try {
                    _packetResults.emit(Result.success(readPacket()))
                    false
                } catch (_: ClosedReceiveChannelException) {
                    Logger.v {
                        "Read channel has been closed, terminating..."
                    }
                    true
                } catch (_: EOFException) {
                    Logger.v {
                        "End of stream detected, terminating..."
                    }
                    true
                } catch (ex: MalformedPacketException) {
                    // Continue with the loop, so that the client can decide what to do.
                    _packetResults.emit(Result.failure(ex))
                    false
                }
                if (shouldTerminate) {
                    break
                }
            }
        } finally {
            Logger.d { "Incoming message loop terminated" }
            withContext(NonCancellable) {
                disconnected()
            }
        }
    }

    private suspend fun ByteWriteChannel.doSend(packet: Packet): Result<Unit> {
        Logger.d { "Sending $packet..." }

        return try {
            writeMutex.withLock {
                write(packet)
                flush()
            }
            Result.success(Unit)
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: ClosedWriteChannelException) {
            Logger.w(throwable = ex) {
                "Write channel has been closed"
            }
            disconnected()
            Result.failure(ex)
        } catch (ex: IllegalStateException) {
            Logger.w(throwable = ex) { "Write channel error detected" }
            disconnected()
            Result.failure(ex)
        }
    }

    private suspend fun disconnected() {
        _connected.value = false
        receiverJob?.cancel()
        socket?.close()
        sendChannel?.flushAndClose()

        receiverJob = null
        socket = null
        sendChannel = null
    }

    private inner class SocketHandlerImpl(val dispatcher: CoroutineDispatcher) : SocketHandler {

        private val selectorManager = SelectorManager(dispatcher)

        override suspend fun openSocket(config: DefaultEngineConfig): Socket = with(config) {
            val tlsConfig = tlsConfigBuilder?.build()
            if (tlsConfig != null) {
                // We must provide our own exception handler for the TLS connection,
                // otherwise errors (which might happen
                // due to an already closed connection) will get propagated
                // to the parent's coroutine, which is not what
                // we want.
                val handler = CoroutineExceptionHandler { _, exception ->
                    if (connected.value) {
                        Logger.e(throwable = exception) {
                            "TLS error while connected to $host:$port, disconnecting..."
                        }
                        scope.launch {
                            disconnect()
                        }
                    }
                    // When not connected, ignore this exception, as it is a
                    // result of being disconnected.
                }
                val tlsContext = CoroutineName("TLS Handler") + dispatcher + handler

                aSocket(selectorManager).tcp().connect(host, port, tcpOptions)
                    .tls(tlsContext, tlsConfig)
            } else {
                aSocket(selectorManager).tcp().connect(host, port, tcpOptions)
            }
        }

        override fun close() {
            selectorManager.close()
        }
    }
}

/**
 * Mainly used for mocking socket connection failures.
 */
internal interface SocketHandler {

    suspend fun openSocket(config: DefaultEngineConfig): Socket

    fun close()
}
