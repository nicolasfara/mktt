package it.nicolasfarabegoli.mktt

/**
 * Configures an MQTT client for connecting the a [brokerUrl] with the given [clientId], [username], [password].
 * The [keepAliveInterval] is the interval in seconds between the client pings to the broker.
 * The [cleanSession] flag indicates if the client should start a new session or resume an existing one.
 */
data class MqttClientConfiguration(
    val brokerUrl: String,
    val port: Int = 1883,
    val clientId: String = "",
    val username: String? = null,
    val password: String? = null,
    val cleanSession: Boolean = true,
    val keepAliveInterval: Long = 60,
    val will: MqttWill? = null,
    val automaticReconnect: Boolean = true,
    val connectionTimeout: Long = 30,
)

/**
 * Configuration scope for the [MqttClientConfiguration].
 */
class MqttClientConfigurationScope {
    /**
     * The URL of the broker to connect to.
     *
     * This property must be set otherwise an exception will be thrown when building the configuration.
     */
    var brokerUrl: String = ""
        set(value) {
            brokerSet = true
            field = value
        }
    private var brokerSet = false

    /**
     * The port of the broker to connect to.
     */
    var port: Int = 1883

    /**
     * The client ID to use when connecting to the broker.
     */
    var clientId: String = ""

    /**
     * The username and password to use when connecting to the broker.
     */
    var username: String? = null

    /**
     * The password to use when connecting to the broker.
     */
    var password: String? = null

    /**
     * The interval in seconds between the client pings to the broker.
     */
    var keepAliveInterval: Long = 60

    /**
     * The flag indicating if the client should start a new session or resume an existing one.
     */
    var cleanSession: Boolean = true

    /**
     * The MQTT Last Will and Testament.
     */
    var will: MqttWill? = null

    /**
     * The flag indicating if the client should automatically reconnect to the broker.
     */
    var automaticReconnect: Boolean = true

    /**
     * The connection timeout in seconds.
     */
    var connectionTimeout: Long = 30

    internal fun build(): MqttClientConfiguration {
        require(brokerSet) { "The broker URL must be set" }
        return MqttClientConfiguration(
            brokerUrl = brokerUrl,
            port = port,
            clientId = clientId,
            username = username,
            password = password,
            keepAliveInterval = keepAliveInterval,
            cleanSession = cleanSession,
            will = will,
            automaticReconnect = automaticReconnect,
            connectionTimeout = connectionTimeout,
        )
    }
}
