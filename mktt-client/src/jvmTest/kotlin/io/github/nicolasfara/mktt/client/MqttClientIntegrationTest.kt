package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class MqttClientIntegrationTest {
    @Test
    fun `publish and receive over local mosquitto`() = runTest {
        withContext(Dispatchers.Default.limitedParallelism(2)) {
            val container = startContainerOrSkip() ?: return@withContext
            val dispatcher = Dispatchers.Default.limitedParallelism(2)
            try {
                val topic = "mktt/integration/${System.nanoTime()}"
                val publisher = newClient(container, "publisher", dispatcher)
                val subscriber = newClient(container, "subscriber", dispatcher)

                subscriber.connect()
                subscriber.subscribe(
                    listOf(
                        TopicFilter(
                            Topic(topic),
                        ),
                    ),
                )

                publisher.connect()
                publisher.publish(
                    PublishRequest(topic) {
                        desiredQoS = QoS.AT_LEAST_ONCE
                        payload("hello")
                    },
                )

                val message = subscriber.messages(
                    TopicFilter(
                        Topic(topic),
                    ),
                ).first()
                assertEquals("hello", message.payloadAsString())

                publisher.disconnect()
                subscriber.disconnect()
            } finally {
                container.stop()
            }
        }
    }

    @Test
    fun `connect over tls to local mosquitto`() = runTest {
        withContext(Dispatchers.Default.limitedParallelism(1)) {
            val container = startContainerOrSkip() ?: return@withContext
            val dispatcher = Dispatchers.Default.limitedParallelism(1)
            try {
                val client =
                    MqttClient(container.host, container.tlsPort) {
                        this.dispatcher = dispatcher
                        ackMessageTimeout = 20.seconds
                        clientId = "tls-client"
                        username = MosquittoContainer.USER
                        password = MosquittoContainer.PASSWORD
                        connection {
                            tls {
                                trustManager = NoTrustManager
                            }
                        }
                    }

                val connack = client.connect()

                assertEquals(Success, connack.reason)
                client.disconnect()
            } finally {
                container.stop()
            }
        }
    }

    private fun newClient(
        container: MosquittoContainer,
        clientId: String,
        dispatcher: CoroutineDispatcher,
    ): MqttClient = MqttClient(container.host, container.defaultPort) {
        this.dispatcher = dispatcher
        ackMessageTimeout = 20.seconds
        this.clientId = clientId
        username = MosquittoContainer.USER
        password = MosquittoContainer.PASSWORD
    }

    private fun startContainerOrSkip(): MosquittoContainer? {
        if (System.getenv("SKIP_INTEGRATION_TEST") == "true") {
            return null
        }
        return try {
            MosquittoContainer().also { it.start() }
        } catch (_: Throwable) {
            null
        }
    }
}
