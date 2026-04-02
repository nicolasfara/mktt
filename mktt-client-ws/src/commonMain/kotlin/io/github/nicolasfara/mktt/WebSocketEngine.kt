package io.github.nicolasfara.mktt

import io.github.nicolasfara.mktt.core.ConnectionException
import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.github.nicolasfara.mktt.core.packet.Packet
import io.github.nicolasfara.mktt.core.packet.readPacket
import io.github.nicolasfara.mktt.core.packet.write
import io.github.nicolasfara.mktt.core.util.Logger
import io.github.nicolasfara.mktt.engine.MqttEngine
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.URLProtocol
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.writeFully
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer

internal class WebSocketEngine(
    private val config: WebSocketEngineConfig,
    override val dispatcher: CoroutineDispatcher,
    replay: Int = 0,
) : MqttEngine {
    private val client = config.http()

    private val _packetResults = MutableSharedFlow<Result<Packet>>(replay = replay)
    override val packetResults = _packetResults.asSharedFlow()

    private val _connected = MutableStateFlow(false)
    override val connected = _connected.asStateFlow()

    private val scope = CoroutineScope(dispatcher)
    private var receiverJob: Job? = null
    private var wsSession: DefaultClientWebSocketSession? = null

    init {
        val errorMessage =
            "No WebSockets plugin installed in ${client.engine::class.simpleName}, consider using 'install(WebSockets)'"
        check(client.pluginOrNull(WebSockets) == null) {
            errorMessage
        }
    }

    override suspend fun start(): Result<Unit> = try {
        if (!config.url.user.isNullOrBlank() || !config.url.password.isNullOrBlank()) {
            Logger.w { "Username/password encoded in URL cannot be used in websocket connections" }
        }
        wsSession = client.webSocketSession(
            method = HttpMethod.Get,
            host = config.url.host,
            port = config.url.port,
            path = config.url.encodedPath,
        ) {
            url.protocol = when (val protocol = config.url.protocol) {
                URLProtocol.WS, URLProtocol.HTTP -> URLProtocol.WS
                URLProtocol.WSS, URLProtocol.HTTPS -> URLProtocol.WSS
                else -> {
                    throw IllegalArgumentException("Unexpected web socket protocol: $protocol (use http(s) or ws(s))")
                }
            }
            headers[HttpHeaders.SecWebSocketProtocol] = "mqtt"
        }.also {
            _connected.value = true
            receiverJob = scope.launch {
                incomingMessagesLoop(it)
            }
        }
        Result.success(Unit)
    } catch (ex: CancellationException) {
        throw ex
    } catch (ex: Exception) {
        Result.failure(ConnectionException("Cannot connect to ${config.url}", ex))
    }

    override suspend fun send(packet: Packet): Result<Unit> =
        wsSession?.let { doSend(it, packet) }
            ?: Result.failure(ConnectionException("Not connected to ${config.url}"))

    override suspend fun disconnect() {
        _connected.value = false
        wsSession?.close()
        receiverJob?.cancel()
    }

    override fun close() {
        client.close()
    }

    private suspend fun doSend(session: DefaultClientWebSocketSession, packet: Packet): Result<Unit> = try {
        Logger.d { "Sending packet: $packet" }
        with(Buffer()) {
            write(packet)
            if (size <= session.maxFrameSize) {
                session.outgoing.send(Frame.Binary(fin = true, packet = this))
            } else {
                val frame = Buffer()
                while (size > 0) {
                    readAtMostTo(frame, size.coerceAtMost(session.maxFrameSize))
                    session.outgoing.send(Frame.Binary(fin = true, packet = frame))
                }
            }
        }
        Result.success(Unit)
    } catch (ex: CancellationException) {
        throw ex
    }
    catch (ex: ClosedSendChannelException) {
        Logger.w(ex) { "Unexpected exception while sending packet: $ex" }
        Result.failure(ex)
    }

    private suspend fun incomingMessagesLoop(session: DefaultClientWebSocketSession) = coroutineScope {
        val channel = ByteChannel(autoFlush = true)
        val reader = launch {
            while (!channel.isClosedForRead) {
                val result = try {
                    Result.success(channel.readPacket())
                } catch (ex: CancellationException) {
                    throw ex
                } catch (ex: MalformedPacketException) {
                    Result.failure(ex)
                }
                _packetResults.emit(result)
            }
        }
        try {
            for (frame in session.incoming) {
                when (frame) {
                    is Frame.Binary -> {
                        Logger.v { "Received incoming packet of size: ${frame.data.size}" }
                        channel.writeFully(frame.readBytes())
                    }
                    else -> {
                        // TODO: Close the network connection when receiving a non-binary frame [MQTT-6.0.0-1]
                        Logger.e { "Received unexpected frame type: $frame" }
                    }
                }
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (_: ClosedReceiveChannelException) {
            Logger.d { "WebSocket incoming channel closed" }
        } finally {
            withContext(NonCancellable) {
                disconnect()
                reader.cancel()
            }
        }
    }
}
