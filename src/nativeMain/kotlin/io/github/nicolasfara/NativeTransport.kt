package io.github.nicolasfara

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.errors.PosixException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

private const val DEFAULT_CONNECT_RETRY_ATTEMPTS = 3
private const val DEFAULT_CONNECT_RETRY_INITIAL_DELAY_MS = 100L
private const val DEFAULT_CONNECT_RETRY_MAX_DELAY_MS = 1_000L
private const val DEFAULT_RECONNECT_INITIAL_DELAY_MS = 500L
private const val DEFAULT_RECONNECT_MAX_DELAY_MS = 10_000L
private const val DEFAULT_ACK_TIMEOUT_MS = 10_000L

internal data class NativeMkttClientTiming(
    val connectRetryAttempts: Int = DEFAULT_CONNECT_RETRY_ATTEMPTS,
    val connectRetryInitialDelayMs: Long = DEFAULT_CONNECT_RETRY_INITIAL_DELAY_MS,
    val connectRetryMaxDelayMs: Long = DEFAULT_CONNECT_RETRY_MAX_DELAY_MS,
    val reconnectInitialDelayMs: Long = DEFAULT_RECONNECT_INITIAL_DELAY_MS,
    val reconnectMaxDelayMs: Long = DEFAULT_RECONNECT_MAX_DELAY_MS,
    val ackTimeoutMs: Long = DEFAULT_ACK_TIMEOUT_MS,
)

internal fun interface NativeTransportFactory {
    suspend fun open(configuration: MqttClientConfiguration, ioDispatcher: CoroutineDispatcher): NativeTransportSession
}

internal interface NativeTransportSession {
    val readChannel: ByteReadChannel
    val writeChannel: ByteWriteChannel

    suspend fun close()
}

internal val DEFAULT_TRANSIENT_FAILURE_DETECTOR: (Throwable) -> Boolean = { error ->
    error is PosixException.TryAgainException
}

internal object KtorNativeTransportFactory : NativeTransportFactory {
    override suspend fun open(
        configuration: MqttClientConfiguration,
        ioDispatcher: CoroutineDispatcher,
    ): NativeTransportSession = withContext(ioDispatcher) {
        val selectorManager = SelectorManager(ioDispatcher)
        var socket: Socket? = null
        recoverNonCancellation(
            block = {
                val rawSocket = aSocket(selectorManager).tcp().connect(
                    hostname = configuration.brokerUrl,
                    port = configuration.port,
                )
                socket = if (configuration.ssl) rawSocket.tls(coroutineContext) else rawSocket
                val connectedSocket = requireNotNull(socket)
                KtorNativeTransportSession(
                    selectorManager = selectorManager,
                    socket = connectedSocket,
                    readChannel = connectedSocket.openReadChannel(),
                    writeChannel = connectedSocket.openWriteChannel(autoFlush = false),
                )
            },
            onFailure = { error ->
                ignoreNonCancellation { socket?.close() }
                selectorManager.close()
                throw error
            },
        )
    }
}

private class KtorNativeTransportSession(
    private val selectorManager: SelectorManager,
    private val socket: Socket,
    override val readChannel: ByteReadChannel,
    override val writeChannel: ByteWriteChannel,
) : NativeTransportSession {
    override suspend fun close() {
        socket.close()
        selectorManager.close()
    }
}
