package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.util.*
import io.ktor.network.sockets.*
import kotlinx.io.*
import kotlinx.io.bytestring.ByteString
import kotlin.jvm.JvmInline
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Represents the MQTT property as defined in chapter 2.2.2 of the MQTT specification.
 */
public sealed interface Property<T> {

    /**
     * The value of this property
     */
    public val value: T
}

/**
 * Returns the property of the specified type, when contained in the list.
 *
 * @throws io.github.nicolasfara.mktt.core.MalformedPacketException when the property is not contained exactly once in the list
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
 * Returns the property of the specified type, when contained in the list or `null` otherwise
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

internal fun Source.readProperty(): Property<*> = when (val identifier = (readByte().toInt() and 0xFF)) {
    1 -> PayloadFormatIndicator.from(readByte())
    2 -> MessageExpiryInterval(readUInt())
    3 -> ContentType(readMqttString())
    8 -> ResponseTopic(readMqttString())
    9 -> CorrelationData(readMqttByteString())
    11 -> SubscriptionIdentifier(readVariableByteInt())
    17 -> SessionExpiryInterval(readUInt())
    18 -> AssignedClientIdentifier(readMqttString())
    19 -> ServerKeepAlive(readUShort())
    21 -> AuthenticationMethod(readMqttString())
    22 -> AuthenticationData(readMqttByteString())
    23 -> byteToBoolean(readByte()) { RequestProblemInformation(it) }
    24 -> WillDelayInterval(readUInt())
    25 -> byteToBoolean(readByte()) { RequestResponseInformation(it) }
    26 -> ResponseInformation(readMqttString())
    28 -> ServerReference(readMqttString())
    31 -> ReasonString(readMqttString())
    33 -> ReceiveMaximum(readUShort())
    34 -> TopicAliasMaximum(readUShort())
    35 -> TopicAlias(readUShort())
    36 -> MaximumQoS(readByte())
    37 -> byteToBoolean(readByte()) { RetainAvailable(it) }
    38 -> UserProperty(readStringPair())
    39 -> MaximumPacketSize(readUInt())
    40 -> byteToBoolean(readByte()) { WildcardSubscriptionAvailable(it) }
    41 -> byteToBoolean(readByte()) { SubscriptionIdentifierAvailable(it) }
    42 -> byteToBoolean(readByte()) { SharedSubscriptionAvailable(it) }
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
public value class PayloadFormatIndicator private constructor(override val value: Byte) :
    WritableProperty<Byte>,
    Property<Byte> {

    /**
     * The identifier value of this property is: `0x01`
     */
    public override val identifier: Int
        get() = 1

    public override val writeValue: Sink.(Byte) -> Unit
        get() = ByteWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()

    public companion object {

        public fun from(byte: Byte): PayloadFormatIndicator = when (byte) {
            0.toByte() -> NONE
            1.toByte() -> UTF_8
            else -> throw MalformedPacketException("Value of $byte not allowed for payload format indicator")
        }

        public val NONE: PayloadFormatIndicator = PayloadFormatIndicator(0)

        public val UTF_8: PayloadFormatIndicator = PayloadFormatIndicator(1)
    }
}

/**
 * Value class representing the **Message Expiry Interval** property as defined in the MQTT specification.
 */
@JvmInline
public value class MessageExpiryInterval(override val value: UInt) :
    WritableProperty<UInt>,
    Property<UInt> {

    /**
     * The identifier value of this property is: `0x02`
     */
    public override val identifier: Int
        get() = 2

    override val writeValue: Sink.(UInt) -> Unit
        get() = UIntWriter

    override fun byteCount(): Int = 5

    override fun toString(): String = value.toString()
}

public fun MessageExpiryInterval.toDuration(): Duration = value.toLong().seconds

public fun Duration.toMessageExpiryInterval(): MessageExpiryInterval = MessageExpiryInterval(inWholeSeconds.toUInt())

/**
 * Value class representing the **Content Type** property as defined in the MQTT specification.
 */
@JvmInline
public value class ContentType(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /**
     * The identifier value of this property is: `0x03`
     */
    public override val identifier: Int
        get() = 3

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Response Topic** property as defined in the MQTT specification.
 */
@JvmInline
public value class ResponseTopic(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /**
     * The identifier value of this property is: `0x08`
     */
    public override val identifier: Int
        get() = 8

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Correlation Data** property as defined in the MQTT specification.
 */
@JvmInline
public value class CorrelationData(override val value: ByteString) :
    WritableProperty<ByteString>,
    Property<ByteString> {

    /**
     * The identifier value of this property is: `0x09`
     */
    public override val identifier: Int
        get() = 9

    override val writeValue: Sink.(ByteString) -> Unit
        get() = ByteStringWriter

    override fun byteCount(): Int = value.size + 1

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Subscription Identifier** property as defined in the MQTT specification.
 */
@JvmInline
public value class SubscriptionIdentifier(override val value: Int) : WritableProperty<Int> {

    init {
        wellFormedWhen(value != 0) { "Subscription identifiers must not be zero" }
    }

    /**
     * The identifier value of this property is: `0x0B`
     */
    // This is a "variable byte integer" property (the only one)
    public override val identifier: Int
        get() = 11

    override val writeValue: Sink.(Int) -> Unit
        get() = { writeVariableByteInt(value) }

    override fun byteCount(): Int = value.variableByteIntSize() + 1

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Session Expiry Interval** property as defined in the MQTT specification.
 */
@JvmInline
public value class SessionExpiryInterval(override val value: UInt) :
    WritableProperty<UInt>,
    Property<UInt> {

    /**
     * The identifier value of this property is: `0x11`
     */
    public override val identifier: Int
        get() = 17

    override val writeValue: Sink.(UInt) -> Unit
        get() = UIntWriter

    override fun byteCount(): Int = 5

    public val doesNotExpire: Boolean
        get() = value == UInt.MAX_VALUE

    override fun toString(): String = value.toString()
}

/**
 * Converts this `SessionExpiryInterval` to its corresponding [Duration].
 *
 * @return the duration, returns [Duration.INFINITE], if this represents an infinite duration
 */
public fun SessionExpiryInterval.toDuration(): Duration = if (doesNotExpire) {
    Duration.INFINITE
} else {
    value.toLong().seconds
}

public fun Duration.toSessionExpiryInterval(): SessionExpiryInterval = SessionExpiryInterval(inWholeSeconds.toUInt())

/**
 * Value class representing the **Assigned Client Identifier** property as defined in the MQTT specification.
 */
@JvmInline
public value class AssignedClientIdentifier(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /**
     * The identifier value of this property is: `0x12`
     */
    public override val identifier: Int
        get() = 18

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Server Keep Alive** property as defined in the MQTT specification.
 */
@JvmInline
public value class ServerKeepAlive(override val value: UShort) :
    WritableProperty<UShort>,
    Property<UShort> {

    /**
     * The identifier value of this property is: `0x13`
     */
    public override val identifier: Int
        get() = 19

    override val writeValue: Sink.(UShort) -> Unit
        get() = UShortWriter

    override fun byteCount(): Int = 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Authentication Method** property as defined in the MQTT specification.
 */
@JvmInline
public value class AuthenticationMethod(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /**
     * The identifier value of this property is: `0x15`
     */
    public override val identifier: Int
        get() = 21

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Authentication Data** property as defined in the MQTT specification.
 */
@JvmInline
public value class AuthenticationData(override val value: ByteString) :
    WritableProperty<ByteString>,
    Property<ByteString> {

    /**
     * The identifier value of this property is: `0x16`
     */
    public override val identifier: Int
        get() = 22

    override val writeValue: Sink.(ByteString) -> Unit
        get() = ByteStringWriter

    override fun byteCount(): Int = value.size + 1

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Request Problem Information** property as defined in the MQTT specification.
 */
@JvmInline
public value class RequestProblemInformation(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /**
     * The identifier value of this property is: `0x17`
     */
    public override val identifier: Int
        get() = 23

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Will Delay Interval** property as defined in the MQTT specification.
 */
@JvmInline
public value class WillDelayInterval(override val value: UInt) :
    WritableProperty<UInt>,
    Property<UInt> {

    /**
     * The identifier value of this property is: `0x18`
     */
    public override val identifier: Int
        get() = 24

    override val writeValue: Sink.(UInt) -> Unit
        get() = UIntWriter

    override fun byteCount(): Int = 5

    override fun toString(): String = value.toString()
}

public fun WillDelayInterval.toDuration(): Duration = value.toLong().seconds

public fun Duration.toWillDelayInterval(): WillDelayInterval = WillDelayInterval(inWholeSeconds.toUInt())

/**
 * Value class representing the **Request Response Information** property as defined in the MQTT specification.
 */
@JvmInline
public value class RequestResponseInformation(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /**
     * The identifier value of this property is: `0x19`
     */
    public override val identifier: Int
        get() = 25

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Response Information** property as defined in the MQTT specification.
 */
@JvmInline
public value class ResponseInformation(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /**
     * The identifier value of this property is: `0x1A`
     */
    public override val identifier: Int
        get() = 26

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

/**
 * Value class representing the **Server Reference** property as defined in the MQTT specification.
 */
@JvmInline
public value class ServerReference(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /**
     * The identifier value of this property is: `0x1C`
     */
    public override val identifier: Int
        get() = 28

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
public val ServerReference.servers: List<SocketAddress>
    get() {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.trim().split(Regex("\\s+")).mapNotNull { str ->
                try {
                    if (str.startsWith("[")) {
                        val endIndex = str.indexOf(']')
                        val server = str.substring(1..<endIndex)
                        val port = str.substring(str.indexOf(':', endIndex) + 1)
                        InetSocketAddress(server.trim(), port.toInt())
                    } else if (str.contains(":")) {
                        InetSocketAddress(str.substringBefore(":"), str.substringAfter(":").toInt())
                    } else {
                        InetSocketAddress(str, 0)
                    }
                } catch (ex: Exception) {
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
public value class ReasonString(override val value: String) :
    WritableProperty<String>,
    Property<String> {

    /**
     * The identifier value of this property is: `0x1F`
     */
    public override val identifier: Int
        get() = 31

    override val writeValue: Sink.(String) -> Unit
        get() = StringWriter

    override fun byteCount(): Int = value.utf8Size() + 3

    override fun toString(): String = value
}

public fun String?.toReasonString(): ReasonString? = if (this != null) ReasonString(this) else null

public fun ReasonString?.ifNull(reasonCode: ReasonCode): String = "${reasonCode.code} ${this?.value ?: reasonCode.name}"

/**
 * Value class representing the **Receive Maximum** property as defined in the MQTT specification.
 */
@JvmInline
public value class ReceiveMaximum(override val value: UShort) :
    WritableProperty<UShort>,
    Property<UShort> {

    init {
        malformedWhen(value == 0.toUShort()) { "The Receive Maximum must not be zero." }
    }

    /**
     * The identifier value of this property is: `0x21`
     */
    public override val identifier: Int
        get() = 33

    override val writeValue: Sink.(UShort) -> Unit
        get() = UShortWriter

    override fun byteCount(): Int = 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Topic Alias Maximum** property as defined in the MQTT specification.
 */
@JvmInline
public value class TopicAliasMaximum(override val value: UShort) :
    WritableProperty<UShort>,
    Property<UShort> {

    /**
     * The identifier value of this property is: `0x22`
     */
    public override val identifier: Int
        get() = 34

    override val writeValue: Sink.(UShort) -> Unit
        get() = UShortWriter

    override fun byteCount(): Int = 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Topic Alias** property as defined in the MQTT specification.
 */
@JvmInline
public value class TopicAlias(override val value: UShort) :
    WritableProperty<UShort>,
    Property<UShort> {

    /**
     * The identifier value of this property is: `0x23`
     */
    public override val identifier: Int
        get() = 35

    override val writeValue: Sink.(UShort) -> Unit
        get() = UShortWriter

    override fun byteCount(): Int = 3

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Maximum QoS** property as defined in the MQTT specification.
 */
@JvmInline
public value class MaximumQoS(override val value: Byte) :
    WritableProperty<Byte>,
    Property<Byte> {

    /**
     * The identifier value of this property is: `0x24`
     */
    public override val identifier: Int
        get() = 36

    override val writeValue: Sink.(Byte) -> Unit
        get() = ByteWriter

    override fun byteCount(): Int = 2

    public val qoS: QoS
        get() = QoS.from(value.toInt())

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Retain Available** property as defined in the MQTT specification.
 */
@JvmInline
public value class RetainAvailable(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /**
     * The identifier value of this property is: `0x25`
     */
    public override val identifier: Int
        get() = 37

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **User Property** property as defined in the MQTT specification.
 */
@JvmInline
public value class UserProperty(override val value: StringPair) :
    WritableProperty<StringPair>,
    Property<StringPair> {

    /**
     * The identifier value of this property is: `0x26`
     */
    public override val identifier: Int
        get() = 38

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
public value class MaximumPacketSize(override val value: UInt) :
    WritableProperty<UInt>,
    Property<UInt> {

    init {
        malformedWhen(value == 0.toUInt()) { "The Maximum Packet Size must not be zero." }
    }

    /**
     * The identifier value of this property is: `0x27`
     */
    public override val identifier: Int
        get() = 39

    override val writeValue: Sink.(UInt) -> Unit
        get() = UIntWriter

    override fun byteCount(): Int = 5

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Wildcard Subscription Available** property as defined in the MQTT specification.
 */
@JvmInline
public value class WildcardSubscriptionAvailable(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /**
     * The identifier value of this property is: `0x28`
     */
    public override val identifier: Int
        get() = 40

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

/**
 * Value class representing the **Subscription Identifier Available** property as defined in the MQTT specification.
 */
@JvmInline
public value class SubscriptionIdentifierAvailable(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /**
     * The identifier value of this property is: `0x29`
     */
    public override val identifier: Int
        get() = 41

    override val writeValue: Sink.(Boolean) -> Unit
        get() = BooleanWriter

    override fun byteCount(): Int = 2

    override fun toString(): String = value.toString()
}

public fun SubscriptionIdentifierAvailable?.isAvailable(): Boolean = this == null || this.value

/**
 * Value class representing the **Shared Subscription Available** property as defined in the MQTT specification.
 */
@JvmInline
public value class SharedSubscriptionAvailable(override val value: Boolean) :
    WritableProperty<Boolean>,
    Property<Boolean> {

    /**
     * The identifier value of this property is: `0x2A`
     */
    public override val identifier: Int
        get() = 42

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

private val ShortWriter: Sink.(Short) -> Unit = {
    writeShort(it)
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
    writeMqttByteString(it) // Do NOT(!) use ByteWriteChannel.writeFully(...) as this will not write the size of the byte array
}

private val BooleanWriter: Sink.(Boolean) -> Unit = {
    writeByte(if (it) 1 else 0)
}

private fun byteToBoolean(byte: Byte, constructor: (Boolean) -> Property<Boolean>): Property<Boolean> = when (byte) {
    0.toByte() -> constructor(false)

    1.toByte() -> constructor(true)

    else -> throw MalformedPacketException(
        "Value $byte not allowed, only 0 and 1 are allowed for boolean properties",
    )
}
