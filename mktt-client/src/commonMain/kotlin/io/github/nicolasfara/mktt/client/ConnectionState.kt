package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.ContentType
import io.github.nicolasfara.mktt.core.CorrelationData
import io.github.nicolasfara.mktt.core.PayloadFormatIndicator
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.ResponseTopic
import io.github.nicolasfara.mktt.core.SubscriptionIdentifier
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicFilter
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.packet.Connack
import io.github.nicolasfara.mktt.core.packet.Suback
import io.github.nicolasfara.mktt.core.packet.Unsuback

typealias ConnAck = Connack
typealias SubAck = Suback
typealias UnsubAck = Unsuback
typealias DisconnectReason = ReasonCode
typealias Subscription = TopicFilter
typealias PublishResult = PublishResponse

/**
 * Observable connection lifecycle states of [MqttClient].
 */
sealed interface MqttConnectionState {
    /**
     * Connection handshake is in progress.
     */
    data object Connecting : MqttConnectionState

    /**
     * Connection is established and the broker accepted CONNECT.
     *
     * @property connack the successful CONNACK packet.
     */
    data class Connected(val connack: ConnAck) : MqttConnectionState

    /**
     * Client is not connected to the broker.
     */
    data object Disconnected : MqttConnectionState

    /**
     * Connection attempt or active session failed.
     *
     * @property cause the failure cause.
     */
    data class ConnectionError(val cause: Throwable) : MqttConnectionState
}

/**
 * Incoming publish message exposed by [MqttClient].
 *
 * @property topic topic name of the publish packet.
 * @property payload payload bytes.
 * @property qos quality of service used by the sender.
 * @property retained whether the retained flag is set.
 * @property duplicate whether the duplicate delivery flag is set.
 * @property responseTopic optional response topic.
 * @property correlationData optional correlation data.
 * @property contentType optional content type.
 * @property payloadFormatIndicator optional payload format indicator.
 * @property subscriptionIdentifier optional matching subscription identifier.
 * @property userProperties user properties carried by the packet.
 */
data class MqttPublishMessage(
    val topic: Topic,
    val payload: ByteArray,
    val qos: QoS,
    val retained: Boolean,
    val duplicate: Boolean,
    val responseTopic: ResponseTopic? = null,
    val correlationData: CorrelationData? = null,
    val contentType: ContentType? = null,
    val payloadFormatIndicator: PayloadFormatIndicator? = null,
    val subscriptionIdentifier: SubscriptionIdentifier? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
) {
    /**
     * Decodes [payload] as UTF-8 text.
     */
    fun payloadAsString(): String = payload.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MqttPublishMessage) return false

        return topic == other.topic &&
            payload.contentEquals(other.payload) &&
            qos == other.qos &&
            retained == other.retained &&
            duplicate == other.duplicate &&
            responseTopic == other.responseTopic &&
            correlationData == other.correlationData &&
            contentType == other.contentType &&
            payloadFormatIndicator == other.payloadFormatIndicator &&
            subscriptionIdentifier == other.subscriptionIdentifier &&
            userProperties == other.userProperties
    }

    override fun hashCode(): Int = combinedHashCode(
        topic,
        payload.contentHashCode(),
        qos,
        retained,
        duplicate,
        responseTopic,
        correlationData,
        contentType,
        payloadFormatIndicator,
        subscriptionIdentifier,
        userProperties,
    )
}

internal fun io.github.nicolasfara.mktt.core.packet.Publish.toIncomingMessage(): MqttPublishMessage =
    MqttPublishMessage(
        topic = topic,
        payload = payload.toByteArray(0, payload.size),
        qos = qoS,
        retained = isRetainMessage,
        duplicate = isDupMessage,
        responseTopic = responseTopic,
        correlationData = correlationData,
        contentType = contentType,
        payloadFormatIndicator = payloadFormatIndicator,
        subscriptionIdentifier = subscriptionIdentifier,
        userProperties = userProperties,
    )

/**
 * Checks whether this filter matches the provided [topic].
 */
fun TopicFilter.matches(topic: Topic): Boolean {
    val rawFilter = if (filter.isShared()) filter.shareNameAndFilter().second.name else filter.name
    return topicMatchesFilter(rawFilter, topic.name)
}

private fun topicMatchesFilter(filter: String, topic: String): Boolean {
    val filterLevels = filter.split('/')
    val topicLevels = topic.split('/')

    var filterIndex = 0
    var topicIndex = 0

    var matches = true
    while (matches && filterIndex < filterLevels.size && topicIndex < topicLevels.size) {
        when (val level = filterLevels[filterIndex]) {
            "#" -> {
                matches = filterIndex == filterLevels.lastIndex
                filterIndex = filterLevels.size
                topicIndex = topicLevels.size
            }

            "+" -> {
                filterIndex += 1
                topicIndex += 1
            }

            else -> {
                if (level != topicLevels[topicIndex]) {
                    matches = false
                }
                filterIndex += 1
                topicIndex += 1
            }
        }
    }

    return matches && (
        (filterIndex == filterLevels.size && topicIndex == topicLevels.size) ||
            (filterIndex == filterLevels.lastIndex && filterLevels[filterIndex] == "#")
        )
}
