package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.GrantedQoS0
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import kotlin.coroutines.ContinuationInterceptor
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class RemoteBrokerIntegrationTest {
    @Test
    fun `connects to public test broker and exchanges a message`() = runTest {
        val dispatcher = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
        val topicName = "mktt/remote-test/${Random.nextLong().toString(16)}"
        val filter = TopicFilter(
            Topic(topicName),
        )
        val subscriber =
            MqttClient(TEST_BROKER_HOST, TEST_BROKER_PORT) {
                this.dispatcher = dispatcher
                clientId = "mktt-remote-sub-${Random.nextLong().toString(16)}"
            }
        val publisher =
            MqttClient(TEST_BROKER_HOST, TEST_BROKER_PORT) {
                this.dispatcher = dispatcher
                clientId = "mktt-remote-pub-${Random.nextLong().toString(16)}"
            }

        try {
            assertEquals(Success, subscriber.connect().reason)
            assertEquals(Success, publisher.connect().reason)
            assertEquals(
                GrantedQoS0,
                subscriber.subscribe(listOf(filter)).reasons.single(),
            )

            val message =
                async(start = CoroutineStart.UNDISPATCHED) {
                    withTimeout(30.seconds) {
                        subscriber.messages(filter).first()
                    }
                }

            publisher.publish(
                PublishRequest(topicName) {
                    desiredQoS = QoS.AT_LEAST_ONCE
                    payload("hello-from-remote-broker")
                },
            )

            assertEquals("hello-from-remote-broker", message.await().payloadAsString())
        } finally {
            withContext(NonCancellable) {
                disconnectSafely(publisher)
                disconnectSafely(subscriber)
            }
            publisher.close()
            subscriber.close()
        }
    }

    private suspend fun disconnectSafely(client: MqttClient) {
        try {
            client.disconnect()
        } catch (ex: CancellationException) {
            throw ex
        } catch (_: Exception) {
            // Ignore cleanup failures in integration tests.
        }
    }

    private companion object {
        private const val TEST_BROKER_HOST = "test.mosquitto.org"
        private const val TEST_BROKER_PORT = 1883
    }
}
