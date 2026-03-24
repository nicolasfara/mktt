package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.packet.isDupMessage
import io.github.nicolasfara.mktt.core.packet.isRetainMessage
import io.github.nicolasfara.mktt.core.packet.qoS
import io.github.nicolasfara.mktt.core.packet.requiresPacketIdentifier
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import io.github.nicolasfara.mktt.core.writeProperties
import io.ktor.utils.io.core.*
import kotlinx.io.*
import kotlinx.io.bytestring.ByteString

public data class Publish(
    val isDupMessage: Boolean = false,
    val qoS: io.github.nicolasfara.mktt.core.QoS = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE,
    val isRetainMessage: Boolean = false,
    val packetIdentifier: UShort? = null,
    val topic: io.github.nicolasfara.mktt.core.Topic,
    val payloadFormatIndicator: io.github.nicolasfara.mktt.core.PayloadFormatIndicator? = null,
    val messageExpiryInterval: io.github.nicolasfara.mktt.core.MessageExpiryInterval? = null,
    val topicAlias: io.github.nicolasfara.mktt.core.TopicAlias? = null,
    val responseTopic: io.github.nicolasfara.mktt.core.ResponseTopic? = null,
    val correlationData: io.github.nicolasfara.mktt.core.CorrelationData? = null,
    val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
    val subscriptionIdentifier: io.github.nicolasfara.mktt.core.SubscriptionIdentifier? = null,
    val contentType: io.github.nicolasfara.mktt.core.ContentType? = null,
    val payload: ByteString,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBLISH,
) {

    init {
        _root_ide_package_.io.github.nicolasfara.mktt.core.wellFormedWhen(topic.isNotBlank() || topicAlias != null) {
            "It is a Protocol Error if the Topic Name is zero length and there is no Topic Alias"
        }
        _root_ide_package_.io.github.nicolasfara.mktt.core.wellFormedWhen(
            topicAlias == null || topicAlias.value != 0.toUShort(),
        ) {
            "A Topic Alias of 0 is not permitted [MQTT-3.3.2-8]"
        }
        _root_ide_package_.io.github.nicolasfara.mktt.core.malformedWhen(
            qoS == _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE && packetIdentifier != null,
        ) {
            "A PUBLISH packet MUST NOT contain a Packet Identifier if its QoS value is set to 0 [MQTT-2.2.1-2]"
        }
        _root_ide_package_.io.github.nicolasfara.mktt.core.malformedWhen(
            qoS.requiresPacketIdentifier && packetIdentifier == null,
        ) {
            "A PUBLISH packet with QoS > 0 must contain a packet identifier [MQTT-2.2.1-4]"
        }
    }

    override val headerFlags: Int
        get() {
            var bits = if (isRetainMessage) 1 else 0
            bits = bits or (qoS.value shl 1)
            if (isDupMessage) bits = bits or (1 shl 3)
            return bits
        }
}

internal fun Sink.write(publish: io.github.nicolasfara.mktt.core.packet.Publish) {
    with(publish) {
        writeMqttString(topic.name)
        if (qoS.requiresPacketIdentifier) {
            writeUShort(packetIdentifier!!)
        }
        writeProperties(
            payloadFormatIndicator,
            messageExpiryInterval,
            topicAlias,
            responseTopic,
            correlationData,
            *userProperties.asArray,
            subscriptionIdentifier,
            contentType,
        )
        write(payload)
    }
}

internal fun Source.readPublish(headerFlags: Int): io.github.nicolasfara.mktt.core.packet.Publish {
    val qoS = headerFlags.qoS
    val topicName = readMqttString()
    val packetIdentifier = if (qoS.requiresPacketIdentifier) {
        readUShort()
    } else {
        null
    }
    val properties = readProperties()
    val payload = readByteString(remaining.toInt())

    return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Publish(
        isDupMessage = headerFlags.isDupMessage,
        qoS = qoS,
        isRetainMessage = headerFlags.isRetainMessage,
        packetIdentifier = packetIdentifier,
        topic = _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(topicName),
        payloadFormatIndicator = properties.singleOrNull<io.github.nicolasfara.mktt.core.PayloadFormatIndicator>(),
        messageExpiryInterval = properties.singleOrNull<io.github.nicolasfara.mktt.core.MessageExpiryInterval>(),
        topicAlias = properties.singleOrNull<io.github.nicolasfara.mktt.core.TopicAlias>(),
        responseTopic = properties.singleOrNull<io.github.nicolasfara.mktt.core.ResponseTopic>(),
        correlationData = properties.singleOrNull<io.github.nicolasfara.mktt.core.CorrelationData>(),
        userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(properties),
        subscriptionIdentifier = properties.singleOrNull<io.github.nicolasfara.mktt.core.SubscriptionIdentifier>(),
        contentType = properties.singleOrNull<io.github.nicolasfara.mktt.core.ContentType>(),
        payload = payload,
    )
}

private val io.github.nicolasfara.mktt.core.QoS.requiresPacketIdentifier: Boolean
    get() = this.value > 0

private val Int.qoS: io.github.nicolasfara.mktt.core.QoS
    get() = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.Companion.from(((this and 6) shr 1))

private val Int.isDupMessage: Boolean
    get() = ((this and 8) shr 3) != 0

private val Int.isRetainMessage: Boolean
    get() = (this and 1) != 0
