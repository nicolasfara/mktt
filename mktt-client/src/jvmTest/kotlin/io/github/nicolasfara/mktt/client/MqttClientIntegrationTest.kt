package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MqttClientIntegrationTest {
    @Test
    fun `publish and receive over local mosquitto`() = runTest {
        val container = startContainerOrSkip() ?: return@runTest
        try {
            val topic = "mktt/integration/${System.nanoTime()}"
            val publisher = newClient(container, "publisher")
            val subscriber = newClient(container, "subscriber")

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

    @Test
    fun `connect over tls to local mosquitto`() = runTest {
        val container = startContainerOrSkip() ?: return@runTest
        try {
            val client =
                MqttClient(container.host, container.tlsPort) {
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

    private fun newClient(container: MosquittoContainer, clientId: String): MqttClient =
        MqttClient(container.host, container.defaultPort) {
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
