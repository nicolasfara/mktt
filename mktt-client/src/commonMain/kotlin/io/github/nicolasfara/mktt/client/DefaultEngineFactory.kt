package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.*
import io.ktor.network.sockets.*
import io.ktor.network.tls.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class DefaultEngineFactory(private val host: String, private val port: Int) :
    io.github.nicolasfara.mktt.client.MqttEngineFactory<io.github.nicolasfara.mktt.client.DefaultEngineConfig> {

    override fun create(
        block: io.github.nicolasfara.mktt.client.DefaultEngineConfig.() -> Unit,
    ): io.github.nicolasfara.mktt.client.MqttEngine =
        _root_ide_package_.io.github.nicolasfara.mktt.client.DefaultEngine(
            _root_ide_package_.io.github.nicolasfara.mktt.client.DefaultEngineConfig(
                host,
                port,
            ).apply(block),
        )
}

public class DefaultEngineConfig(public val host: String, public val port: Int) :
    io.github.nicolasfara.mktt.client.MqttEngineConfig() {
    internal var tlsConfigBuilder: TLSConfigBuilder? = null
    internal var tcpOptions: (SocketOptions.TCPClientSocketOptions.() -> Unit) = { }

    /**
     * The time before a connection request times out. Ktor doesn't provide an option to specify the connection timeout
     * in the TCP settings (see https://youtrack.jetbrains.com/issue/KTOR-5064/), hence we use this extra parameter.
     */
    public var connectionTimeout: Duration = 10.seconds

    /**
     * Add TLS configuration for this client. Just use `tls { }` to enable TLS support.
     */
    public fun tls(init: TLSConfigBuilder.() -> Unit) {
        tlsConfigBuilder = TLSConfigBuilder().also(init)
    }

    /**
     * Configure the TCP options for this client
     *
     * @see SocketOptions.TCPClientSocketOptions
     */
    public fun tcp(init: SocketOptions.TCPClientSocketOptions.() -> Unit) {
        tcpOptions = init
    }
}
