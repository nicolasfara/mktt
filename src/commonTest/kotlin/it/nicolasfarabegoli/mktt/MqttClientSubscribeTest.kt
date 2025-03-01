@file:OptIn(ExperimentalUuidApi::class)

package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttTestConfiguration.connectionConfiguration
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class MqttClientSubscribeTest {
    @Test
    @Ignore
    fun `The client should subscribe successfully to a topic`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val mqttClient = MkttClient(dispatcher, connectionConfiguration)
            mqttClient.connect()
            mqttClient.subscribe("test/topic")
            mqttClient.disconnect()
        }

    @Test
    fun `The client should subscribe to a topic and start collecting the messages`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val sendClient = MkttClient(dispatcher, connectionConfiguration)
            val receiveClient = MkttClient(dispatcher, connectionConfiguration)
            val messageCount = 5
            sendClient.connect()
            receiveClient.connect()
            val topicName = Uuid.random().toString()
            val flow = receiveClient.subscribe(topicName)
            backgroundScope.launch {
                for (index in 0 until messageCount) {
                    sendClient.publish(
                        topic = topicName,
                        message = "test message -- $index".encodeToByteArray(),
                        qos = MqttQoS.AtLeastOnce,
                    )
                }
            }
            flow.take(messageCount).onEach { println(it) }.collect {
                assertContains(it.payload.decodeToString(), "test message")
            }
            receiveClient.disconnect()
            sendClient.disconnect()
        }
}
