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

public typealias ConnAck = io.github.nicolasfara.mktt.core.packet.Connack
public typealias SubAck = io.github.nicolasfara.mktt.core.packet.Suback
public typealias UnsubAck = io.github.nicolasfara.mktt.core.packet.Unsuback
public typealias DisconnectReason = io.github.nicolasfara.mktt.core.ReasonCode
public typealias Subscription = io.github.nicolasfara.mktt.core.TopicFilter
public typealias PublishResult = io.github.nicolasfara.mktt.client.PublishResponse

public sealed interface MqttConnectionState {
    public data object Connecting : io.github.nicolasfara.mktt.client.MqttConnectionState

    public data class Connected(public val connack: io.github.nicolasfara.mktt.client.ConnAck) :
        io.github.nicolasfara.mktt.client.MqttConnectionState

    public data object Disconnected : io.github.nicolasfara.mktt.client.MqttConnectionState

    public data class ConnectionError(public val cause: Throwable) :
        io.github.nicolasfara.mktt.client.MqttConnectionState
}

public data class MqttPublishMessage(
    public val topic: io.github.nicolasfara.mktt.core.Topic,
    public val payload: ByteArray,
    public val qos: io.github.nicolasfara.mktt.core.QoS,
    public val retained: Boolean,
    public val duplicate: Boolean,
    public val responseTopic: io.github.nicolasfara.mktt.core.ResponseTopic? = null,
    public val correlationData: io.github.nicolasfara.mktt.core.CorrelationData? = null,
    public val contentType: io.github.nicolasfara.mktt.core.ContentType? = null,
    public val payloadFormatIndicator: io.github.nicolasfara.mktt.core.PayloadFormatIndicator? = null,
    public val subscriptionIdentifier: io.github.nicolasfara.mktt.core.SubscriptionIdentifier? = null,
    public val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) {
    public fun payloadAsString(): String = payload.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is io.github.nicolasfara.mktt.client.MqttPublishMessage) return false

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

internal fun io.github.nicolasfara.mktt.core.packet.Publish.toIncomingMessage(): io.github.nicolasfara.mktt.client.MqttPublishMessage =
    _root_ide_package_.io.github.nicolasfara.mktt.client.MqttPublishMessage(
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

public fun io.github.nicolasfara.mktt.core.TopicFilter.matches(topic: io.github.nicolasfara.mktt.core.Topic): Boolean {
    val rawFilter = if (filter.isShared()) filter.shareNameAndFilter().second.name else filter.name
    return _root_ide_package_.io.github.nicolasfara.mktt.client.topicMatchesFilter(rawFilter, topic.name)
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
