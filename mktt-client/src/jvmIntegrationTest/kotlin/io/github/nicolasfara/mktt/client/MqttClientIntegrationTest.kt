package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

internal class MqttClientIntegrationTest {
    @Test
    fun publishAndReceiveOverLocalMosquitto() = runTest {
        val container = LocalBrokerSupport.startBrokerOrSkip() ?: return@runTest
        val dispatcher = createDispatcher(PUBLISH_RECEIVE_PARALLELISM)
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
            dispatcher.close()
            container.stop()
        }
    }

    @Test
    fun connectOverTlsToLocalMosquitto() = runTest {
        val container = LocalBrokerSupport.startBrokerOrSkip() ?: return@runTest
        val dispatcher = createDispatcher(TLS_CONNECT_PARALLELISM)
        try {
            val client =
                MqttClient(container.host, container.tlsPort) {
                    this.dispatcher = dispatcher
                    ackMessageTimeout = ACK_MESSAGE_TIMEOUT
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
            dispatcher.close()
            container.stop()
        }
    }

    private fun createDispatcher(parallelism: Int): ExecutorCoroutineDispatcher =
        Executors.newFixedThreadPool(parallelism).asCoroutineDispatcher()

    private fun newClient(
        container: MosquittoContainer,
        clientId: String,
        dispatcher: CoroutineDispatcher,
    ): MqttClient = MqttClient(container.host, container.defaultPort) {
        this.dispatcher = dispatcher
        ackMessageTimeout = ACK_MESSAGE_TIMEOUT
        this.clientId = clientId
        username = MosquittoContainer.USER
        password = MosquittoContainer.PASSWORD
    }

    private companion object {
        private val ACK_MESSAGE_TIMEOUT = 20.seconds
        private const val PUBLISH_RECEIVE_PARALLELISM = 2
        private const val TLS_CONNECT_PARALLELISM = 1
    }
}
