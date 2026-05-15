package io.github.nicolasfara.mktt.engine

import io.ktor.network.sockets.SocketOptions
import io.ktor.network.tls.TLSConfigBuilder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Engine configuration used by [io.github.nicolasfara.mktt.engine.DefaultEngine].
 *
 * @property host target MQTT broker host name.
 * @property port target MQTT broker TCP port.
 */
class DefaultEngineConfig(val host: String, val port: Int) : MqttEngineConfig() {
    internal var tlsConfigBuilder: TLSConfigBuilder? = null
    internal var tcpOptions: (SocketOptions.TCPClientSocketOptions.() -> Unit) = { }

    /**
     * Time before a TCP connection attempt times out.
     *
     * This is exposed separately because Ktor does not provide a TCP socket option for connection timeout. See
     * [KTOR-5064](https://youtrack.jetbrains.com/issue/KTOR-5064/).
     */
    var connectionTimeout: Duration = 10.seconds

    /**
     * Add TLS configuration for this client. Just use `tls { }` to enable TLS support.
     *
     * @param init TLS configuration block.
     */
    fun tls(init: TLSConfigBuilder.() -> Unit = {}) {
        tlsConfigBuilder = TLSConfigBuilder().also(init)
    }

    /**
     * Configure the TCP options for this client.
     *
     * @param init TCP socket options block.
     * @see SocketOptions.TCPClientSocketOptions
     */
    fun tcp(init: SocketOptions.TCPClientSocketOptions.() -> Unit = {}) {
        tcpOptions = init
    }
}
