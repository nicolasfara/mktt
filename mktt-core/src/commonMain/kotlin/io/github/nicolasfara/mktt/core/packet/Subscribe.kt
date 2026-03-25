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

data class Subscribe(
    override val packetIdentifier: UShort,
    val filters: List<TopicFilter>,
    val subscriptionIdentifier: SubscriptionIdentifier? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
) : AbstractPacket(PacketType.SUBSCRIBE),
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
        if (isNoLocal) bits = bits or 4
        if (retainAsPublished) bits = bits or 8
        bits = bits or (retainHandling.value shl 4)

        return bits.toByte()
    }

private fun Byte.toSubscriptionOptions(): SubscriptionOptions {
    val bits = toInt()
    val qoS = QoS.from(bits and 3)
    val isNoLocal = (bits and 4) shr 2 != 0
    val retainAsPublished = (bits and 8) shr 3 != 0
    return SubscriptionOptions(
        qoS,
        isNoLocal,
        retainAsPublished,
        RetainHandling.from((bits and 48) shr 4),
    )
}
