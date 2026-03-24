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
internal const val MQTT_UNSIGNED_BYTE_MASK = 0xFF
internal const val MQTT_VARIABLE_BYTE_VALUE_MASK = 0x7F
internal const val MQTT_VARIABLE_BYTE_CONTINUATION_MASK = 0x80
internal const val MQTT_CONNACK_SESSION_PRESENT_FLAG = 0x01

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
internal const val MQTT_REASON_UNSPECIFIED_ERROR = 0x80
internal const val MQTT_REASON_MALFORMED_PACKET = 0x81
internal const val MQTT_REASON_PROTOCOL_ERROR = 0x82
internal const val MQTT_REASON_IMPLEMENTATION_SPECIFIC_ERROR = 0x83
internal const val MQTT_REASON_UNSUPPORTED_PROTOCOL_VERSION = 0x84
internal const val MQTT_REASON_CLIENT_IDENTIFIER_NOT_VALID = 0x85
internal const val MQTT_REASON_BAD_USER_NAME_OR_PASSWORD = 0x86
internal const val MQTT_REASON_NOT_AUTHORIZED = 0x87
internal const val MQTT_REASON_SERVER_UNAVAILABLE = 0x88
internal const val MQTT_REASON_SERVER_BUSY = 0x89
internal const val MQTT_REASON_BANNED = 0x8A
internal const val MQTT_REASON_SERVER_SHUTTING_DOWN = 0x8B
internal const val MQTT_REASON_BAD_AUTHENTICATION_METHOD = 0x8C
internal const val MQTT_REASON_KEEP_ALIVE_TIMEOUT = 0x8D
internal const val MQTT_REASON_SESSION_TAKEN_OVER = 0x8E
internal const val MQTT_REASON_TOPIC_FILTER_INVALID = 0x8F
internal const val MQTT_REASON_TOPIC_NAME_INVALID = 0x90
internal const val MQTT_REASON_PACKET_IDENTIFIER_IN_USE = 0x91
internal const val MQTT_REASON_PACKET_IDENTIFIER_NOT_FOUND = 0x92
internal const val MQTT_REASON_PACKET_TOO_LARGE = 0x95
internal const val MQTT_REASON_QUOTA_EXCEEDED = 0x97
internal const val MQTT_REASON_PAYLOAD_FORMAT_INVALID = 0x99
internal const val MQTT_REASON_RETAIN_NOT_SUPPORTED = 0x9A
internal const val MQTT_REASON_QOS_NOT_SUPPORTED = 0x9B
internal const val MQTT_REASON_USE_ANOTHER_SERVER = 0x9C
internal const val MQTT_REASON_SERVER_MOVED = 0x9D
internal const val MQTT_REASON_SHARED_SUBSCRIPTIONS_NOT_SUPPORTED = 0x9E
internal const val MQTT_REASON_CONNECTION_RATE_EXCEEDED = 0x9F
internal const val MQTT_REASON_SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED = 0xA1
internal const val MQTT_REASON_WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED = 0xA2

internal const val MQTT_PROPERTY_SESSION_EXPIRY_INTERVAL = 0x11
internal const val MQTT_MAX_PACKET_ID = 0xFFFF
internal const val MQTT_EMPTY_PROPERTIES_LENGTH = 0
internal const val MQTT_PACKET_ID_SIZE = 2
internal const val MQTT_UTF8_LENGTH_PREFIX_SIZE = MQTT_PACKET_ID_SIZE

private const val MAX_REMAINING_LENGTH_BYTES = 4
private const val MAX_REMAINING_LENGTH = 268_435_455
private const val MQTT_VARIABLE_BYTE_INTEGER_BASE = 128
private const val MQTT_UINT16_HIGH_BYTE_SHIFT = 8
private const val MQTT_UINT32_BYTE_3_SHIFT = 24
private const val MQTT_UINT32_BYTE_2_SHIFT = 16
private const val MQTT_UINT32_BYTE_1_SHIFT = 8
private const val UINT32_MAX = 0xFFFF_FFFFL
private const val MQTT_PACKET_ID_OFFSET = 0
private const val MQTT_ACK_REASON_CODE_OFFSET = 2
private const val MQTT_ACK_PROPERTIES_OFFSET = 3
private const val MQTT_CONNACK_REASON_CODE_OFFSET = 1
private const val MQTT_CONNACK_PROPERTIES_OFFSET = 2
private const val MQTT_MULTI_LEVEL_WILDCARD = "#"
private const val MQTT_SINGLE_LEVEL_WILDCARD = "+"
private const val MQTT_TOPIC_SEPARATOR = "/"
private const val MQTT_HEX_BYTE_WIDTH = 2
private const val MQTT_HEX_RADIX = 16

private val MQTT_REASON_DESCRIPTIONS = mapOf(
    MQTT_REASON_SUCCESS to "Success",
    MQTT_REASON_NO_MATCHING_SUBSCRIBERS to "No matching subscribers",
    MQTT_REASON_NO_SUBSCRIPTION_EXISTED to "No subscription existed",
    MQTT_REASON_UNSPECIFIED_ERROR to "Unspecified error",
    MQTT_REASON_MALFORMED_PACKET to "Malformed packet",
    MQTT_REASON_PROTOCOL_ERROR to "Protocol error",
    MQTT_REASON_IMPLEMENTATION_SPECIFIC_ERROR to "Implementation specific error",
    MQTT_REASON_NOT_AUTHORIZED to "Not authorized",
    MQTT_REASON_SERVER_BUSY to "Server busy",
    MQTT_REASON_SERVER_SHUTTING_DOWN to "Server shutting down",
    MQTT_REASON_KEEP_ALIVE_TIMEOUT to "Keep alive timeout",
    MQTT_REASON_SESSION_TAKEN_OVER to "Session taken over",
    MQTT_REASON_TOPIC_FILTER_INVALID to "Topic filter invalid",
    MQTT_REASON_TOPIC_NAME_INVALID to "Topic name invalid",
    MQTT_REASON_PACKET_IDENTIFIER_IN_USE to "Packet identifier in use",
    MQTT_REASON_PACKET_IDENTIFIER_NOT_FOUND to "Packet identifier not found",
    MQTT_REASON_PACKET_TOO_LARGE to "Packet too large",
    MQTT_REASON_QUOTA_EXCEEDED to "Quota exceeded",
    MQTT_REASON_PAYLOAD_FORMAT_INVALID to "Payload format invalid",
    MQTT_REASON_RETAIN_NOT_SUPPORTED to "Retain not supported",
    MQTT_REASON_QOS_NOT_SUPPORTED to "QoS not supported",
    MQTT_REASON_USE_ANOTHER_SERVER to "Use another server",
    MQTT_REASON_SERVER_MOVED to "Server moved",
    MQTT_REASON_SHARED_SUBSCRIPTIONS_NOT_SUPPORTED to "Shared subscriptions not supported",
    MQTT_REASON_CONNECTION_RATE_EXCEEDED to "Connection rate exceeded",
    MQTT_REASON_SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED to "Subscription identifiers not supported",
    MQTT_REASON_WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED to "Wildcard subscriptions not supported",
)

private val MQTT_CONNECT_REASON_DESCRIPTIONS = MQTT_REASON_DESCRIPTIONS + mapOf(
    MQTT_REASON_UNSUPPORTED_PROTOCOL_VERSION to "Unsupported protocol version",
    MQTT_REASON_CLIENT_IDENTIFIER_NOT_VALID to "Client identifier not valid",
    MQTT_REASON_BAD_USER_NAME_OR_PASSWORD to "Bad user name or password",
    MQTT_REASON_SERVER_UNAVAILABLE to "Server unavailable",
    MQTT_REASON_BANNED to "Banned",
    MQTT_REASON_BAD_AUTHENTICATION_METHOD to "Bad authentication method",
)

internal data class NativeMqttPacket(val fixedHeader: Int, val payload: ByteArray)

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
    return NativeMqttPacket(firstByte.toInt() and MQTT_UNSIGNED_BYTE_MASK, payload)
}

internal fun packetType(fixedHeader: Int): Int = fixedHeader and MQTT_PACKET_TYPE_MASK

internal fun isPublishPacket(fixedHeader: Int): Boolean = packetType(fixedHeader) == MQTT_PUBLISH_TYPE

internal fun isPubRelPacket(fixedHeader: Int): Boolean = packetType(fixedHeader) == MQTT_PUBREL_TYPE

internal fun buildPublishFixedHeader(qos: MqttQoS): Int = MQTT_PUBLISH_TYPE or (qos.code shl MQTT_PUBLISH_QOS_SHIFT)

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

internal fun buildPublishPayload(topic: String, payload: ByteArray, qos: MqttQoS, packetId: Int): ByteArray =
    MqttBuffer().apply {
        writeUtf8String(topic)
        if (qos != MqttQoS.AtMostOnce) {
            writeUInt16(packetId)
        }
        writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
        writeBytes(payload)
    }.toByteArray()

internal fun buildReasonCodeAckPayload(packetId: Int, reasonCode: Int = MQTT_REASON_SUCCESS): ByteArray =
    MqttBuffer().apply {
        writeUInt16(packetId)
        writeByte(reasonCode)
        writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
    }.toByteArray()

internal fun buildSubscribePayload(topic: String, qos: MqttQoS, packetId: Int): ByteArray = MqttBuffer().apply {
    writeUInt16(packetId)
    writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
    writeUtf8String(topic)
    writeByte(qos.code)
}.toByteArray()

internal fun buildUnsubscribePayload(topic: String, packetId: Int): ByteArray = MqttBuffer().apply {
    writeUInt16(packetId)
    writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
    writeUtf8String(topic)
}.toByteArray()

internal fun buildDisconnectPayload(): ByteArray = MqttBuffer().apply {
    writeByte(MQTT_REASON_SUCCESS)
    writeVariableByteInteger(MQTT_EMPTY_PROPERTIES_LENGTH)
}.toByteArray()

internal fun buildMqttPacket(fixedHeader: Int, payload: ByteArray): ByteArray = MqttBuffer().apply {
    writeByte(fixedHeader)
    writeVariableByteInteger(payload.size)
    writeBytes(payload)
}.toByteArray()

internal fun ByteArray.readConnAckReasonCode(): Int {
    check(size >= 3) { "CONNACK packet is too short" }
    val propertiesEnd = skipPropertySection(MQTT_CONNACK_PROPERTIES_OFFSET, "CONNACK")
    check(propertiesEnd == size) { "CONNACK packet has trailing bytes" }
    return this[MQTT_CONNACK_REASON_CODE_OFFSET].toInt() and MQTT_UNSIGNED_BYTE_MASK
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
    val reasonCode = this[MQTT_ACK_REASON_CODE_OFFSET].toInt() and MQTT_UNSIGNED_BYTE_MASK
    val propertiesEnd = skipPropertySection(MQTT_ACK_PROPERTIES_OFFSET, packetName)
    check(propertiesEnd == size) { "$packetName packet has trailing bytes" }
    return reasonCode
}

internal fun ByteArray.readReasonCodes(packetName: String): List<Int> {
    check(size >= 3) { "$packetName packet is too short" }
    val reasonsOffset = skipPropertySection(MQTT_PACKET_ID_SIZE, packetName)
    check(reasonsOffset < size) { "$packetName packet is missing reason codes" }
    return copyOfRange(reasonsOffset, size).map { it.toInt() and MQTT_UNSIGNED_BYTE_MASK }
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
    val reasonCode = first().toInt() and MQTT_UNSIGNED_BYTE_MASK
    if (size > 1) {
        val propertiesEnd = skipPropertySection(1, "DISCONNECT")
        check(propertiesEnd == size) { "DISCONNECT packet has trailing bytes" }
    }
    return IllegalStateException("Broker sent DISCONNECT: ${describeReasonCode(reasonCode)}")
}

internal fun matchesTopicFilter(topic: String, filter: String): Boolean {
    val topicParts = topic.split(MQTT_TOPIC_SEPARATOR)
    val filterParts = filter.split(MQTT_TOPIC_SEPARATOR)
    var topicIndex = 0
    var filterIndex = 0
    var matches = true

    while (matches && filterIndex < filterParts.size) {
        val filterPart = filterParts[filterIndex]
        when {
            filterPart == MQTT_MULTI_LEVEL_WILDCARD -> {
                matches = filterIndex == filterParts.lastIndex
                topicIndex = topicParts.size
                filterIndex = filterParts.size
            }

            topicIndex >= topicParts.size -> matches = false

            filterPart != MQTT_SINGLE_LEVEL_WILDCARD && filterPart != topicParts[topicIndex] -> {
                matches = false
            }

            else -> {
                topicIndex += 1
                filterIndex += 1
            }
        }
    }

    return matches && topicIndex == topicParts.size && filterIndex == filterParts.size
}

internal fun describeConnectReasonCode(reasonCode: Int): String =
    MQTT_CONNECT_REASON_DESCRIPTIONS[reasonCode] ?: "Reason code ${reasonCode.toHexByte()}"

internal fun describeReasonCode(reasonCode: Int): String =
    MQTT_REASON_DESCRIPTIONS[reasonCode] ?: "Reason code ${reasonCode.toHexByte()}"

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
        val byte = readMqttByte().toInt() and MQTT_UNSIGNED_BYTE_MASK
        value += (byte and MQTT_VARIABLE_BYTE_VALUE_MASK) * multiplier
        check(value <= MAX_REMAINING_LENGTH) { "MQTT remaining length exceeds maximum" }
        multiplier *= MQTT_VARIABLE_BYTE_INTEGER_BASE
        count += 1
        check(count <= MAX_REMAINING_LENGTH_BYTES) { "Malformed MQTT remaining length" }
    } while (byte and MQTT_VARIABLE_BYTE_CONTINUATION_MASK != 0)

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
    val start = offset + MQTT_UTF8_LENGTH_PREFIX_SIZE
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
        val encodedByte = this[currentOffset].toInt() and MQTT_UNSIGNED_BYTE_MASK
        value += (encodedByte and MQTT_VARIABLE_BYTE_VALUE_MASK) * multiplier
        currentOffset += 1
        count += 1
        check(count <= MAX_REMAINING_LENGTH_BYTES) { "Malformed $packetName variable byte integer" }
        if (encodedByte and MQTT_VARIABLE_BYTE_CONTINUATION_MASK == 0) {
            return value to currentOffset
        }
        multiplier *= MQTT_VARIABLE_BYTE_INTEGER_BASE
        check(value <= MAX_REMAINING_LENGTH) { "$packetName variable byte integer exceeds maximum" }
    }
}

private fun ByteArray.readMqttUInt16(offset: Int): Int {
    check(offset + 1 < size) { "Malformed MQTT packet: expected UInt16 at offset $offset" }
    return ((this[offset].toInt() and MQTT_UNSIGNED_BYTE_MASK) shl MQTT_UINT16_HIGH_BYTE_SHIFT) or
        (this[offset + 1].toInt() and MQTT_UNSIGNED_BYTE_MASK)
}

private fun Int.toHexByte(): String = "0x" + toString(MQTT_HEX_RADIX).padStart(MQTT_HEX_BYTE_WIDTH, '0')

private class MqttBuffer {
    private val buffer = mutableListOf<Byte>()

    val size: Int
        get() = buffer.size

    fun writeByte(value: Int) {
        buffer.add((value and MQTT_UNSIGNED_BYTE_MASK).toByte())
    }

    fun writeUInt16(value: Int) {
        writeByte(value shr MQTT_UINT16_HIGH_BYTE_SHIFT)
        writeByte(value)
    }

    fun writeUInt32(value: Long) {
        writeByte((value shr MQTT_UINT32_BYTE_3_SHIFT).toInt())
        writeByte((value shr MQTT_UINT32_BYTE_2_SHIFT).toInt())
        writeByte((value shr MQTT_UINT32_BYTE_1_SHIFT).toInt())
        writeByte(value.toInt())
    }

    fun writeVariableByteInteger(value: Int) {
        require(value in 0..MAX_REMAINING_LENGTH) { "Variable byte integer out of range: $value" }
        var remaining = value
        do {
            var digit = remaining % MQTT_VARIABLE_BYTE_INTEGER_BASE
            remaining /= MQTT_VARIABLE_BYTE_INTEGER_BASE
            if (remaining > 0) {
                digit = digit or MQTT_VARIABLE_BYTE_CONTINUATION_MASK
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
