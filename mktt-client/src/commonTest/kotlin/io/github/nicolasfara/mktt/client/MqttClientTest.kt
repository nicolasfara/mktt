package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.ConnectionException
import io.github.nicolasfara.mktt.core.GrantedQoS0
import io.github.nicolasfara.mktt.core.HandshakeFailedException
import io.github.nicolasfara.mktt.core.MaximumQoS
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.ReceiveMaximum
import io.github.nicolasfara.mktt.core.ServerKeepAlive
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.TimeoutException
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import io.github.nicolasfara.mktt.core.packet.Connack
import io.github.nicolasfara.mktt.core.packet.Connect
import io.github.nicolasfara.mktt.core.packet.Pingreq
import io.github.nicolasfara.mktt.core.packet.Pingresp
import io.github.nicolasfara.mktt.core.packet.Puback
import io.github.nicolasfara.mktt.core.packet.Publish
import io.github.nicolasfara.mktt.core.packet.Suback
import io.github.nicolasfara.mktt.core.packet.Subscribe
import io.github.nicolasfara.mktt.core.packet.Unsuback
import io.github.nicolasfara.mktt.core.packet.Unsubscribe
import io.github.nicolasfara.mktt.core.packet.hasFailure
import io.github.nicolasfara.mktt.core.packet.isUnsubscribed
import io.github.nicolasfara.mktt.engine.MqttEngine
import io.github.nicolasfara.mktt.engine.MqttEngineConfig
import io.github.nicolasfara.mktt.engine.MqttEngineFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.io.bytestring.ByteString

@OptIn(ExperimentalCoroutinesApi::class)
class MqttClientTest {
    @Test
    fun `connect returns connack and updates connection state`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val engine = FakeMqttEngine(dispatcher).apply {
            sendHandler = { packet ->
                sentPackets += packet
                if (packet is Connect) {
                    emit(
                        Connack(
                            isSessionPresent = false,
                            reason = Success,
                        ),
                    )
                }
                Result.success(Unit)
            }
        }
        val client = createClient(engine)

        val connack = client.connect()
        try {
            assertEquals(Success, connack.reason)
            assertEquals(
                MqttConnectionState.Connected(connack),
                client.connectionState.value,
            )
        } finally {
            withContext(NonCancellable) {
                client.disconnect()
            }
            client.close()
        }
    }

    @Test
    fun `connect failure exposes connection error state`() = runTest {
        val engine = FakeMqttEngine(UnconfinedTestDispatcher(testScheduler)).apply {
            startHandler =
                {
                    Result.failure(
                        ConnectionException("cannot connect"),
                    )
                }
        }
        val client = createClient(engine)

        assertFailsWith<ConnectionException> {
            client.connect()
        }
        assertIs<MqttConnectionState.ConnectionError>(client.connectionState.value)
        client.close()
    }

    @Test
    fun `publish qos1 succeeds when ack arrives during send`() = runTest {
        val engine = FakeMqttEngine(UnconfinedTestDispatcher(testScheduler)).apply {
            sendHandler = { packet ->
                sentPackets += packet
                when (packet) {
                    is Connect -> emit(
                        Connack(
                            isSessionPresent = false,
                            reason = Success,
                        ),
                    )

                    is Publish -> emit(
                        Puback.from(packet),
                    )
                }
                Result.success(Unit)
            }
        }
        val client = createClient(engine)
        client.connect()
        try {
            val result = client.publish(
                PublishRequest("sensor/temperature") {
                    desiredQoS = QoS.AT_LEAST_ONCE
                    payload("21")
                },
            )

            assertIs<AtLeastOncePublishResponse>(result)
            assertEquals(Success, result.reason)
        } finally {
            withContext(NonCancellable) {
                client.disconnect()
            }
            client.close()
        }
    }

    @Test
    fun `connect does not reuse stale connack from a previous attempt`() = runTest {
        val engine = FakeMqttEngine(UnconfinedTestDispatcher(testScheduler)).apply {
            var connackCount = 0
            sendHandler = { packet ->
                sentPackets += packet
                if (packet is Connect && connackCount++ == 0) {
                    emit(
                        Connack(
                            isSessionPresent = false,
                            reason = Success,
                        ),
                    )
                }
                Result.success(Unit)
            }
        }
        val client = createClient(engine) {
            ackMessageTimeout = 100.milliseconds
        }

        client.connect()

        val failure = backgroundScope.async {
            assertFailsWith<TimeoutException> {
                client.connect()
            }
        }
        advanceTimeBy(100.milliseconds)
        runCurrent()

        assertIs<TimeoutException>(failure.await())
        assertIs<MqttConnectionState.ConnectionError>(client.connectionState.value)
        client.close()
    }

    @Test
    fun `publish downgrades qos to broker maximum`() = runTest {
        val engine = FakeMqttEngine(UnconfinedTestDispatcher(testScheduler)).apply {
            sendHandler = { packet ->
                sentPackets += packet
                if (packet is Connect) {
                    emit(
                        Connack(
                            isSessionPresent = false,
                            reason = Success,
                            maximumQoS = MaximumQoS(0),
                        ),
                    )
                }
                Result.success(Unit)
            }
        }
        val client = createClient(engine)

        client.connect()
        try {
            val result = client.publish(
                PublishRequest("sensor/temperature") {
                    desiredQoS = QoS.AT_LEAST_ONCE
                    payload("21")
                },
            )

            assertIs<AtMostOncePublishResponse>(result)
            assertEquals(QoS.AT_MOST_ONCE, result.qoS)
        } finally {
            withContext(NonCancellable) {
                client.disconnect()
            }
            client.close()
        }
    }

    @Test
    fun `publish timeout disconnects client and allows reconnect`() = runTest {
        val engine = FakeMqttEngine(UnconfinedTestDispatcher(testScheduler)).apply {
            sendHandler = { packet ->
                sentPackets += packet
                when (packet) {
                    is Connect -> emit(
                        Connack(
                            isSessionPresent = false,
                            reason = Success,
                            receiveMaximum = ReceiveMaximum(1u),
                        ),
                    )

                    is Publish -> {
                        if (packet.topic.name == "sensor/retry") {
                            emit(Puback.from(packet))
                        }
                    }
                }
                Result.success(Unit)
            }
        }
        val client = createClient(engine) {
            ackMessageTimeout = 100.milliseconds
        }

        client.connect()

        val failure = backgroundScope.async {
            assertFailsWith<HandshakeFailedException> {
                client.publish(
                    PublishRequest("sensor/timeout") {
                        desiredQoS = QoS.AT_LEAST_ONCE
                        payload("stuck")
                    },
                )
            }
        }
        advanceTimeBy(100.milliseconds)
        runCurrent()

        assertIs<HandshakeFailedException>(failure.await())
        assertFalse(engine.connected.value)
        assertEquals(MqttConnectionState.Disconnected, client.connectionState.value)

        client.connect()
        val result = client.publish(
            PublishRequest("sensor/retry") {
                desiredQoS = QoS.AT_LEAST_ONCE
                payload("ok")
            },
        )

        assertIs<AtLeastOncePublishResponse>(result)
        client.close()
    }

    @Test
    fun `messages flow filters incoming publishes locally`() = runTest {
        val engine = FakeMqttEngine(UnconfinedTestDispatcher(testScheduler)).apply {
            sendHandler = { packet ->
                sentPackets += packet
                if (packet is Connect) {
                    emit(
                        Connack(
                            isSessionPresent = false,
                            reason = Success,
                        ),
                    )
                }
                Result.success(Unit)
            }
        }
        val client = createClient(engine)
        client.connect()
        try {
            val message = backgroundScope.async {
                client.messages(
                    TopicFilter(
                        Topic(
                            "sensors/+",
                        ),
                    ),
                ).first()
            }
            runCurrent()

            engine.emit(
                Publish(
                    topic = Topic("sensors/temperature"),
                    payload = ByteString("22".encodeToByteArray()),
                ),
            )

            assertEquals("22", message.await().payloadAsString())
        } finally {
            withContext(NonCancellable) {
                client.disconnect()
            }
            client.close()
        }
    }

    @Test
    fun `subscribe and unsubscribe return acknowledgements`() = runTest {
        val engine = FakeMqttEngine(UnconfinedTestDispatcher(testScheduler)).apply {
            sendHandler = { packet ->
                sentPackets += packet
                when (packet) {
                    is Connect -> emit(
                        Connack(
                            isSessionPresent = false,
                            reason = Success,
                        ),
                    )

                    is Subscribe -> emit(
                        Suback(
                            packet.packetIdentifier,
                            listOf(GrantedQoS0),
                        ),
                    )

                    is Unsubscribe -> emit(
                        Unsuback(
                            packet.packetIdentifier,
                            listOf(Success),
                        ),
                    )
                }
                Result.success(Unit)
            }
        }
        val client = createClient(engine)
        val filter = TopicFilter(
            Topic("sensors/+"),
        )
        client.connect()
        try {
            val suback = client.subscribe(listOf(filter))
            val unsuback = client.unsubscribe(listOf(filter))

            assertTrue(!suback.hasFailure)
            assertTrue(unsuback.isUnsubscribed)
        } finally {
            withContext(NonCancellable) {
                client.disconnect()
            }
            client.close()
        }
    }

    @Test
    fun `keep alive sends ping requests with virtual time`() = runTest {
        val engine = FakeMqttEngine(UnconfinedTestDispatcher(testScheduler)).apply {
            sendHandler = { packet ->
                sentPackets += packet
                when (packet) {
                    is Connect -> emit(
                        Connack(
                            isSessionPresent = false,
                            reason = Success,
                            serverKeepAlive = ServerKeepAlive(1u),
                        ),
                    )

                    is Pingreq -> emit(
                        Pingresp,
                    )
                }
                Result.success(Unit)
            }
        }
        val client = createClient(engine)
        client.connect()
        try {
            advanceTimeBy(1_000.milliseconds)
            runCurrent()

            assertTrue(engine.sentPackets.any { it is Pingreq })
        } finally {
            withContext(NonCancellable) {
                client.disconnect()
            }
            client.close()
        }
    }

    private fun createClient(
        engine: FakeMqttEngine,
        init: MqttClientConfigBuilder<MqttEngineConfig>.() -> Unit = {},
    ): MqttClient {
        val config = buildConfig(TestEngineFactory(engine)) {
            clientId = "test-client"
            init()
        }
        return DefaultMqttClient(config)
    }

    private class TestEngineFactory(private val engine: FakeMqttEngine) :
        MqttEngineFactory<MqttEngineConfig> {
        override fun create(block: MqttEngineConfig.() -> Unit): MqttEngine = engine
    }
}
