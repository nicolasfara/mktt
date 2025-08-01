@file:OptIn(ExperimentalUuidApi::class)

package io.github.nicolasfara

import arrow.fx.coroutines.CountDownLatch
import io.github.nicolasfara.configuration.MqttTestConfiguration.connectionConfiguration
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertSame
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

    @Test
    fun `Two subscriptions to the same topic should return the same flow instance`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val client = MkttClient(dispatcher, connectionConfiguration)
        client.connect()
        val topicName = Uuid.random().toString()
        val flow1 = client.subscribe(topicName)
        val flow2 = client.subscribe(topicName)
        assertSame(flow1, flow2)
        client.disconnect()
    }
}
