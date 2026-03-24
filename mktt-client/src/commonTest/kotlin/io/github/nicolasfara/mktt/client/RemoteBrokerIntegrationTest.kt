package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.GrantedQoS0
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class RemoteBrokerIntegrationTest {
    @Test
    fun `connects to public test broker and exchanges a message`() = runBlocking {
        if (!isRemoteBrokerTestEnabled()) {
            return@runBlocking
        }

        val topicName = "mktt/remote-test/${Random.nextLong().toString(16)}"
        val filter = _root_ide_package_.io.github.nicolasfara.mktt.core.TopicFilter(
            _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(topicName),
        )
        val subscriber =
            _root_ide_package_.io.github.nicolasfara.mktt.client.MqttClient(TEST_BROKER_HOST, TEST_BROKER_PORT) {
                clientId = "mktt-remote-sub-${Random.nextLong().toString(16)}"
            }
        val publisher =
            _root_ide_package_.io.github.nicolasfara.mktt.client.MqttClient(TEST_BROKER_HOST, TEST_BROKER_PORT) {
                clientId = "mktt-remote-pub-${Random.nextLong().toString(16)}"
            }

        try {
            assertEquals(_root_ide_package_.io.github.nicolasfara.mktt.core.Success, subscriber.connect().reason)
            assertEquals(_root_ide_package_.io.github.nicolasfara.mktt.core.Success, publisher.connect().reason)
            assertEquals(
                _root_ide_package_.io.github.nicolasfara.mktt.core.GrantedQoS0,
                subscriber.subscribe(listOf(filter)).reasons.single(),
            )

            val message =
                async<io.github.nicolasfara.mktt.client.MqttPublishMessage>(start = CoroutineStart.UNDISPATCHED) {
                    withTimeout(30.seconds) {
                        subscriber.messages(filter).first()
                    }
                }

            publisher.publish(
                _root_ide_package_.io.github.nicolasfara.mktt.client.PublishRequest(topicName) {
                    desiredQoS = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_LEAST_ONCE
                    payload("hello-from-remote-broker")
                },
            )

            assertEquals("hello-from-remote-broker", message.await().payloadAsString())
        } finally {
            runCatching { publisher.disconnect() }
            runCatching { subscriber.disconnect() }
            publisher.close()
            subscriber.close()
        }
    }

    private companion object {
        private const val TEST_BROKER_HOST = "test.mosquitto.org"
        private const val TEST_BROKER_PORT = 1883
    }
}
