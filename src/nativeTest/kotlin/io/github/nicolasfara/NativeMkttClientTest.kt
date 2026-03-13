package io.github.nicolasfara

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val TEST_MQTT_CONNECT = 0x10
private const val TEST_MQTT_CONNACK = 0x20
private const val TEST_MQTT_PUBLISH = 0x30
private const val TEST_MQTT_PUBREC = 0x50
private const val TEST_MQTT_PUBREL = 0x62
private const val TEST_MQTT_PUBCOMP = 0x70
private const val TEST_MQTT_SUBSCRIBE = 0x82
private const val TEST_MQTT_SUBACK = 0x90
private const val TEST_MQTT_DISCONNECT = 0xe0

private class TransientConnectFailure : RuntimeException("EAGAIN")

class NativeMkttClientTest {
    @Test
    fun `connect retries transient failure and eventually succeeds`() = runTest {
        val scriptedSession = ScriptedSession { fromClient, toClient ->
            val connect = readPacket(fromClient)
            assertEquals(TEST_MQTT_CONNECT, connect.fixedHeader)
            writePacket(toClient, TEST_MQTT_CONNACK, byteArrayOf(0x00, 0x00))

            val disconnect = readPacket(fromClient)
            assertEquals(TEST_MQTT_DISCONNECT, disconnect.fixedHeader)
        }

        val factory = QueueTransportFactory(
            listOf(
                { throw TransientConnectFailure() },
                { scriptedSession.open() },
            ),
        )

        val client = NativeMkttClient(
            dispatcher = Dispatchers.Default,
            configuration = testConfiguration(automaticReconnect = false),
            transportFactory = factory,
            ioDispatcher = Dispatchers.Default,
            timing = NativeMkttClientTiming(
                connectRetryAttempts = 3,
                connectRetryInitialDelayMs = 1,
                connectRetryMaxDelayMs = 2,
                reconnectInitialDelayMs = 1,
                reconnectMaxDelayMs = 2,
                ackTimeoutMs = 1_000,
            ),
            isTransientFailure = { it is TransientConnectFailure },
        )

        client.connect()
        assertEquals(2, factory.openCount)
        assertIs<MqttConnectionState.Connected>(client.connectionState.value)

        client.disconnect()
        scriptedSession.await()
    }

    @Test
    fun `qos2 publish completes pubrec pubrel pubcomp handshake`() = runTest {
        val scriptedSession = ScriptedSession { fromClient, toClient ->
            val connect = readPacket(fromClient)
            assertEquals(TEST_MQTT_CONNECT, connect.fixedHeader)
            writePacket(toClient, TEST_MQTT_CONNACK, byteArrayOf(0x00, 0x00))

            val publish = readPacket(fromClient)
            assertEquals(TEST_MQTT_PUBLISH, publish.fixedHeader and 0xF0)
            val qosBits = (publish.fixedHeader shr 1) and 0x03
            assertEquals(MqttQoS.ExactlyOnce.code, qosBits)

            val publishPacketId = readPublishPacketId(publish.payload)
            writePacket(toClient, TEST_MQTT_PUBREC, uInt16ToBytes(publishPacketId))

            val pubrel = readPacket(fromClient)
            assertEquals(TEST_MQTT_PUBREL, pubrel.fixedHeader)
            assertEquals(publishPacketId, readUInt16(pubrel.payload, 0))
            writePacket(toClient, TEST_MQTT_PUBCOMP, uInt16ToBytes(publishPacketId))

            val disconnect = readPacket(fromClient)
            assertEquals(TEST_MQTT_DISCONNECT, disconnect.fixedHeader)
        }

        val client = NativeMkttClient(
            dispatcher = Dispatchers.Default,
            configuration = testConfiguration(automaticReconnect = false),
            transportFactory = QueueTransportFactory(listOf { scriptedSession.open() }),
            ioDispatcher = Dispatchers.Default,
            timing = NativeMkttClientTiming(ackTimeoutMs = 1_000),
        )

        client.connect()
        client.publish("test/topic", "hello".encodeToByteArray(), MqttQoS.ExactlyOnce)
        client.disconnect()

        scriptedSession.await()
    }

    @Test
    fun `unexpected disconnect reconnects and re-subscribes active topics`() = runTest {
        val topic = "reconnect/topic"
        val firstSubscribeSeen = CompletableDeferred<Unit>()
        val secondSubscribeSeen = CompletableDeferred<Unit>()

        val firstSession = ScriptedSession { fromClient, toClient ->
            val connect = readPacket(fromClient)
            assertEquals(TEST_MQTT_CONNECT, connect.fixedHeader)
            writePacket(toClient, TEST_MQTT_CONNACK, byteArrayOf(0x00, 0x00))

            val subscribe = readPacket(fromClient)
            assertEquals(TEST_MQTT_SUBSCRIBE, subscribe.fixedHeader)
            assertEquals(topic, readSubscribeTopic(subscribe.payload))
            writePacket(
                toClient,
                TEST_MQTT_SUBACK,
                uInt16ToBytes(readUInt16(subscribe.payload, 0)) + byteArrayOf(MqttQoS.AtMostOnce.code.toByte()),
            )
            firstSubscribeSeen.complete(Unit)

            delay(50)
            toClient.close()
        }

        val secondSession = ScriptedSession { fromClient, toClient ->
            val connect = readPacket(fromClient)
            assertEquals(TEST_MQTT_CONNECT, connect.fixedHeader)
            writePacket(toClient, TEST_MQTT_CONNACK, byteArrayOf(0x00, 0x00))

            val subscribe = readPacket(fromClient)
            assertEquals(TEST_MQTT_SUBSCRIBE, subscribe.fixedHeader)
            assertEquals(topic, readSubscribeTopic(subscribe.payload))
            writePacket(
                toClient,
                TEST_MQTT_SUBACK,
                uInt16ToBytes(readUInt16(subscribe.payload, 0)) + byteArrayOf(MqttQoS.AtMostOnce.code.toByte()),
            )
            secondSubscribeSeen.complete(Unit)

            writePacket(
                toClient,
                TEST_MQTT_PUBLISH,
                encodeUtf8WithLength(topic) + "after-reconnect".encodeToByteArray(),
            )

            val disconnect = readPacket(fromClient)
            assertEquals(TEST_MQTT_DISCONNECT, disconnect.fixedHeader)
        }

        val factory = QueueTransportFactory(listOf({ firstSession.open() }, { secondSession.open() }))

        val client = NativeMkttClient(
            dispatcher = Dispatchers.Default,
            configuration = testConfiguration(automaticReconnect = true),
            transportFactory = factory,
            ioDispatcher = Dispatchers.Default,
            timing = NativeMkttClientTiming(
                connectRetryAttempts = 1,
                reconnectInitialDelayMs = 10,
                reconnectMaxDelayMs = 20,
                ackTimeoutMs = 1_000,
            ),
        )

        client.connect()
        val receivedMessage = CompletableDeferred<MqttMessage>()
        val collectingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val collectingJob = collectingScope.launch {
            client.subscribe(topic, MqttQoS.AtMostOnce).collect {
                if (!receivedMessage.isCompleted) {
                    receivedMessage.complete(it)
                }
            }
        }

        try {
            withRealTimeout(3_000) {
                firstSubscribeSeen.await()
                secondSubscribeSeen.await()
                val message = receivedMessage.await()
                assertEquals(topic, message.topic)
                assertEquals("after-reconnect", message.payloadAsString())
            }
        } catch (error: TimeoutCancellationException) {
            error(
                "Timeout waiting for reconnect flow: openCount=${factory.openCount}, " +
                    "firstSubscribe=${firstSubscribeSeen.isCompleted}, " +
                    "secondSubscribe=${secondSubscribeSeen.isCompleted}, " +
                    "message=${receivedMessage.isCompleted}",
            )
        }

        assertEquals(2, factory.openCount)
        assertIs<MqttConnectionState.Connected>(client.connectionState.value)

        client.disconnect()
        collectingJob.cancelAndJoin()
        collectingScope.cancel()

        firstSession.await()
        secondSession.await()
    }
}

private data class Packet(
    val fixedHeader: Int,
    val payload: ByteArray,
)

private class QueueTransportFactory(
    private val opens: List<suspend () -> NativeTransportSession>,
) : NativeTransportFactory {
    var openCount: Int = 0
        private set

    override suspend fun open(
        configuration: MqttClientConfiguration,
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    ): NativeTransportSession {
        check(openCount < opens.size) { "No more scripted transport sessions available" }
        return opens[openCount++].invoke()
    }
}

private class ScriptedSession(
    private val script: suspend (fromClient: ByteReadChannel, toClient: ByteChannel) -> Unit,
) {
    private val completion = CompletableDeferred<Unit>()

    fun open(): NativeTransportSession {
        val inbound = ByteChannel(autoFlush = true)
        val outbound = ByteChannel(autoFlush = true)
        val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

        scope.launch {
            runCatching {
                script(outbound, inbound)
            }.onSuccess {
                completion.complete(Unit)
            }.onFailure {
                completion.completeExceptionally(it)
            }
            inbound.close()
            outbound.close()
        }

        return object : NativeTransportSession {
            override val readChannel: ByteReadChannel = inbound
            override val writeChannel: ByteWriteChannel = outbound

            override suspend fun close() {
                scope.cancel()
                inbound.close()
                outbound.close()
            }
        }
    }

    suspend fun await() {
        withRealTimeout(3_000) {
            completion.await()
        }
    }
}

private suspend fun <T> withRealTimeout(timeoutMs: Long, block: suspend () -> T): T =
    withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(timeoutMs) {
            block()
        }
    }

private fun testConfiguration(automaticReconnect: Boolean): MqttClientConfiguration = MqttClientConfiguration(
    brokerUrl = "scripted-broker",
    port = 1883,
    clientId = "native-test-${Random.nextInt()}",
    keepAliveInterval = 0,
    automaticReconnect = automaticReconnect,
    connectionTimeout = 1,
)

private suspend fun readPacket(channel: ByteReadChannel): Packet {
    val header = channel.readOneByte()
    val remainingLength = readRemainingLength(channel)
    val payload = ByteArray(remainingLength)
    if (remainingLength > 0) {
        channel.readFully(payload, 0, remainingLength)
    }
    return Packet(header, payload)
}

private suspend fun writePacket(channel: ByteWriteChannel, fixedHeader: Int, payload: ByteArray = ByteArray(0)) {
    val out = mutableListOf<Byte>()
    out.add((fixedHeader and 0xFF).toByte())

    var remaining = payload.size
    do {
        var digit = remaining % 128
        remaining /= 128
        if (remaining > 0) {
            digit = digit or 0x80
        }
        out.add(digit.toByte())
    } while (remaining > 0)

    out.addAll(payload.toList())

    channel.writeFully(out.toByteArray())
    channel.flush()
}

private suspend fun ByteReadChannel.readOneByte(): Int {
    val data = ByteArray(1)
    readFully(data, 0, 1)
    return data[0].toInt() and 0xFF
}

private suspend fun readRemainingLength(channel: ByteReadChannel): Int {
    var multiplier = 1
    var value = 0

    do {
        val digit = channel.readOneByte()
        value += (digit and 0x7F) * multiplier
        multiplier *= 128
        check(multiplier <= 128 * 128 * 128 * 128) { "Malformed remaining length" }
    } while ((digit and 0x80) != 0)

    return value
}

private fun readPublishPacketId(payload: ByteArray): Int {
    val topicLength = readUInt16(payload, 0)
    val packetIdOffset = 2 + topicLength
    return readUInt16(payload, packetIdOffset)
}

private fun readSubscribeTopic(payload: ByteArray): String {
    val topicLength = readUInt16(payload, 2)
    val topicStart = 4
    val topicEnd = topicStart + topicLength
    return payload.decodeToString(topicStart, topicEnd)
}

private fun readUInt16(payload: ByteArray, offset: Int): Int =
    ((payload[offset].toInt() and 0xFF) shl 8) or (payload[offset + 1].toInt() and 0xFF)

private fun uInt16ToBytes(value: Int): ByteArray =
    byteArrayOf((value shr 8 and 0xFF).toByte(), (value and 0xFF).toByte())

private fun encodeUtf8WithLength(text: String): ByteArray {
    val bytes = text.encodeToByteArray()
    return uInt16ToBytes(bytes.size) + bytes
}
