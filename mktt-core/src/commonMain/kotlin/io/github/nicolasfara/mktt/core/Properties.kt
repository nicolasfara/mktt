package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.util.Logger
import io.github.nicolasfara.mktt.core.util.readMqttByteString
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.readVariableByteInt
import io.github.nicolasfara.mktt.core.util.utf8Size
import io.github.nicolasfara.mktt.core.util.variableByteIntSize
import io.github.nicolasfara.mktt.core.util.writeMqttByteString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import io.github.nicolasfara.mktt.core.util.writeVariableByteInt
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.SocketAddress
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readUInt
import kotlinx.io.readUShort
import kotlinx.io.writeUInt
import kotlinx.io.writeUShort

private const val PROPERTY_IDENTIFIER_MASK = 0xFF
private const val PROPERTY_ID_PAYLOAD_FORMAT_INDICATOR = 1
private const val PROPERTY_ID_MESSAGE_EXPIRY_INTERVAL = 2
private const val PROPERTY_ID_CONTENT_TYPE = 3
private const val PROPERTY_ID_RESPONSE_TOPIC = 8
private const val PROPERTY_ID_CORRELATION_DATA = 9
private const val PROPERTY_ID_SUBSCRIPTION_IDENTIFIER = 11
private const val PROPERTY_ID_SESSION_EXPIRY_INTERVAL = 17
private const val PROPERTY_ID_ASSIGNED_CLIENT_IDENTIFIER = 18
private const val PROPERTY_ID_SERVER_KEEP_ALIVE = 19
private const val PROPERTY_ID_AUTHENTICATION_METHOD = 21
private const val PROPERTY_ID_AUTHENTICATION_DATA = 22
private const val PROPERTY_ID_REQUEST_PROBLEM_INFORMATION = 23
private const val PROPERTY_ID_WILL_DELAY_INTERVAL = 24
private const val PROPERTY_ID_REQUEST_RESPONSE_INFORMATION = 25
private const val PROPERTY_ID_RESPONSE_INFORMATION = 26
private const val PROPERTY_ID_SERVER_REFERENCE = 28
private const val PROPERTY_ID_REASON_STRING = 31
private const val PROPERTY_ID_RECEIVE_MAXIMUM = 33
private const val PROPERTY_ID_TOPIC_ALIAS_MAXIMUM = 34
private const val PROPERTY_ID_TOPIC_ALIAS = 35
private const val PROPERTY_ID_MAXIMUM_QOS = 36
private const val PROPERTY_ID_RETAIN_AVAILABLE = 37
private const val PROPERTY_ID_USER_PROPERTY = 38
private const val PROPERTY_ID_MAXIMUM_PACKET_SIZE = 39
private const val PROPERTY_ID_WILDCARD_SUBSCRIPTION_AVAILABLE = 40
private const val PROPERTY_ID_SUBSCRIPTION_IDENTIFIER_AVAILABLE = 41
private const val PROPERTY_ID_SHARED_SUBSCRIPTION_AVAILABLE = 42

private const val BYTE_FALSE: Byte = 0
private const val BYTE_TRUE: Byte = 1

/**
 * Represents the MQTT property as defined in chapter 2.2.2 of the MQTT specification.
 */
sealed interface Property<T> {

    /**
     * The value of this property.
     */
    val value: T
}

/**
 * Returns the property of the specified type, when contained in the list.
 *
 * @throws io.github.nicolasfara.mktt.core.MalformedPacketException
 *   when the property is not contained exactly once in the list
 */
internal inline fun <reified T : Property<*>> List<Property<*>>.single(): T {
    val instances = filterIsInstance<T>()
    return if (instances.size == 1) {
        instances.first()
    } else {
        throw MalformedPacketException("Property of type: ${T::class} is not contained exactly once: ${instances.size}")
    }
}

/**
 * Returns the property of the specified type, when contained in the list,
 * or `null` otherwise.
 *
 * @throws io.github.nicolasfara.mktt.core.MalformedPacketException when the property is contained more than once
 */
internal inline fun <reified T : Property<*>> List<Property<*>>.singleOrNull(): T? {
    val instances = filterIsInstance<T>()
    return if (instances.isEmpty()) {
        null
    } else if (instances.size == 1) {
        instances.first()
    } else {
        throw MalformedPacketException(
            "A property which may appear only once, exists ${instances.size} times: $instances",
        )
    }
}

internal fun <T> Sink.write(property: Property<T>) {
    with(property as WritableProperty<T>) {
        writeByte(identifier.toByte())
        writeValue(value)
    }
}

/**
 * Writes all specified properties, which are non-null. Also prepends the byte count as a variable byte integer.
 */
internal fun Sink.writeProperties(vararg properties: Property<*>?) {
    val byteCount = properties.sumOf { (it as? WritableProperty<*>)?.byteCount() ?: 0 }
    writeVariableByteInt(byteCount)

    properties.forEach {
        if (it != null) write(it)
    }
}

internal fun Source.readProperty(): Property<*> = when (
    val identifier = (
        readByte().toInt() and
            PROPERTY_IDENTIFIER_MASK
        )
) {
    PROPERTY_ID_PAYLOAD_FORMAT_INDICATOR -> PayloadFormatIndicator.from(readByte())
    PROPERTY_ID_MESSAGE_EXPIRY_INTERVAL -> MessageExpiryInterval(readUInt())
    PROPERTY_ID_CONTENT_TYPE -> ContentType(readMqttString())
    PROPERTY_ID_RESPONSE_TOPIC -> ResponseTopic(readMqttString())
    PROPERTY_ID_CORRELATION_DATA -> CorrelationData(readMqttByteString())
    PROPERTY_ID_SUBSCRIPTION_IDENTIFIER -> SubscriptionIdentifier(readVariableByteInt())
    PROPERTY_ID_SESSION_EXPIRY_INTERVAL -> SessionExpiryInterval(readUInt())
    PROPERTY_ID_ASSIGNED_CLIENT_IDENTIFIER -> AssignedClientIdentifier(readMqttString())
    PROPERTY_ID_SERVER_KEEP_ALIVE -> ServerKeepAlive(readUShort())
    PROPERTY_ID_AUTHENTICATION_METHOD -> AuthenticationMethod(readMqttString())
    PROPERTY_ID_AUTHENTICATION_DATA -> AuthenticationData(readMqttByteString())
    PROPERTY_ID_REQUEST_PROBLEM_INFORMATION -> byteToBoolean(readByte()) { RequestProblemInformation(it) }
    PROPERTY_ID_WILL_DELAY_INTERVAL -> WillDelayInterval(readUInt())
    PROPERTY_ID_REQUEST_RESPONSE_INFORMATION -> byteToBoolean(readByte()) { RequestResponseInformation(it) }
    PROPERTY_ID_RESPONSE_INFORMATION -> ResponseInformation(readMqttString())
    PROPERTY_ID_SERVER_REFERENCE -> ServerReference(readMqttString())
    PROPERTY_ID_REASON_STRING -> ReasonString(readMqttString())
    PROPERTY_ID_RECEIVE_MAXIMUM -> ReceiveMaximum(readUShort())
    PROPERTY_ID_TOPIC_ALIAS_MAXIMUM -> TopicAliasMaximum(readUShort())
    PROPERTY_ID_TOPIC_ALIAS -> TopicAlias(readUShort())
    PROPERTY_ID_MAXIMUM_QOS -> MaximumQoS(readByte())
    PROPERTY_ID_RETAIN_AVAILABLE -> byteToBoolean(readByte()) { RetainAvailable(it) }
    PROPERTY_ID_USER_PROPERTY -> UserProperty(readStringPair())
    PROPERTY_ID_MAXIMUM_PACKET_SIZE -> MaximumPacketSize(readUInt())
    PROPERTY_ID_WILDCARD_SUBSCRIPTION_AVAILABLE -> byteToBoolean(readByte()) { WildcardSubscriptionAvailable(it) }
    PROPERTY_ID_SUBSCRIPTION_IDENTIFIER_AVAILABLE -> byteToBoolean(readByte()) { SubscriptionIdentifierAvailable(it) }
    PROPERTY_ID_SHARED_SUBSCRIPTION_AVAILABLE -> byteToBoolean(readByte()) { SharedSubscriptionAvailable(it) }
    else -> throw MalformedPacketException("Unknown property identifier: $identifier")
}

/**
 * Reads all properties from this packet by first reading the variable int byte count and then the properties.
 */
internal fun Source.readProperties(): List<Property<*>> {
    val byteCount = readVariableByteInt()
    var bytesRead = 0

    return buildList {
        while (bytesRead < byteCount) {
            val property = readProperty()
            bytesRead += (property as WritableProperty<*>).byteCount()
            add(property)
        }
    }
}

/**
 * Value class representing the **Payload Format Indicator** property as defined in the MQTT specification.
 */
@JvmInline
value class PayloadFormatIndicator private constructor(override val value: Byte) :
    WritableProperty<Byte>,
    Property<Byte> {

    /** The identifier value of this property is: `0x01`. */
    override val identifier: Int
        get() = PROPERTY_ID_PAYLOAD_FORMAT_INDICATOR

    override val writeValue: Sink.(Byte) -> Unit
        get() = ByteWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()

    /** Static helpers and constants for [PayloadFormatIndicator]. */
    companion object {
        /** Converts a wire value to [PayloadFormatIndicator]. */
        fun from(byte: Byte): PayloadFormatIndicator = when (byte) {
            BYTE_FALSE -> NONE
            BYTE_TRUE -> UTF_8
            else -> throw MalformedPacketException("Value of $byte not allowed for payload format indicator")
        }

        /** Binary payload with unspecified format. */
        val NONE: PayloadFormatIndicator = PayloadFormatIndicator(0)

        /** UTF-8 text payload format. */
        val UTF_8: PayloadFormatIndicator = PayloadFormatIndicator(1)
    }
}

/**
 * Value class representing the **Message Expiry Interval** property as defined in the MQTT specification.
 */
@JvmInline
value class MessageExpiryInterval(override val value: UInt) :
    WritableProperty<UInt>,
    Property<UInt> {

    /** The identifier value of this property is: `0x02`. */
    override val identifier: Int
        get() = PROPERTY_ID_MESSAGE_EXPIRY_INTERVAL

    override val writeValue: Sink.(UInt) -> Unit
        get() = UIntWriter

    override fun byteCount(): Int = 5

    override fun toString(): String = value.toString()
}

/** Converts [MessageExpiryInterval] to [Duration]. */
fun MessageExpiryInterval.toDuration(): Duration = value.toLong().seconds

/** Converts [Duration] to [MessageExpiryInterval]. */
fun Duration.toMessageExpiryInterval(): MessageExpiryInterval = MessageExpiryInterval(inWholeSeconds.toUInt())

/**
 * Value class representing the **Content Type** property as defined in the MQTT specification.
 */
@JvmInline
value class ContentType(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /** The identifier value of this property is: `0x03`. */
    override val identifier: Int
        get() = PROPERTY_ID_CONTENT_TYPE

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Response Topic** property as defined in the MQTT specification.
 */
@JvmInline
value class ResponseTopic(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /** The identifier value of this property is: `0x08`. */
    override val identifier: Int
        get() = PROPERTY_ID_RESPONSE_TOPIC

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Correlation Data** property as defined in the MQTT specification.
 */
@JvmInline
value class CorrelationData(override val value: ByteString) :
    WritableProperty<ByteString>,
    Property<ByteString> {

    /** The identifier value of this property is: `0x09`. */
    override val identifier: Int
        get() = PROPERTY_ID_CORRELATION_DATA

    override val writeValue: Sink.(ByteString) -> Unit
        get() = ByteStringWriter

    override fun byteCount(): Int = value.size + 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Subscription Identifier** property as defined in the MQTT specification.
 */
@JvmInline
value class SubscriptionIdentifier(override val value: Int) : WritableProperty<Int> {

    init {
        wellFormedWhen(value != 0) { "Subscription identifiers must not be zero" }
    }

    /** The identifier value of this property is: `0x0B`. */
    // This is a "variable byte integer" property (the only one)
    override val identifier: Int
        get() = PROPERTY_ID_SUBSCRIPTION_IDENTIFIER

    override val writeValue: Sink.(Int) -> Unit
        get() = { writeVariableByteInt(value) }

    override fun byteCount(): Int = value.variableByteIntSize() + 1

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Session Expiry Interval** property as defined in the MQTT specification.
 */
@JvmInline
value class SessionExpiryInterval(override val value: UInt) :
    WritableProperty<UInt>,
    Property<UInt> {

    /** The identifier value of this property is: `0x11`. */
    override val identifier: Int
        get() = PROPERTY_ID_SESSION_EXPIRY_INTERVAL

    override val writeValue: Sink.(UInt) -> Unit
        get() = UIntWriter

    override fun byteCount(): Int = 5

    /** Returns `true` when this interval means the session never expires. */
    val doesNotExpire: Boolean
        get() = value == UInt.MAX_VALUE

    override fun toString(): String = value.toString()
}

/** Converts this `SessionExpiryInterval` to its corresponding [Duration].
 *
 * @return the duration, returns [Duration.INFINITE], if this represents an infinite duration
 */
fun SessionExpiryInterval.toDuration(): Duration = if (doesNotExpire) {
    Duration.INFINITE
} else {
    value.toLong().seconds
}

/** Converts [Duration] to [SessionExpiryInterval]. */
fun Duration.toSessionExpiryInterval(): SessionExpiryInterval = SessionExpiryInterval(inWholeSeconds.toUInt())

/**
 * Value class representing the **Assigned Client Identifier** property as defined in the MQTT specification.
 */
@JvmInline
value class AssignedClientIdentifier(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /** The identifier value of this property is: `0x12`. */
    override val identifier: Int
        get() = PROPERTY_ID_ASSIGNED_CLIENT_IDENTIFIER

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Server Keep Alive** property as defined in the MQTT specification.
 */
@JvmInline
value class ServerKeepAlive(override val value: UShort) :
    WritableProperty<UShort>,
    Property<UShort> {

    /** The identifier value of this property is: `0x13`. */
    override val identifier: Int
        get() = PROPERTY_ID_SERVER_KEEP_ALIVE

    override val writeValue: Sink.(UShort) -> Unit
        get() = UShortWriter

    override fun byteCount(): Int = 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Authentication Method** property as defined in the MQTT specification.
 */
@JvmInline
value class AuthenticationMethod(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /** The identifier value of this property is: `0x15`. */
    override val identifier: Int
        get() = PROPERTY_ID_AUTHENTICATION_METHOD

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Authentication Data** property as defined in the MQTT specification.
 */
@JvmInline
value class AuthenticationData(override val value: ByteString) :
    WritableProperty<ByteString>,
    Property<ByteString> {

    /** The identifier value of this property is: `0x16`. */
    override val identifier: Int
        get() = PROPERTY_ID_AUTHENTICATION_DATA

    override val writeValue: Sink.(ByteString) -> Unit
        get() = ByteStringWriter

    override fun byteCount(): Int = value.size + 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Request Problem Information** property as defined in the MQTT specification.
 */
@JvmInline
value class RequestProblemInformation(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /** The identifier value of this property is: `0x17`. */
    override val identifier: Int
        get() = PROPERTY_ID_REQUEST_PROBLEM_INFORMATION

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Will Delay Interval** property as defined in the MQTT specification.
 */
@JvmInline
value class WillDelayInterval(override val value: UInt) :
    WritableProperty<UInt>,
    Property<UInt> {

    /** The identifier value of this property is: `0x18`. */
    override val identifier: Int
        get() = PROPERTY_ID_WILL_DELAY_INTERVAL

    override val writeValue: Sink.(UInt) -> Unit
        get() = UIntWriter

    override fun byteCount(): Int = 5

    override fun toString(): String = value.toString()
}

/** Converts [WillDelayInterval] to [Duration]. */
fun WillDelayInterval.toDuration(): Duration = value.toLong().seconds

/** Converts [Duration] to [WillDelayInterval]. */
fun Duration.toWillDelayInterval(): WillDelayInterval = WillDelayInterval(inWholeSeconds.toUInt())

/**
 * Value class representing the **Request Response Information** property as defined in the MQTT specification.
 */
@JvmInline
value class RequestResponseInformation(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /** The identifier value of this property is: `0x19`. */
    override val identifier: Int
        get() = PROPERTY_ID_REQUEST_RESPONSE_INFORMATION

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Response Information** property as defined in the MQTT specification.
 */
@JvmInline
value class ResponseInformation(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /** The identifier value of this property is: `0x1A`. */
    override val identifier: Int
        get() = PROPERTY_ID_RESPONSE_INFORMATION

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Server Reference** property as defined in the MQTT specification.
 */
@JvmInline
value class ServerReference(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /** The identifier value of this property is: `0x1C`. */
    override val identifier: Int
        get() = PROPERTY_ID_SERVER_REFERENCE

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Tries to parse the list of servers according to the recommendations of the
 * [MQTT 5 specification](https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Server_redirection).
 *
 * Note that in case no port number is specified, it will be set to 0 in the returned [SocketAddress]
 */
val ServerReference.servers: List<SocketAddress>
    get() {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.trim().split(Regex("\\s+")).mapNotNull { str ->
                try {
                    if (str.startsWith("[")) {
                        val endIndex = str.indexOf(']')
                        if (endIndex <= 0 || endIndex + 1 >= str.length || str[endIndex + 1] != ':') {
                            Logger.e { "Failed to parse server reference: '$str'" }
                            return@mapNotNull null
                        }
                        val server = str.substring(1..<endIndex)
                        val port = str.substring(endIndex + 2)
                        InetSocketAddress(server.trim(), port.toInt())
                    } else if (str.contains(":")) {
                        InetSocketAddress(str.substringBefore(":"), str.substringAfter(":").toInt())
                    } else {
                        InetSocketAddress(str, 0)
                    }
                } catch (ex: NumberFormatException) {
                    Logger.e(throwable = ex) { "Failed to parse server reference: '$str'" }
                    null
                } catch (ex: IllegalArgumentException) {
                    Logger.e(throwable = ex) { "Failed to parse server reference: '$str'" }
                    null
                }
            }
        }
    }

/**
 * Value class representing the **Reason String** property as defined in the MQTT specification.
 */
@JvmInline
value class ReasonString(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /** The identifier value of this property is: `0x1F`. */
    override val identifier: Int
        get() = PROPERTY_ID_REASON_STRING

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/** Converts this nullable [String] to a nullable [ReasonString]. */
fun String?.toReasonString(): ReasonString? = if (this != null) ReasonString(this) else null

/** Returns this reason text or a fallback derived from [reasonCode]. */
fun ReasonString?.ifNull(reasonCode: ReasonCode): String = "${reasonCode.code} ${this?.value ?: reasonCode.name}"

/**
 * Value class representing the **Receive Maximum** property as defined in the MQTT specification.
 */
@JvmInline
value class ReceiveMaximum(override val value: UShort) :
    WritableProperty<UShort>,
    Property<UShort> {

    init {
        malformedWhen(value == 0.toUShort()) { "The Receive Maximum must not be zero." }
    }

    /** The identifier value of this property is: `0x21`. */
    override val identifier: Int
        get() = PROPERTY_ID_RECEIVE_MAXIMUM

    override val writeValue: Sink.(UShort) -> Unit
        get() = UShortWriter

    override fun byteCount(): Int = 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Topic Alias Maximum** property as defined in the MQTT specification.
 */
@JvmInline
value class TopicAliasMaximum(override val value: UShort) :
    WritableProperty<UShort>,
    Property<UShort> {

    /** The identifier value of this property is: `0x22`. */
    override val identifier: Int
        get() = PROPERTY_ID_TOPIC_ALIAS_MAXIMUM

    override val writeValue: Sink.(UShort) -> Unit
        get() = UShortWriter

    override fun byteCount(): Int = 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Topic Alias** property as defined in the MQTT specification.
 */
@JvmInline
value class TopicAlias(override val value: UShort) :
    WritableProperty<UShort>,
    Property<UShort> {

    /** The identifier value of this property is: `0x23`. */
    override val identifier: Int
        get() = PROPERTY_ID_TOPIC_ALIAS

    override val writeValue: Sink.(UShort) -> Unit
        get() = UShortWriter

    override fun byteCount(): Int = 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Maximum QoS** property as defined in the MQTT specification.
 */
@JvmInline
value class MaximumQoS(override val value: Byte) :
    WritableProperty<Byte>,
    Property<Byte> {

    /** The identifier value of this property is: `0x24`. */
    override val identifier: Int
        get() = PROPERTY_ID_MAXIMUM_QOS

    override val writeValue: Sink.(Byte) -> Unit
        get() = ByteWriter

    override fun byteCount(): Int = 2

    /** Converts this value to [QoS]. */
    val qoS: QoS
        get() = QoS.from(value.toInt())

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Retain Available** property as defined in the MQTT specification.
 */
@JvmInline
value class RetainAvailable(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /** The identifier value of this property is: `0x25`. */
    override val identifier: Int
        get() = PROPERTY_ID_RETAIN_AVAILABLE

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **User Property** property as defined in the MQTT specification.
 */
@JvmInline
value class UserProperty(override val value: StringPair) :
    WritableProperty<StringPair>,
    Property<StringPair> {

    /** The identifier value of this property is: `0x26`. */
    override val identifier: Int
        get() = PROPERTY_ID_USER_PROPERTY

    override val writeValue: Sink.(StringPair) -> Unit
        get() = { write(it) }

    override fun byteCount(): Int {
        return value.name.utf8Size() + value.value.utf8Size() + 5 // 1 for the identifier 2 + 2 for the string lengths
    }

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Maximum Packet Size** property as defined in the MQTT specification.
 */
@JvmInline
value class MaximumPacketSize(override val value: UInt) :
    WritableProperty<UInt>,
    Property<UInt> {

    init {
        malformedWhen(value == 0.toUInt()) { "The Maximum Packet Size must not be zero." }
    }

    /** The identifier value of this property is: `0x27`. */
    override val identifier: Int
        get() = PROPERTY_ID_MAXIMUM_PACKET_SIZE

    override val writeValue: Sink.(UInt) -> Unit
        get() = UIntWriter

    override fun byteCount(): Int = 5

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Wildcard Subscription Available** property as defined in the MQTT specification.
 */
@JvmInline
value class WildcardSubscriptionAvailable(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /** The identifier value of this property is: `0x28`. */
    override val identifier: Int
        get() = PROPERTY_ID_WILDCARD_SUBSCRIPTION_AVAILABLE

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Subscription Identifier Available** property as defined in the MQTT specification.
 */
@JvmInline
value class SubscriptionIdentifierAvailable(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /** The identifier value of this property is: `0x29`. */
    override val identifier: Int
        get() = PROPERTY_ID_SUBSCRIPTION_IDENTIFIER_AVAILABLE

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

/** Returns `true` when subscription identifiers are available, or unspecified. */
fun SubscriptionIdentifierAvailable?.isAvailable(): Boolean = this == null || this.value

/**
 * Value class representing the **Shared Subscription Available** property as defined in the MQTT specification.
 */
@JvmInline
value class SharedSubscriptionAvailable(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /** The identifier value of this property is: `0x2A`. */
    override val identifier: Int
        get() = PROPERTY_ID_SHARED_SUBSCRIPTION_AVAILABLE

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

// ---- Helper functions/classes ---------------------------------------------------------------------------------------

private interface WritableProperty<T> : Property<T> {

    val identifier: Int

    val writeValue: Sink.(T) -> Unit

    /**
     * The number of bytes which are used by this property when encoded in MQTT format.
     */
    fun byteCount(): Int
}

private val ByteWriter: Sink.(Byte) -> Unit = {
    writeByte(it)
}

private val UShortWriter: Sink.(UShort) -> Unit = {
    writeUShort(it)
}

private val UIntWriter: Sink.(UInt) -> Unit = {
    writeUInt(it)
}

private val StringWriter: Sink.(String) -> Unit = {
    writeMqttString(it)
}

private val ByteStringWriter: Sink.(ByteString) -> Unit = {
    // Do NOT(!) use ByteWriteChannel.writeFully(...) as this will not write the size of the byte array
    writeMqttByteString(it)
}

private val BooleanWriter: Sink.(Boolean) -> Unit = {
    writeByte(if (it) BYTE_TRUE else BYTE_FALSE)
}

private fun byteToBoolean(byte: Byte, constructor: (Boolean) -> Property<Boolean>): Property<Boolean> = when (byte) {
    BYTE_FALSE -> constructor(false)

    BYTE_TRUE -> constructor(true)

    else -> throw MalformedPacketException(
        "Value $byte not allowed, only 0 and 1 are allowed for boolean properties",
    )
}
