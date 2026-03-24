package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.ContentType
import io.github.nicolasfara.mktt.core.CorrelationData
import io.github.nicolasfara.mktt.core.MessageExpiryInterval
import io.github.nicolasfara.mktt.core.PayloadFormatIndicator
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.ResponseTopic
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicAlias
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.UserPropertiesBuilder
import io.github.nicolasfara.mktt.core.toMessageExpiryInterval
import io.github.nicolasfara.mktt.core.util.MqttDslMarker
import io.github.nicolasfara.mktt.core.util.toTopic
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

public data class PublishRequest(
    val topic: io.github.nicolasfara.mktt.core.Topic,
    val desiredQoS: io.github.nicolasfara.mktt.core.QoS = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE,
    val payload: ByteArray = byteArrayOf(),
    val isRetainMessage: Boolean = false,
    val messageExpiryInterval: io.github.nicolasfara.mktt.core.MessageExpiryInterval? = null,
    val topicAlias: io.github.nicolasfara.mktt.core.TopicAlias? = null,
    val responseTopic: io.github.nicolasfara.mktt.core.ResponseTopic? = null,
    val correlationData: io.github.nicolasfara.mktt.core.CorrelationData? = null,
    val contentType: io.github.nicolasfara.mktt.core.ContentType? = null,
    val payloadFormatIndicator: io.github.nicolasfara.mktt.core.PayloadFormatIndicator? = null,
    val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) {
    init {
        require(!topic.containsWildcard()) {
            "Topic Name in PUBLISH packet contains wildcard characters [MQTT-3.3.2-2]: '$topic'"
        }
    }

    internal fun payloadAsByteString(): ByteString = ByteString(payload)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is io.github.nicolasfara.mktt.client.PublishRequest) return false

        return topic == other.topic &&
            desiredQoS == other.desiredQoS &&
            payload.contentEquals(other.payload) &&
            isRetainMessage == other.isRetainMessage &&
            messageExpiryInterval == other.messageExpiryInterval &&
            topicAlias == other.topicAlias &&
            responseTopic == other.responseTopic &&
            correlationData == other.correlationData &&
            contentType == other.contentType &&
            payloadFormatIndicator == other.payloadFormatIndicator &&
            userProperties == other.userProperties
    }

    override fun hashCode(): Int {
        var result = topic.hashCode()
        result = 31 * result + desiredQoS.hashCode()
        result = 31 * result + payload.contentHashCode()
        result = 31 * result + isRetainMessage.hashCode()
        result = 31 * result + (messageExpiryInterval?.hashCode() ?: 0)
        result = 31 * result + (topicAlias?.hashCode() ?: 0)
        result = 31 * result + (responseTopic?.hashCode() ?: 0)
        result = 31 * result + (correlationData?.hashCode() ?: 0)
        result = 31 * result + (contentType?.hashCode() ?: 0)
        result = 31 * result + (payloadFormatIndicator?.hashCode() ?: 0)
        result = 31 * result + userProperties.hashCode()
        return result
    }
}

public fun PublishRequest(
    topicName: String,
    topicAlias: UShort? = null,
    init: io.github.nicolasfara.mktt.client.PublishRequestBuilder.() -> Unit,
): io.github.nicolasfara.mktt.client.PublishRequest =
    _root_ide_package_.io.github.nicolasfara.mktt.client.PublishRequestBuilder(
        topicName.toTopic(),
        topicAlias,
    ).apply(init).build()

@io.github.nicolasfara.mktt.core.util.MqttDslMarker
public class PublishRequestBuilder(
    private val topic: io.github.nicolasfara.mktt.core.Topic,
    private val topicAlias: UShort? = null,
) {
    public var desiredQoS: io.github.nicolasfara.mktt.core.QoS = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE
    public var isRetainMessage: Boolean = false
    public var messageExpiryInterval: Duration? = 5.minutes
    public var responseTopic: String? = null
    public var correlationData: ByteArray? = null
    public var contentType: String? = null
    public var payloadFormatIndicator: io.github.nicolasfara.mktt.core.PayloadFormatIndicator? = null

    private var payload: ByteArray = byteArrayOf()
    private var userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY

    public fun payload(text: String) {
        val byteString = text.encodeToByteString()
        payload = byteString.toByteArray(0, byteString.size)
        payloadFormatIndicator =
            _root_ide_package_.io.github.nicolasfara.mktt.core.PayloadFormatIndicator.Companion.UTF_8
    }

    public fun payload(bytes: ByteArray) {
        payload = bytes.copyOf()
    }

    public fun userProperties(init: io.github.nicolasfara.mktt.core.UserPropertiesBuilder.() -> Unit) {
        userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserPropertiesBuilder().apply(init).build()
    }

    public fun build(): io.github.nicolasfara.mktt.client.PublishRequest =
        _root_ide_package_.io.github.nicolasfara.mktt.client.PublishRequest(
            topic = topic,
            desiredQoS = desiredQoS,
            payload = payload,
            isRetainMessage = isRetainMessage,
            messageExpiryInterval = messageExpiryInterval?.toMessageExpiryInterval(),
            topicAlias = topicAlias?.let(::TopicAlias),
            responseTopic = responseTopic?.let(::ResponseTopic),
            correlationData = correlationData?.let {
                _root_ide_package_.io.github.nicolasfara.mktt.core.CorrelationData(
                    ByteString(it),
                )
            },
            contentType = contentType?.let(::ContentType),
            payloadFormatIndicator = payloadFormatIndicator,
            userProperties = userProperties,
        )
}
