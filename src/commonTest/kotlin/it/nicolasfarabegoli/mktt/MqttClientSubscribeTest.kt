@file:OptIn(ExperimentalUuidApi::class)

package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttTestConfiguration.connectionConfiguration
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MqttClientSubscribeTest {
    @Test
    fun `The client should subscribe successfully to a topic`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        mqttClient.subscribe("test/topic")
        mqttClient.disconnect()
    }

    @Test
    fun `The client should subscribe to a topic and start collecting the messages`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val sendClient = MqttClient(dispatcher, connectionConfiguration)
        val receiveClient = MqttClient(dispatcher, connectionConfiguration)
        val messageCount = 5
        sendClient.connect()
        receiveClient.connect()
        val topicName = Uuid.random().toString()
        backgroundScope.launch {
            for (index in 0 until messageCount) {
                sendClient.publish(
                    topic = topicName,
                    message = "test message $index".encodeToByteArray(),
                    qos = MqttQoS.AtMostOnce,
                )
            }
        }
        val flow = receiveClient.subscribe(topicName)
        flow.take(messageCount).collect {
            assertContains(it.payload.decodeToString(), "test message")
        }
        receiveClient.disconnect()
        sendClient.disconnect()
    }
}
