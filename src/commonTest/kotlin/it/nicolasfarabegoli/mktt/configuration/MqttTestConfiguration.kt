package it.nicolasfarabegoli.mktt.configuration

import it.nicolasfarabegoli.mktt.MqttClientConfigurationScope
import kotlin.random.Random

/**
 * Represents the test configuration for the MQTT client.
 */
object MqttTestConfiguration {
    /**
     * The URL of the broker to connect to.
     */
    const val BROKER = "test.mosquitto.org"

    /**
     * The wrong port to connect to the broker.
     */
    const val WRONG_PORT = 5555

    val connectionConfiguration: MqttClientConfigurationScope.() -> Unit = {
        brokerUrl = BROKER
        clientId = "mktt-test-client${Random.nextInt()}"
    }
    val wrongConnectionConfiguration: MqttClientConfigurationScope.() -> Unit = {
        brokerUrl = "invalid.broker.com"
        automaticReconnect = false
    }
    val invalidPortConnectionConfiguration: MqttClientConfigurationScope.() -> Unit = {
        brokerUrl = BROKER
        port = WRONG_PORT
        automaticReconnect = false
    }
}
