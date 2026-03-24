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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

private const val TEST_MQTT_CONNECT = 0x10
private const val TEST_MQTT_CONNACK = 0x20
private const val TEST_MQTT_PUBLISH = 0x30
private const val TEST_MQTT_PUBREC = 0x50
private const val TEST_MQTT_PUBREL = 0x62
private const val TEST_MQTT_PUBCOMP = 0x70
private const val TEST_MQTT_SUBSCRIBE = 0x82
private const val TEST_MQTT_SUBACK = 0x90
private const val TEST_MQTT_UNSUBSCRIBE = 0xa2
private const val TEST_MQTT_UNSUBACK = 0xb0
private const val TEST_MQTT_DISCONNECT = 0xe0
private const val TEST_MQTT_PROTOCOL_NAME = "MQTT"
private const val TEST_MQTT_PROTOCOL_LEVEL = 0x05
private const val TEST_PACKET_TYPE_MASK = 0xF0
private const val TEST_PUBLISH_QOS_SHIFT = 1
private const val TEST_PUBLISH_QOS_MASK = 0x03
private const val TEST_CONNECT_FLAG_CLEAN_START = 0x02

private class TransientConnectFailure : RuntimeException("EAGAIN")

class NativeMkttClientTest {
    @Test
    fun `connect retries transient failure and eventually succeeds`() = runTest {
        val scriptedSession = ScriptedSession { fromClient, toClient ->
            val connect = readPacket(fromClient)
            assertEquals(TEST_MQTT_CONNECT, connect.fixedHeader)
            assertMqtt5Connect(connect.payload)
            writePacket(toClient, TEST_MQTT_CONNACK, encodeConnAck())

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
    fun `publish while disconnected fails`() = runTest {
        val client = NativeMkttClient(
            dispatcher = Dispatchers.Default,
            configuration = testConfiguration(automaticReconnect = false),
            transportFactory = QueueTransportFactory(emptyList()),
            ioDispatcher = Dispatchers.Default,
        )

        assertFailsWith<Throwable> {
            client.publish("test/topic", "hello".encodeToByteArray(), MqttQoS.AtLeastOnce)
        }
    }

    @Test
    fun `qos2 publish completes pubrec pubrel pubcomp handshake`() = runTest {
        val scriptedSession = ScriptedSession { fromClient, toClient ->
            val connect = readPacket(fromClient)
            assertEquals(TEST_MQTT_CONNECT, connect.fixedHeader)
            assertMqtt5Connect(connect.payload)
            writePacket(toClient, TEST_MQTT_CONNACK, encodeConnAck())

            val publish = readPacket(fromClient)
            assertEquals(TEST_MQTT_PUBLISH, publish.fixedHeader and TEST_PACKET_TYPE_MASK)
            val qosBits = (publish.fixedHeader shr TEST_PUBLISH_QOS_SHIFT) and TEST_PUBLISH_QOS_MASK
            assertEquals(MqttQoS.ExactlyOnce.code, qosBits)
            assertEquals(0, readPublishPropertiesLength(publish.payload))

            val publishPacketId = readPublishPacketId(publish.payload)
            writePacket(toClient, TEST_MQTT_PUBREC, encodeAckPacket(publishPacketId))

            val pubrel = readPacket(fromClient)
            assertEquals(TEST_MQTT_PUBREL, pubrel.fixedHeader)
            assertAckPacket(pubrel.payload, publishPacketId)
            writePacket(toClient, TEST_MQTT_PUBCOMP, encodeAckPacket(publishPacketId))

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
            assertMqtt5Connect(connect.payload)
            writePacket(toClient, TEST_MQTT_CONNACK, encodeConnAck())

            val subscribe = readPacket(fromClient)
            assertEquals(TEST_MQTT_SUBSCRIBE, subscribe.fixedHeader)
            assertEquals(topic, readSubscribeTopic(subscribe.payload))
            writePacket(toClient, TEST_MQTT_SUBACK, encodeSubAck(readUInt16(subscribe.payload, 0), MqttQoS.AtMostOnce))
            firstSubscribeSeen.complete(Unit)

            delay(50)
            toClient.close()
        }

        val secondSession = ScriptedSession { fromClient, toClient ->
            val connect = readPacket(fromClient)
            assertEquals(TEST_MQTT_CONNECT, connect.fixedHeader)
            assertMqtt5Connect(connect.payload)
            writePacket(toClient, TEST_MQTT_CONNACK, encodeConnAck())

            val subscribe = readPacket(fromClient)
            assertEquals(TEST_MQTT_SUBSCRIBE, subscribe.fixedHeader)
            assertEquals(topic, readSubscribeTopic(subscribe.payload))
            writePacket(toClient, TEST_MQTT_SUBACK, encodeSubAck(readUInt16(subscribe.payload, 0), MqttQoS.AtMostOnce))
            secondSubscribeSeen.complete(Unit)

            delay(10)
            writePacket(toClient, TEST_MQTT_PUBLISH, encodePublishPayload(topic, "after-reconnect".encodeToByteArray()))

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

    @Test
    fun `unsubscribe clears cached flow and a new subscribe creates a new flow`() = runTest {
        val topic = "cache/topic"
        val scriptedSession = ScriptedSession { fromClient, toClient ->
            val connect = readPacket(fromClient)
            assertEquals(TEST_MQTT_CONNECT, connect.fixedHeader)
            assertMqtt5Connect(connect.payload)
            writePacket(toClient, TEST_MQTT_CONNACK, encodeConnAck())

            val firstSubscribe = readPacket(fromClient)
            assertEquals(TEST_MQTT_SUBSCRIBE, firstSubscribe.fixedHeader)
            assertEquals(topic, readSubscribeTopic(firstSubscribe.payload))
            writePacket(toClient, TEST_MQTT_SUBACK, encodeSubAck(readUInt16(firstSubscribe.payload, 0), MqttQoS.AtMostOnce))
            delay(10)
            writePacket(toClient, TEST_MQTT_PUBLISH, encodePublishPayload(topic, "first".encodeToByteArray()))

            val unsubscribe = readPacket(fromClient)
            assertEquals(TEST_MQTT_UNSUBSCRIBE, unsubscribe.fixedHeader)
            assertEquals(topic, readUnsubscribeTopic(unsubscribe.payload))
            writePacket(toClient, TEST_MQTT_UNSUBACK, encodeUnsubAck(readUInt16(unsubscribe.payload, 0)))

            val secondSubscribe = readPacket(fromClient)
            assertEquals(TEST_MQTT_SUBSCRIBE, secondSubscribe.fixedHeader)
            assertEquals(topic, readSubscribeTopic(secondSubscribe.payload))
            writePacket(toClient, TEST_MQTT_SUBACK, encodeSubAck(readUInt16(secondSubscribe.payload, 0), MqttQoS.AtMostOnce))
            delay(10)
            writePacket(toClient, TEST_MQTT_PUBLISH, encodePublishPayload(topic, "second".encodeToByteArray()))

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

        val flow1 = client.subscribe(topic, MqttQoS.AtMostOnce)
        val flow2 = client.subscribe(topic, MqttQoS.AtMostOnce)
        assertSame(flow1, flow2)

        val firstMessage = withRealTimeout(3_000) { flow1.first() }
        assertEquals("first", firstMessage.payloadAsString())

        client.unsubscribe(topic)

        val flow3 = client.subscribe(topic, MqttQoS.AtMostOnce)
        assertNotSame(flow1, flow3)

        val secondMessage = withRealTimeout(3_000) { flow3.first() }
        assertEquals("second", secondMessage.payloadAsString())

        client.disconnect()
        scriptedSession.await()
    }

    @Test
    fun `wildcard subscriptions only emit matching messages`() = runTest {
        val filter = "wild/+/sensor"
        val nonMatchingTopic = "wild/device/status"
        val matchingTopic = "wild/device/sensor"

        val scriptedSession = ScriptedSession { fromClient, toClient ->
            val connect = readPacket(fromClient)
            assertEquals(TEST_MQTT_CONNECT, connect.fixedHeader)
            assertMqtt5Connect(connect.payload)
            writePacket(toClient, TEST_MQTT_CONNACK, encodeConnAck())

            val subscribe = readPacket(fromClient)
            assertEquals(TEST_MQTT_SUBSCRIBE, subscribe.fixedHeader)
            assertEquals(filter, readSubscribeTopic(subscribe.payload))
            writePacket(toClient, TEST_MQTT_SUBACK, encodeSubAck(readUInt16(subscribe.payload, 0), MqttQoS.AtMostOnce))

            delay(10)
            writePacket(toClient, TEST_MQTT_PUBLISH, encodePublishPayload(nonMatchingTopic, "ignore".encodeToByteArray()))
            writePacket(toClient, TEST_MQTT_PUBLISH, encodePublishPayload(matchingTopic, "match".encodeToByteArray()))

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
        val message = withRealTimeout(3_000) { client.subscribe(filter, MqttQoS.AtMostOnce).first() }
        assertEquals(matchingTopic, message.topic)
        assertEquals("match", message.payloadAsString())

        client.disconnect()
        scriptedSession.await()
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

    @Suppress("UNUSED_PARAMETER")
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

private fun assertMqtt5Connect(payload: ByteArray) {
    var offset = 0
    val protocolName = readUtf8(payload, offset)
    offset += 2 + protocolName.length
    val protocolLevel = payload[offset].toInt() and 0xFF
    offset += 1
    val flags = payload[offset].toInt() and 0xFF
    offset += 1
    offset += 2
    val (propertiesLength, nextOffset) = readVariableByteInteger(payload, offset)

    assertEquals(TEST_MQTT_PROTOCOL_NAME, protocolName)
    assertEquals(TEST_MQTT_PROTOCOL_LEVEL, protocolLevel)
    assertEquals(TEST_CONNECT_FLAG_CLEAN_START, flags and TEST_CONNECT_FLAG_CLEAN_START)
    assertEquals(0, propertiesLength)
    check(nextOffset <= payload.size) { "Malformed CONNECT payload" }
}

private fun encodeConnAck(reasonCode: Int = 0x00, sessionPresent: Boolean = false): ByteArray =
    byteArrayOf(if (sessionPresent) 0x01 else 0x00, reasonCode.toByte(), 0x00)

private fun encodeAckPacket(packetId: Int, reasonCode: Int = 0x00): ByteArray =
    uInt16ToBytes(packetId) + byteArrayOf(reasonCode.toByte(), 0x00)

private fun encodeSubAck(packetId: Int, qos: MqttQoS): ByteArray =
    uInt16ToBytes(packetId) + byteArrayOf(0x00, qos.code.toByte())

private fun encodeUnsubAck(packetId: Int, reasonCode: Int = 0x00): ByteArray =
    uInt16ToBytes(packetId) + byteArrayOf(0x00, reasonCode.toByte())

private fun encodePublishPayload(
    topic: String,
    payload: ByteArray,
    qos: MqttQoS = MqttQoS.AtMostOnce,
    packetId: Int? = null,
): ByteArray {
    val buffer = mutableListOf<Byte>()
    val topicBytes = topic.encodeToByteArray()
    buffer.addAll(uInt16ToBytes(topicBytes.size).toList())
    buffer.addAll(topicBytes.toList())
    if (qos != MqttQoS.AtMostOnce) {
        require(packetId != null) { "packetId is required for QoS > 0" }
        buffer.addAll(uInt16ToBytes(packetId).toList())
    }
    buffer.add(0x00)
    buffer.addAll(payload.toList())
    return buffer.toByteArray()
}

private fun assertAckPacket(payload: ByteArray, packetId: Int) {
    assertEquals(packetId, readUInt16(payload, 0))
    assertEquals(0x00, payload[2].toInt() and 0xFF)
    assertEquals(0x00, payload[3].toInt() and 0xFF)
}

private fun readPublishPacketId(payload: ByteArray): Int {
    val topicLength = readUInt16(payload, 0)
    val packetIdOffset = 2 + topicLength
    return readUInt16(payload, packetIdOffset)
}

private fun readPublishPropertiesLength(payload: ByteArray): Int {
    val topicLength = readUInt16(payload, 0)
    val packetIdOffset = 2 + topicLength
    val propertiesOffset = packetIdOffset + 2
    return readVariableByteInteger(payload, propertiesOffset).first
}

private fun readSubscribeTopic(payload: ByteArray): String {
    var offset = 2
    val (_, nextOffset) = readVariableByteInteger(payload, offset)
    offset = nextOffset
    return readUtf8(payload, offset)
}

private fun readUnsubscribeTopic(payload: ByteArray): String {
    var offset = 2
    val (_, nextOffset) = readVariableByteInteger(payload, offset)
    offset = nextOffset
    return readUtf8(payload, offset)
}

private fun readUtf8(payload: ByteArray, offset: Int): String {
    val length = readUInt16(payload, offset)
    val start = offset + 2
    val end = start + length
    return payload.decodeToString(start, end)
}

private fun readVariableByteInteger(payload: ByteArray, offset: Int): Pair<Int, Int> {
    var multiplier = 1
    var value = 0
    var currentOffset = offset

    do {
        val digit = payload[currentOffset].toInt() and 0xFF
        value += (digit and 0x7F) * multiplier
        multiplier *= 128
        currentOffset += 1
        check(multiplier <= 128 * 128 * 128 * 128) { "Malformed variable byte integer" }
    } while ((digit and 0x80) != 0)

    return value to currentOffset
}

private fun readUInt16(payload: ByteArray, offset: Int): Int =
    ((payload[offset].toInt() and 0xFF) shl 8) or (payload[offset + 1].toInt() and 0xFF)

private fun uInt16ToBytes(value: Int): ByteArray =
    byteArrayOf((value shr 8 and 0xFF).toByte(), (value and 0xFF).toByte())
