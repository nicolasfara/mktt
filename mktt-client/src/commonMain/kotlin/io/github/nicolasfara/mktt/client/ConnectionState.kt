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

sealed interface MqttConnectionState {
    data object Connecting : MqttConnectionState

    data class Connected(val connack: ConnAck) : MqttConnectionState

    data object Disconnected : MqttConnectionState

    data class ConnectionError(val cause: Throwable) : MqttConnectionState
}

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

    override fun hashCode(): Int {
        var result = topic.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + qos.hashCode()
        result = 31 * result + retained.hashCode()
        result = 31 * result + duplicate.hashCode()
        result = 31 * result + (responseTopic?.hashCode() ?: 0)
        result = 31 * result + (correlationData?.hashCode() ?: 0)
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + (payloadFormatIndicator?.hashCode() ?: 0)
        result = 31 * result + (subscriptionIdentifier?.hashCode() ?: 0)
        result = 31 * result + userProperties.hashCode()
        return result
    }
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

fun TopicFilter.matches(topic: Topic): Boolean {
    val rawFilter = if (filter.isShared()) filter.shareNameAndFilter().second.name else filter.name
    return topicMatchesFilter(rawFilter, topic.name)
}

private fun topicMatchesFilter(filter: String, topic: String): Boolean {
    val filterLevels = filter.split('/')
    val topicLevels = topic.split('/')

    var filterIndex = 0
    var topicIndex = 0

    while (filterIndex < filterLevels.size && topicIndex < topicLevels.size) {
        when (val level = filterLevels[filterIndex]) {
            "#" -> return filterIndex == filterLevels.lastIndex

            "+" -> {
                filterIndex += 1
                topicIndex += 1
            }

            else -> {
                if (level != topicLevels[topicIndex]) {
                    return false
                }
                filterIndex += 1
                topicIndex += 1
            }
        }
    }

    return when {
        filterIndex == filterLevels.size && topicIndex == topicLevels.size -> true
        filterIndex == filterLevels.lastIndex && filterLevels[filterIndex] == "#" -> true
        else -> false
    }
}
