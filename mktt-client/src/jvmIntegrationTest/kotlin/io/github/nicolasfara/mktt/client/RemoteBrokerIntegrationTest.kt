package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.GrantedQoS0
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class RemoteBrokerIntegrationTest {
    @Test
    fun connectsToPublicTestBrokerAndExchangesMessage() = runBlocking {
        if (!shouldRunRemoteBrokerTests()) {
            return@runBlocking
        }

        withTimeout(REMOTE_TEST_TIMEOUT) {
            val dispatcher = createDispatcher()
            val topicName = "mktt/remote-test/${Random.nextLong().toString(HEX_RADIX)}"
            val filter = TopicFilter(
                Topic(topicName),
            )
            val subscriber =
                MqttClient(TEST_BROKER_HOST, TEST_BROKER_PORT, dispatcher) {
                    ackMessageTimeout = ACK_MESSAGE_TIMEOUT
                    clientId = "mktt-remote-sub-${Random.nextLong().toString(HEX_RADIX)}"
                }
            val publisher =
                MqttClient(TEST_BROKER_HOST, TEST_BROKER_PORT, dispatcher) {
                    ackMessageTimeout = ACK_MESSAGE_TIMEOUT
                    clientId = "mktt-remote-pub-${Random.nextLong().toString(HEX_RADIX)}"
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
                        withTimeout(MESSAGE_WAIT_TIMEOUT) {
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
                dispatcher.close()
            }
        }
    }

    private fun createDispatcher(): ExecutorCoroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()

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
        private const val RUN_REMOTE_BROKER_TESTS_ENV = "MKTT_RUN_REMOTE_BROKER_TESTS"
        private const val TEST_BROKER_HOST = "test.mosquitto.org"
        private const val TEST_BROKER_PORT = 1883
        private const val HEX_RADIX = 16

        private val ACK_MESSAGE_TIMEOUT = 20.seconds
        private val REMOTE_TEST_TIMEOUT = 60.seconds
        private val MESSAGE_WAIT_TIMEOUT = 30.seconds

        private fun shouldRunRemoteBrokerTests(): Boolean {
            val envValue = System.getenv(RUN_REMOTE_BROKER_TESTS_ENV)?.lowercase()
            return envValue == "1" || envValue == "true" || envValue == "yes"
        }
    }
}
