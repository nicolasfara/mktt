package io.github.nicolasfara

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readFully

internal const val MQTT_CONNECT = 0x10
internal const val MQTT_CONNACK = 0x20
internal const val MQTT_PUBLISH_TYPE = 0x30
internal const val MQTT_PUBACK = 0x40
internal const val MQTT_PUBREC = 0x50
internal const val MQTT_PUBREL_TYPE = 0x60
internal const val MQTT_PUBREL_FLAGS = 0x02
internal const val MQTT_PUBREL = MQTT_PUBREL_TYPE or MQTT_PUBREL_FLAGS
internal const val MQTT_PUBCOMP = 0x70
internal const val MQTT_SUBSCRIBE_TYPE = 0x80
internal const val MQTT_SUBSCRIBE_FLAGS = 0x02
internal const val MQTT_SUBSCRIBE = MQTT_SUBSCRIBE_TYPE or MQTT_SUBSCRIBE_FLAGS
internal const val MQTT_SUBACK = 0x90
internal const val MQTT_UNSUBSCRIBE_TYPE = 0xA0
internal const val MQTT_UNSUBSCRIBE_FLAGS = 0x02
internal const val MQTT_UNSUBSCRIBE = MQTT_UNSUBSCRIBE_TYPE or MQTT_UNSUBSCRIBE_FLAGS
internal const val MQTT_UNSUBACK = 0xB0
internal const val MQTT_PINGREQ = 0xC0
internal const val MQTT_PINGRESP = 0xD0
internal const val MQTT_DISCONNECT = 0xE0

internal const val MQTT_PROTOCOL_NAME = "MQTT"
internal const val MQTT_PROTOCOL_LEVEL = 5

internal const val MQTT_PACKET_TYPE_MASK = 0xF0
internal const val MQTT_FIXED_HEADER_RETAIN_FLAG = 0x01
internal const val MQTT_PUBLISH_QOS_SHIFT = 1
internal const val MQTT_PUBLISH_QOS_MASK = 0x03

internal const val MQTT_CONNECT_FLAG_CLEAN_START = 0x02
internal const val MQTT_CONNECT_FLAG_WILL = 0x04
internal const val MQTT_CONNECT_FLAG_WILL_QOS_SHIFT = 3
internal const val MQTT_CONNECT_FLAG_WILL_RETAIN = 0x20
internal const val MQTT_CONNECT_FLAG_PASSWORD = 0x40
internal const val MQTT_CONNECT_FLAG_USERNAME = 0x80

internal const val MQTT_REASON_SUCCESS = 0x00
internal const val MQTT_REASON_NO_MATCHING_SUBSCRIBERS = 0x10
internal const val MQTT_REASON_NO_SUBSCRIPTION_EXISTED = 0x11
internal const val MQTT_REASON_ERROR_THRESHOLD = 0x80

internal const val MQTT_PROPERTY_SESSION_EXPIRY_INTERVAL = 0x11
internal const val MQTT_MAX_PACKET_ID = 0xFFFF

private const val MAX_REMAINING_LENGTH_BYTES = 4
private const val MAX_REMAINING_LENGTH = 268_435_455
private const val UINT32_MAX = 0xFFFF_FFFFL
private const val MQTT_EMPTY_PROPERTIES_LENGTH = 0
private const val MQTT_PACKET_ID_OFFSET = 0
private const val MQTT_PACKET_ID_SIZE = 2
private const val MQTT_ACK_REASON_CODE_OFFSET = 2
private const val MQTT_ACK_PROPERTIES_OFFSET = 3
private const val MQTT_CONNACK_REASON_CODE_OFFSET = 1
private const val MQTT_CONNACK_PROPERTIES_OFFSET = 2

internal data class NativeMqttPacket(
    val fixedHeader: Int,
    val payload: ByteArray,
)

internal data class NativeIncomingPublish(
    val topic: String,
    val payload: ByteArray,
    val qos: MqttQoS,
    val retain: Boolean,
    val packetId: Int,
)

internal suspend fun ByteReadChannel.readMqttPacket(): NativeMqttPacket {
    val firstByte = readMqttByte()
    val remainingLength = readMqttVariableLength()
    val payload = ByteArray(remainingLength)
    if (remainingLength > 0) {
        readFully(payload, 0, remainingLength)
    }
    return NativeMqttPacket(firstByte.toInt() and 0xFF, payload)
}

internal fun packetType(fixedHeader: Int): Int = fixedHeader and MQTT_PACKET_TYPE_MASK

internal fun isPublishPacket(fixedHeader: Int): Boolean = packetType(fixedHeader) == MQTT_PUBLISH_TYPE

internal fun isPubRelPacket(fixedHeader: Int): Boolean = packetType(fixedHeader) == MQTT_PUBREL_TYPE

internal fun buildPublishFixedHeader(qos: MqttQoS): Int =
    MQTT_PUBLISH_TYPE or (qos.code shl MQTT_PUBLISH_QOS_SHIFT)

internal fun buildConnectPayload(configuration: MqttClientConfiguration): ByteArray {
    val properties = MqttBuffer().apply {
        if (!configuration.cleanSession) {
            writeByte(MQTT_PROPERTY_SESSION_EXPIRY_INTERVAL)
            writeUInt32(UINT32_MAX)
        }
    }

    return MqttBuffer().apply {
        writeUtf8String(MQTT_PROTOCOL_NAME)
        writeByte(MQTT_PROTOCOL_LEVEL)
        writeByte(buildConnectFlags(configuration))
        writeUInt16(configuration.keepAliveInterval.toInt())
        writeVariableByteInteger(properties.size)
        writeBytes(properties.toByteArray())

        writeUtf8String(configuration.clientId)
        configuration.will?.let { will ->
            writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
            writeUtf8String(will.topic)
            writeBinaryData(will.message)
        }
        configuration.username?.let { writeUtf8String(it) }
        configuration.password?.let { writeBinaryData(it.encodeToByteArray()) }
    }.toByteArray()
}

internal fun buildPublishPayload(
    topic: String,
    payload: ByteArray,
    qos: MqttQoS,
    packetId: Int,
): ByteArray = MqttBuffer().apply {
    writeUtf8String(topic)
    if (qos != MqttQoS.AtMostOnce) {
        writeUInt16(packetId)
    }
    writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
    writeBytes(payload)
}.toByteArray()

internal fun buildReasonCodeAckPayload(
    packetId: Int,
    reasonCode: Int = MQTT_REASON_SUCCESS,
): ByteArray = MqttBuffer().apply {
    writeUInt16(packetId)
    writeByte(reasonCode)
    writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
}.toByteArray()

internal fun buildSubscribePayload(topic: String, qos: MqttQoS, packetId: Int): ByteArray =
    MqttBuffer().apply {
        writeUInt16(packetId)
        writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
        writeUtf8String(topic)
        writeByte(qos.code)
    }.toByteArray()

internal fun buildUnsubscribePayload(topic: String, packetId: Int): ByteArray =
    MqttBuffer().apply {
        writeUInt16(packetId)
        writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
        writeUtf8String(topic)
    }.toByteArray()

internal fun buildDisconnectPayload(): ByteArray = MqttBuffer().apply {
    writeByte(MQTT_REASON_SUCCESS)
    writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
}.toByteArray()

internal fun buildMqttPacket(fixedHeader: Int, payload: ByteArray): ByteArray =
    MqttBuffer().apply {
        writeByte(fixedHeader)
        writeVariableByteInteger(payload.size)
        writeBytes(payload)
    }.toByteArray()

internal fun ByteArray.readConnAckReasonCode(): Int {
    check(size >= 3) { "CONNACK packet is too short" }
    val propertiesEnd = skipPropertySection(MQTT_CONNACK_PROPERTIES_OFFSET, "CONNACK")
    check(propertiesEnd == size) { "CONNACK packet has trailing bytes" }
    return this[MQTT_CONNACK_REASON_CODE_OFFSET].toInt() and 0xFF
}

internal fun ByteArray.readMqttPacketId(): Int {
    check(size >= MQTT_PACKET_ID_SIZE) { "ACK packet is too short" }
    return readMqttUInt16(MQTT_PACKET_ID_OFFSET)
}

internal fun ByteArray.readAckReasonCode(packetName: String): Int {
    check(size >= MQTT_PACKET_ID_SIZE) { "$packetName packet is too short" }
    if (size == MQTT_PACKET_ID_SIZE) {
        return MQTT_REASON_SUCCESS
    }
    check(size >= 4) { "$packetName packet is malformed" }
    val reasonCode = this[MQTT_ACK_REASON_CODE_OFFSET].toInt() and 0xFF
    val propertiesEnd = skipPropertySection(MQTT_ACK_PROPERTIES_OFFSET, packetName)
    check(propertiesEnd == size) { "$packetName packet has trailing bytes" }
    return reasonCode
}

internal fun ByteArray.readReasonCodes(packetName: String): List<Int> {
    check(size >= 3) { "$packetName packet is too short" }
    val reasonsOffset = skipPropertySection(MQTT_PACKET_ID_SIZE, packetName)
    check(reasonsOffset < size) { "$packetName packet is missing reason codes" }
    return copyOfRange(reasonsOffset, size).map { it.toInt() and 0xFF }
}

internal fun ByteArray.parseIncomingPublish(fixedHeader: Int): NativeIncomingPublish {
    val qos = MqttQoS.from((fixedHeader shr MQTT_PUBLISH_QOS_SHIFT) and MQTT_PUBLISH_QOS_MASK)
    val retain = (fixedHeader and MQTT_FIXED_HEADER_RETAIN_FLAG) != 0

    var offset = 0
    val (topic, nextOffset) = readUtf8String(offset, "PUBLISH topic")
    offset = nextOffset

    val packetId = if (qos != MqttQoS.AtMostOnce) {
        val id = readMqttUInt16(offset)
        offset += MQTT_PACKET_ID_SIZE
        id
    } else {
        0
    }

    offset = skipPropertySection(offset, "PUBLISH")
    val payload = copyOfRange(offset, size)
    return NativeIncomingPublish(topic = topic, payload = payload, qos = qos, retain = retain, packetId = packetId)
}

internal fun ByteArray.toDisconnectException(): IllegalStateException {
    if (isEmpty()) {
        return IllegalStateException("Broker sent DISCONNECT")
    }
    val reasonCode = first().toInt() and 0xFF
    if (size > 1) {
        val propertiesEnd = skipPropertySection(1, "DISCONNECT")
        check(propertiesEnd == size) { "DISCONNECT packet has trailing bytes" }
    }
    return IllegalStateException("Broker sent DISCONNECT: ${describeReasonCode(reasonCode)}")
}

internal fun matchesTopicFilter(topic: String, filter: String): Boolean {
    val topicParts = topic.split("/")
    val filterParts = filter.split("/")

    fun match(topicIndex: Int, filterIndex: Int): Boolean {
        if (filterIndex == filterParts.size) {
            return topicIndex == topicParts.size
        }
        if (filterParts[filterIndex] == "#") {
            return true
        }
        if (topicIndex == topicParts.size) {
            return false
        }
        if (filterParts[filterIndex] != "+" && filterParts[filterIndex] != topicParts[topicIndex]) {
            return false
        }
        return match(topicIndex + 1, filterIndex + 1)
    }

    return match(0, 0)
}

internal fun describeConnectReasonCode(reasonCode: Int): String =
    when (reasonCode) {
        0x80 -> "Unspecified error"
        0x81 -> "Malformed packet"
        0x82 -> "Protocol error"
        0x83 -> "Implementation specific error"
        0x84 -> "Unsupported protocol version"
        0x85 -> "Client identifier not valid"
        0x86 -> "Bad user name or password"
        0x87 -> "Not authorized"
        0x88 -> "Server unavailable"
        0x89 -> "Server busy"
        0x8A -> "Banned"
        0x8C -> "Bad authentication method"
        0x90 -> "Topic name invalid"
        0x95 -> "Packet too large"
        0x97 -> "Quota exceeded"
        0x99 -> "Payload format invalid"
        0x9A -> "Retain not supported"
        0x9B -> "QoS not supported"
        0x9C -> "Use another server"
        0x9D -> "Server moved"
        0x9F -> "Connection rate exceeded"
        else -> "Reason code ${reasonCode.toHexByte()}"
    }

internal fun describeReasonCode(reasonCode: Int): String =
    when (reasonCode) {
        MQTT_REASON_SUCCESS -> "Success"
        MQTT_REASON_NO_MATCHING_SUBSCRIBERS -> "No matching subscribers"
        MQTT_REASON_NO_SUBSCRIPTION_EXISTED -> "No subscription existed"
        0x80 -> "Unspecified error"
        0x81 -> "Malformed packet"
        0x82 -> "Protocol error"
        0x83 -> "Implementation specific error"
        0x87 -> "Not authorized"
        0x89 -> "Server busy"
        0x8B -> "Server shutting down"
        0x8D -> "Keep alive timeout"
        0x8E -> "Session taken over"
        0x8F -> "Topic filter invalid"
        0x90 -> "Topic name invalid"
        0x91 -> "Packet identifier in use"
        0x92 -> "Packet identifier not found"
        0x95 -> "Packet too large"
        0x97 -> "Quota exceeded"
        0x99 -> "Payload format invalid"
        0x9A -> "Retain not supported"
        0x9B -> "QoS not supported"
        0x9C -> "Use another server"
        0x9D -> "Server moved"
        0x9E -> "Shared subscriptions not supported"
        0xA1 -> "Subscription identifiers not supported"
        0xA2 -> "Wildcard subscriptions not supported"
        else -> "Reason code ${reasonCode.toHexByte()}"
    }

private suspend fun ByteReadChannel.readMqttByte(): Byte {
    val buffer = ByteArray(1)
    readFully(buffer, 0, 1)
    return buffer[0]
}

private suspend fun ByteReadChannel.readMqttVariableLength(): Int {
    var multiplier = 1
    var value = 0
    var count = 0

    do {
        val byte = readMqttByte().toInt() and 0xFF
        value += (byte and 0x7F) * multiplier
        check(value <= MAX_REMAINING_LENGTH) { "MQTT remaining length exceeds maximum" }
        multiplier *= 128
        count += 1
        check(count <= MAX_REMAINING_LENGTH_BYTES) { "Malformed MQTT remaining length" }
    } while (byte and 0x80 != 0)

    return value
}

private fun buildConnectFlags(configuration: MqttClientConfiguration): Int {
    var flags = 0
    if (configuration.cleanSession) {
        flags = flags or MQTT_CONNECT_FLAG_CLEAN_START
    }
    configuration.will?.let { will ->
        flags = flags or MQTT_CONNECT_FLAG_WILL
        flags = flags or (will.qos.code shl MQTT_CONNECT_FLAG_WILL_QOS_SHIFT)
        if (will.retained) {
            flags = flags or MQTT_CONNECT_FLAG_WILL_RETAIN
        }
    }
    if (configuration.username != null) {
        flags = flags or MQTT_CONNECT_FLAG_USERNAME
    }
    if (configuration.password != null && configuration.username != null) {
        flags = flags or MQTT_CONNECT_FLAG_PASSWORD
    }
    return flags
}

private fun ByteArray.readUtf8String(offset: Int, fieldName: String): Pair<String, Int> {
    val length = readMqttUInt16(offset)
    val start = offset + MQTT_PACKET_ID_SIZE
    val end = start + length
    check(end <= size) { "Malformed $fieldName field" }
    return decodeToString(start, end) to end
}

private fun ByteArray.skipPropertySection(offset: Int, packetName: String): Int {
    val (propertiesLength, nextOffset) = readMqttVariableByteInteger(offset, packetName)
    val endOffset = nextOffset + propertiesLength
    check(endOffset <= size) { "$packetName properties exceed packet size" }
    return endOffset
}

private fun ByteArray.readMqttVariableByteInteger(offset: Int, packetName: String): Pair<Int, Int> {
    var value = 0
    var multiplier = 1
    var count = 0
    var currentOffset = offset

    while (true) {
        check(currentOffset < size) { "Malformed $packetName variable byte integer" }
        val encodedByte = this[currentOffset].toInt() and 0xFF
        value += (encodedByte and 0x7F) * multiplier
        currentOffset += 1
        count += 1
        check(count <= MAX_REMAINING_LENGTH_BYTES) { "Malformed $packetName variable byte integer" }
        if (encodedByte and 0x80 == 0) {
            return value to currentOffset
        }
        multiplier *= 128
        check(value <= MAX_REMAINING_LENGTH) { "$packetName variable byte integer exceeds maximum" }
    }
}

private fun ByteArray.readMqttUInt16(offset: Int): Int {
    check(offset + 1 < size) { "Malformed MQTT packet: expected UInt16 at offset $offset" }
    return ((this[offset].toInt() and 0xFF) shl 8) or (this[offset + 1].toInt() and 0xFF)
}

private fun Int.toHexByte(): String = "0x" + toString(16).padStart(2, '0')

private class MqttBuffer {
    private val buffer = mutableListOf<Byte>()

    val size: Int
        get() = buffer.size

    fun writeByte(value: Int) {
        buffer.add((value and 0xFF).toByte())
    }

    fun writeUInt16(value: Int) {
        writeByte(value shr 8)
        writeByte(value)
    }

    fun writeUInt32(value: Long) {
        writeByte((value shr 24).toInt())
        writeByte((value shr 16).toInt())
        writeByte((value shr 8).toInt())
        writeByte(value.toInt())
    }

    fun writeVariableByteInteger(value: Int) {
        require(value in 0..MAX_REMAINING_LENGTH) { "Variable byte integer out of range: $value" }
        var remaining = value
        do {
            var digit = remaining % 128
            remaining /= 128
            if (remaining > 0) {
                digit = digit or 0x80
            }
            writeByte(digit)
        } while (remaining > 0)
    }

    fun writeBytes(bytes: ByteArray) {
        buffer.addAll(bytes.toList())
    }

    fun writeUtf8String(value: String) {
        val bytes = value.encodeToByteArray()
        writeUInt16(bytes.size)
        writeBytes(bytes)
    }

    fun writeBinaryData(bytes: ByteArray) {
        writeUInt16(bytes.size)
        writeBytes(bytes)
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()
}
