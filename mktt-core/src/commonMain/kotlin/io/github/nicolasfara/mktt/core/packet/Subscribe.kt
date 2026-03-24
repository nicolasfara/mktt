package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.packet.bits
import io.github.nicolasfara.mktt.core.packet.toSubscriptionOptions
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

public data class Subscribe(
    public override val packetIdentifier: UShort,
    public val filters: List<io.github.nicolasfara.mktt.core.TopicFilter>,
    public val subscriptionIdentifier: io.github.nicolasfara.mktt.core.SubscriptionIdentifier? = null,
    public val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.SUBSCRIBE,
),
    io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket {

    init {
        _root_ide_package_.io.github.nicolasfara.mktt.core.malformedWhen(filters.isEmpty()) {
            "SUBSCRIBE MUST contain at least one Topic Filter [MQTT-3.8.3-2]"
        }
    }

    override val headerFlags: Int = 2
}

internal fun Sink.write(subscribe: io.github.nicolasfara.mktt.core.packet.Subscribe) {
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

internal fun Source.readSubscribe(): io.github.nicolasfara.mktt.core.packet.Subscribe {
    val packetIdentifier = readUShort()
    val properties = readProperties()
    val filters = buildList {
        while (!exhausted()) {
            val filter = readMqttString()
            val options = readByte().toSubscriptionOptions()
            add(
                _root_ide_package_.io.github.nicolasfara.mktt.core.TopicFilter(
                    _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(
                        filter,
                    ),
                    options,
                ),
            )
        }
    }

    return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Subscribe(
        packetIdentifier = packetIdentifier,
        filters = filters,
        subscriptionIdentifier = properties.singleOrNull<io.github.nicolasfara.mktt.core.SubscriptionIdentifier>(),
        userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(properties),
    )
}

private val io.github.nicolasfara.mktt.core.SubscriptionOptions.bits: Byte
    get() {
        var bits = qoS.value
        if (isNoLocal) bits = bits or 4
        if (retainAsPublished) bits = bits or 8
        bits = bits or (retainHandling.value shl 4)

        return bits.toByte()
    }

private fun Byte.toSubscriptionOptions(): io.github.nicolasfara.mktt.core.SubscriptionOptions {
    val bits = toInt()
    val qoS = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.Companion.from(bits and 3)
    val isNoLocal = (bits and 4) shr 2 != 0
    val retainAsPublished = (bits and 8) shr 3 != 0

    return _root_ide_package_.io.github.nicolasfara.mktt.core.SubscriptionOptions(
        qoS,
        isNoLocal,
        retainAsPublished,
        _root_ide_package_.io.github.nicolasfara.mktt.core.RetainHandling.Companion.from((bits and 48) shr 4),
    )
}
