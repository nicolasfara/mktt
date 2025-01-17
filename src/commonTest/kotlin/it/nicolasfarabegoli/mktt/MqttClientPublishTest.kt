package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.topic.MqttTopic.Companion.asTopic
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MqttClientPublishTest {
    @Test
    @Ignore
    fun `The client should publish successfully a message`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
        assertEquals(mqttClient.connect().reasonCode, MqttConnAckReasonCode.Success)
        val messageResult = mqttClient.publish(
            "hello world".encodeToByteArray(),
            "test/topic".asTopic(),
            qoS = MqttQoS.ExactlyOnce,
        )
        assertNull(messageResult.error)
        assertEquals("test/topic".asTopic(), messageResult.publish.topic)
        assertEquals(MqttQoS.ExactlyOnce, messageResult.publish.qos)
        assertContentEquals("hello world".encodeToByteArray(), messageResult.publish.payload)
        mqttClient.disconnect()
    }

    @Test
    @Ignore
    fun `When multiple messages will be sent, a flow containing the successfully sent message should be returned`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
            val messages = (0..9).map {
                MqttPublish(
                    topic = "test/topic".asTopic(),
                    payload = byteArrayOf(it.toByte()),
                    qos = MqttQoS.ExactlyOnce,
                )
            }.asFlow()
            assertEquals(mqttClient.connect().reasonCode, MqttConnAckReasonCode.Success)
            val responses = mqttClient.publish(messages).toList()
            assertEquals(10, responses.size)
            (0..9).map {
                assertContentEquals(byteArrayOf(it.toByte()), responses[it].publish.payload)
            }
            mqttClient.disconnect()
        }

    @Test
    @Ignore
    fun `The client should fail with an exception when publishing and the client is not connected`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
        assertFailsWith<Exception> {
            mqttClient.publish(byteArrayOf(0x00), "test/topic".asTopic(), qoS = MqttQoS.ExactlyOnce)
        }
    }
}
