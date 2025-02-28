package it.nicolasfarabegoli.mktt

import com.hivemq.client.mqtt.exceptions.ConnectionFailedException
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MqttClientTestJvm {
    @Test
    fun `The client should raise an ConnectionFailedException exception when connecting to an invalid broker in JVM`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val mqttClient = MqttClient(dispatcher) {
                brokerUrl = "invalid.broker.com"
            }
            assertFailsWith<ConnectionFailedException> {
                mqttClient.connect()
            }
        }
}
