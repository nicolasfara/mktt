@file:OptIn(ExperimentalUuidApi::class)

package it.nicolasfarabegoli.mktt

import arrow.fx.coroutines.CountDownLatch
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
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        mqttClient.subscribe("test/topic")
        mqttClient.disconnect()
    }

    @Test
    fun `The client should subscribe to a topic and start collecting the messages`() = runTest {
        val latch = CountDownLatch(1)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val client = MkttClient(dispatcher, connectionConfiguration)
        val messageCount = 5
        client.connect()
        val topicName = Uuid.random().toString()
        val flow = client.subscribe(topicName)
        backgroundScope.launch {
            latch.await()
            for (index in 0 until messageCount) {
                client.publish(
                    topic = topicName,
                    message = "test message -- $index".encodeToByteArray(),
                    qos = MqttQoS.AtLeastOnce,
                )
            }
        }
        latch.countDown()
        flow.take(messageCount / 2).collect {
            assertContains(it.payload.decodeToString(), "test message")
        }
        client.disconnect()
    }
}
