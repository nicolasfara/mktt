package io.github.nicolasfara

import arrow.fx.coroutines.CountDownLatch
import com.hivemq.client.mqtt.exceptions.ConnectionFailedException
import io.github.nicolasfara.configuration.MqttTestConfiguration.connectionConfiguration
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MqttClientTestJvm {
    @Test
    fun `The client should raise an ConnectionFailedException exception when connecting to an invalid broker in JVM`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val mqttClient =
                MkttClient(dispatcher) {
                    brokerUrl = "invalid.broker.com"
                    automaticReconnect = false
                }
            assertFailsWith<ConnectionFailedException> {
                mqttClient.connect()
            }
        }

    @Test
    fun `No exception should be thrown when cancel a publishing`() = runTest {
        val latch = CountDownLatch(5)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        val topicName = "test/topic"
        val job = backgroundScope.launch {
            for (i in 0..10) {
                mqttClient.publish(topicName, "test message $i".encodeToByteArray())
                latch.countDown()
            }
        }
        latch.await()
        job.cancelAndJoin()
        mqttClient.disconnect()
    }
}
