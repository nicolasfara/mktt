package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.topic.MqttTopic.Companion.asTopic
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MqttClientSubscribeTest {
    @Test
    @Ignore
    fun `The client should subscribe successfully to a topic`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
        assertEquals(MqttConnAckReasonCode.Success, mqttClient.connect().reasonCode)
        val filterTopic = MqttTopicFilter.of("test/topic")
        mqttClient.subscribe(filterTopic)
    }

    @Test
    @Ignore
    @OptIn(ExperimentalUuidApi::class)
    fun `The client should subscribe to a topic and start collecting the messages`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sendClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
        val receiveClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
        assertEquals(MqttConnAckReasonCode.Success, sendClient.connect().reasonCode)
        assertEquals(MqttConnAckReasonCode.Success, receiveClient.connect().reasonCode)
        val topicName = Uuid.random().toString()
        val filterTopic = MqttTopicFilter.of(topicName)
        backgroundScope.launch {
            for (index in 0..100) {
                val message = MqttPublish(
                    topic = topicName.asTopic(),
                    payload = "test message $index".encodeToByteArray(),
                    qos = MqttQoS.ExactlyOnce,
                )
                assertNull(sendClient.publish(message).error)
            }
        }
        receiveClient.subscribe(filterTopic).take(1).collect {
            assertEquals("test message 0", it.payload?.decodeToString())
        }
    }
}
