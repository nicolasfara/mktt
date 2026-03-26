package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.ContentType
import io.github.nicolasfara.mktt.core.CorrelationData
import io.github.nicolasfara.mktt.core.MessageExpiryInterval
import io.github.nicolasfara.mktt.core.PayloadFormatIndicator
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.ResponseTopic
import io.github.nicolasfara.mktt.core.SubscriptionIdentifier
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicAlias
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.malformedWhen
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import io.github.nicolasfara.mktt.core.wellFormedWhen
import io.github.nicolasfara.mktt.core.writeProperties
import io.ktor.utils.io.core.remaining
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.readByteString
import kotlinx.io.readUShort
import kotlinx.io.write
import kotlinx.io.writeUShort

private const val RETAIN_FLAG_MASK = 0b0000_0001
private const val QOS_MASK = 0b0000_0110
private const val QOS_SHIFT = 1
private const val DUP_FLAG_MASK = 0b0000_1000
private const val DUP_FLAG_SHIFT = 3

/**
 * MQTT PUBLISH packet carrying an application message from client to server or server to client.
 *
 * @property isDupMessage whether this packet is a re-delivery of an earlier PUBLISH.
 * @property qoS the quality of service level for this message.
 * @property isRetainMessage whether the broker should retain this message as the latest value for the topic.
 * @property packetIdentifier packet identifier for QoS 1 and QoS 2 publishes, `null` for QoS 0.
 * @property topic topic name of this message.
 * @property payloadFormatIndicator optional MQTT payload format indicator property.
 * @property messageExpiryInterval optional message expiry interval property.
 * @property topicAlias optional topic alias property.
 * @property responseTopic optional response topic property.
 * @property correlationData optional correlation data property.
 * @property userProperties optional user properties attached to this packet.
 * @property subscriptionIdentifier optional subscription identifier property.
 * @property contentType optional content type property.
 * @property payload binary payload of the published message.
 */
data class Publish(
    val isDupMessage: Boolean = false,
    val qoS: QoS = QoS.AT_MOST_ONCE,
    val isRetainMessage: Boolean = false,
    val packetIdentifier: UShort? = null,
    val topic: Topic,
    val payloadFormatIndicator: PayloadFormatIndicator? = null,
    val messageExpiryInterval: MessageExpiryInterval? = null,
    val topicAlias: TopicAlias? = null,
    val responseTopic: ResponseTopic? = null,
    val correlationData: CorrelationData? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
    val subscriptionIdentifier: SubscriptionIdentifier? = null,
    val contentType: ContentType? = null,
    val payload: ByteString,
) : BasePacket(PacketType.PUBLISH) {
    init {
        wellFormedWhen(topic.isNotBlank() || topicAlias != null) {
            "It is a Protocol Error if the Topic Name is zero length and there is no Topic Alias"
        }
        wellFormedWhen(topicAlias == null || topicAlias.value != 0.toUShort()) {
            "A Topic Alias of 0 is not permitted [MQTT-3.3.2-8]"
        }
        malformedWhen(qoS == QoS.AT_MOST_ONCE && packetIdentifier != null) {
            "A PUBLISH packet MUST NOT contain a Packet Identifier if its QoS value is set to 0 [MQTT-2.2.1-2]"
        }
        malformedWhen(qoS.requiresPacketIdentifier && packetIdentifier == null) {
            "A PUBLISH packet with QoS > 0 must contain a packet identifier [MQTT-2.2.1-4]"
        }
    }

    override val headerFlags: Int
        get() {
            var bits = if (isRetainMessage) RETAIN_FLAG_MASK else 0
            bits = bits or (qoS.value shl QOS_SHIFT)
            if (isDupMessage) {
                bits = bits or DUP_FLAG_MASK
            }
            return bits
        }
}

internal fun Sink.write(publish: Publish) {
    with(publish) {
        this@write.writeMqttString(topic.name)
        if (qoS.requiresPacketIdentifier) {
            val identifier = requireNotNull(packetIdentifier) {
                "PUBLISH packet identifier is required for QoS > 0"
            }
            writeUShort(identifier)
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

internal fun Source.readPublish(headerFlags: Int): Publish {
    val qoS = headerFlags.qoS
    val topicName = readMqttString()
    val packetIdentifier = if (qoS.requiresPacketIdentifier) {
        readUShort()
    } else {
        null
    }
    val properties = readProperties()
    val payload = readByteString(remaining.toInt())

    return Publish(
        isDupMessage = headerFlags.isDupMessage,
        qoS = qoS,
        isRetainMessage = headerFlags.isRetainMessage,
        packetIdentifier = packetIdentifier,
        topic = Topic(topicName),
        payloadFormatIndicator = properties.singleOrNull<PayloadFormatIndicator>(),
        messageExpiryInterval = properties.singleOrNull<MessageExpiryInterval>(),
        topicAlias = properties.singleOrNull<TopicAlias>(),
        responseTopic = properties.singleOrNull<ResponseTopic>(),
        correlationData = properties.singleOrNull<CorrelationData>(),
        userProperties = UserProperties.from(properties),
        subscriptionIdentifier = properties.singleOrNull<SubscriptionIdentifier>(),
        contentType = properties.singleOrNull<ContentType>(),
        payload = payload,
    )
}

private val QoS.requiresPacketIdentifier: Boolean
    get() = this.value > 0

private val Int.qoS: QoS
    get() = QoS.from((this and QOS_MASK) shr QOS_SHIFT)

private val Int.isDupMessage: Boolean
    get() = ((this and DUP_FLAG_MASK) shr DUP_FLAG_SHIFT) != 0

private val Int.isRetainMessage: Boolean
    get() = (this and RETAIN_FLAG_MASK) != 0
