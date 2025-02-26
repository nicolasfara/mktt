package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.topic.MqttTopic.Companion.asTopic
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MqttClientPublishTest {
    @Test
    fun `The client should publish successfully a message`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
        assertEquals(mqttClient.connect().reasonCode, MqttConnAckReasonCode.Success)
        mqttClient.publish(
            "hello world".encodeToByteArray(),
            "test/topic".asTopic(),
            qoS = MqttQoS.ExactlyOnce,
        )
        mqttClient.disconnect()
    }

    @Test
    fun `When multiple messages will be sent, a flow containing the successfully sent message should be returned`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
            val messages = (0..9).map {
                MqttPublish(
                    topic = "test/topic".asTopic(),
                    payload = it.toString().encodeToByteArray(),
                    qos = MqttQoS.ExactlyOnce,
                )
            }.asFlow()
            assertEquals(mqttClient.connect().reasonCode, MqttConnAckReasonCode.Success)
            mqttClient.publish(messages)
            mqttClient.disconnect()
        }

    @Test
    fun `The client should fail with an exception when publishing and the client is not connected`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
        assertFailsWith<Exception> {
            mqttClient.publish(byteArrayOf(0x00), "test/topic".asTopic(), qoS = MqttQoS.ExactlyOnce)
        }
    }
}
