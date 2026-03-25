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

data class PublishRequest(
    val topic: Topic,
    val desiredQoS: QoS = QoS.AT_MOST_ONCE,
    val payload: ByteArray = byteArrayOf(),
    val isRetainMessage: Boolean = false,
    val messageExpiryInterval: MessageExpiryInterval? = null,
    val topicAlias: TopicAlias? = null,
    val responseTopic: ResponseTopic? = null,
    val correlationData: CorrelationData? = null,
    val contentType: ContentType? = null,
    val payloadFormatIndicator: PayloadFormatIndicator? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
) {
    init {
        require(!topic.containsWildcard()) {
            "Topic Name in PUBLISH packet contains wildcard characters [MQTT-3.3.2-2]: '$topic'"
        }
    }

    internal fun payloadAsByteString(): ByteString = ByteString(payload)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PublishRequest) return false

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

fun PublishRequest(
    topicName: String,
    topicAlias: UShort? = null,
    init: PublishRequestBuilder.() -> Unit,
): PublishRequest = PublishRequestBuilder(
    topicName.toTopic(),
    topicAlias,
).apply(init).build()

@MqttDslMarker
class PublishRequestBuilder(private val topic: Topic, private val topicAlias: UShort? = null) {
    var desiredQoS: QoS = QoS.AT_MOST_ONCE
    var isRetainMessage: Boolean = false
    var messageExpiryInterval: Duration? = 5.minutes
    var responseTopic: String? = null
    var correlationData: ByteArray? = null
    var contentType: String? = null
    var payloadFormatIndicator: PayloadFormatIndicator? = null

    private var payload: ByteArray = byteArrayOf()
    private var userProperties: UserProperties = UserProperties.EMPTY

    fun payload(text: String) {
        val byteString = text.encodeToByteString()
        payload = byteString.toByteArray(0, byteString.size)
        payloadFormatIndicator =
            PayloadFormatIndicator.UTF_8
    }

    fun payload(bytes: ByteArray) {
        payload = bytes.copyOf()
    }

    fun userProperties(init: UserPropertiesBuilder.() -> Unit) {
        userProperties = UserPropertiesBuilder().apply(init).build()
    }

    fun build(): PublishRequest = PublishRequest(
        topic = topic,
        desiredQoS = desiredQoS,
        payload = payload,
        isRetainMessage = isRetainMessage,
        messageExpiryInterval = messageExpiryInterval?.toMessageExpiryInterval(),
        topicAlias = topicAlias?.let(::TopicAlias),
        responseTopic = responseTopic?.let(::ResponseTopic),
        correlationData = correlationData?.let {
            CorrelationData(
                ByteString(it),
            )
        },
        contentType = contentType?.let(::ContentType),
        payloadFormatIndicator = payloadFormatIndicator,
        userProperties = userProperties,
    )
}
