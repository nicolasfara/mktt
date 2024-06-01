package it.nicolasfarabegoli.mktt.configuration

data class MqttConfiguration(
    val hostname: String = "localhost",
    val port: Int = 1883,
    val clientId: String = "mktt",
    val username: String = "",
    val password: String = "",
    val cleanSession: Boolean = true,
    val keepAliveInterval: Int = 60,
    val connectionTimeout: Int = 30,
    val maxInFlight: Int = 10,
)
