package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.RetainHandling
import io.github.nicolasfara.mktt.core.SubscriptionIdentifier
import io.github.nicolasfara.mktt.core.SubscriptionOptions
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.malformedWhen
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

private const val QOS_MASK = 0b0000_0011
private const val NO_LOCAL_MASK = 0b0000_0100
private const val RETAIN_AS_PUBLISHED_MASK = 0b0000_1000
private const val RETAIN_HANDLING_MASK = 0b0011_0000
private const val NO_LOCAL_SHIFT = 2
private const val RETAIN_AS_PUBLISHED_SHIFT = 3
private const val RETAIN_HANDLING_SHIFT = 4

/**
 * MQTT SUBSCRIBE packet used to request one or more topic filter subscriptions.
 *
 * @property packetIdentifier packet identifier of this subscribe request.
 * @property filters topic filters and options requested by the client.
 * @property subscriptionIdentifier optional subscription identifier for this request.
 * @property userProperties optional user properties attached to this packet.
 */
data class Subscribe(
    override val packetIdentifier: UShort,
    val filters: List<TopicFilter>,
    val subscriptionIdentifier: SubscriptionIdentifier? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
) : BasePacket(PacketType.SUBSCRIBE),
    PacketIdentifierPacket {

    init {
        malformedWhen(filters.isEmpty()) {
            "SUBSCRIBE MUST contain at least one Topic Filter [MQTT-3.8.3-2]"
        }
    }

    override val headerFlags: Int = 2
}

internal fun Sink.write(subscribe: Subscribe) {
    with(subscribe) {
        writeUShort(subscribe.packetIdentifier)
        writeProperties(
            subscriptionIdentifier,
            *userProperties.asArray,
        )

        // Filters are written as payload
        filters.forEach {
            writeMqttString(it.filter.name)
            writeByte(it.subscriptionOptions.bits)
        }
    }
}

internal fun Source.readSubscribe(): Subscribe {
    val packetIdentifier = readUShort()
    val properties = readProperties()
    val filters = buildList {
        while (!exhausted()) {
            val filter = readMqttString()
            val options = readByte().toSubscriptionOptions()
            add(
                TopicFilter(
                    Topic(filter),
                    options,
                ),
            )
        }
    }
    return Subscribe(
        packetIdentifier = packetIdentifier,
        filters = filters,
        subscriptionIdentifier = properties.singleOrNull<SubscriptionIdentifier>(),
        userProperties = UserProperties.from(properties),
    )
}

private val SubscriptionOptions.bits: Byte
    get() {
        var bits = qoS.value
        if (isNoLocal) bits = bits or NO_LOCAL_MASK
        if (retainAsPublished) bits = bits or RETAIN_AS_PUBLISHED_MASK
        bits = bits or (retainHandling.value shl RETAIN_HANDLING_SHIFT)

        return bits.toByte()
    }

private fun Byte.toSubscriptionOptions(): SubscriptionOptions {
    val bits = toInt()
    val qoS = QoS.from(bits and QOS_MASK)
    val isNoLocal = (bits and NO_LOCAL_MASK) shr NO_LOCAL_SHIFT != 0
    val retainAsPublished =
        (bits and RETAIN_AS_PUBLISHED_MASK) shr RETAIN_AS_PUBLISHED_SHIFT != 0
    return SubscriptionOptions(
        qoS,
        isNoLocal,
        retainAsPublished,
        RetainHandling.from((bits and RETAIN_HANDLING_MASK) shr RETAIN_HANDLING_SHIFT),
    )
}
