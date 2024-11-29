package it.nicolasfarabegoli.mktt.configuration

/**
 * Represents the configuration of an MQTT client.
 *
 * The [hostname] property indicates the hostname of the MQTT broker.
 * The [port] property indicates the port of the MQTT broker.
 * The [clientId] property indicates the client identifier.
 * The [username] property indicates the username for the connection.
 * The [password] property indicates the password for the connection.
 * The [cleanSession] property indicates if the session should be cleaned.
 * The [keepAliveInterval] property indicates the keep alive interval.
 * The [connectionTimeout] property indicates the connection timeout.
 * The [maxInFlight] property indicates the maximum number of in-flight messages.
 */
data class MqttConfiguration(
    val hostname: String = "localhost",
    val port: Int = 1883,
    val clientId: String = "mktt",
    val username: String = "user",
    val password: String = "password",
    val cleanSession: Boolean = true,
    val keepAliveInterval: Int = 60,
    val connectionTimeout: Int = 30,
    val maxInFlight: Int = 10,
)
