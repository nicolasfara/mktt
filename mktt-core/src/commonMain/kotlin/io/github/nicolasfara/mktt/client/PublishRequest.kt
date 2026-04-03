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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

/**
 * Immutable request used to publish a message.
 *
 * @property topic destination topic.
 * @property desiredQoS requested quality of service.
 * @property payload payload bytes.
 * @property isRetainMessage whether the retain flag should be set.
 * @property messageExpiryInterval optional expiry interval.
 * @property topicAlias optional topic alias.
 * @property responseTopic optional response topic.
 * @property correlationData optional correlation data.
 * @property contentType optional payload content type.
 * @property payloadFormatIndicator optional payload format hint.
 * @property userProperties user properties to include in PUBLISH.
 */
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

    /**
     * Returns [payload] as an immutable [ByteString].
     */
    fun payloadAsByteString(): ByteString = ByteString(payload)

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

    override fun hashCode(): Int = combinedHashCode(
        topic,
        desiredQoS,
        payload.contentHashCode(),
        isRetainMessage,
        messageExpiryInterval,
        topicAlias,
        responseTopic,
        correlationData,
        contentType,
        payloadFormatIndicator,
        userProperties,
    )
}

/**
 * Builds a [PublishRequest] for [topicName] using the DSL [init] block.
 */
fun PublishRequest(
    topicName: String,
    topicAlias: UShort? = null,
    init: PublishRequestBuilder.() -> Unit,
): PublishRequest = PublishRequestBuilder(
    topicName.toTopic(),
    topicAlias,
).apply(init).build()

/**
 * DSL builder for [PublishRequest].
 */
@MqttDslMarker
class PublishRequestBuilder(private val topic: Topic, private val topicAlias: UShort? = null) {
    /**
     * Requested quality of service.
     */
    var desiredQoS: QoS = QoS.AT_MOST_ONCE

    /**
     * Whether to set the retain flag.
     */
    var isRetainMessage: Boolean = false

    /**
     * Optional message expiry interval.
     */
    var messageExpiryInterval: Duration? = 5.minutes

    /**
     * Optional response topic.
     */
    var responseTopic: String? = null

    /**
     * Optional correlation data.
     */
    var correlationData: ByteArray? = null

    /**
     * Optional content type.
     */
    var contentType: String? = null

    /**
     * Optional payload format indicator.
     */
    var payloadFormatIndicator: PayloadFormatIndicator? = null

    private var payload: ByteArray = byteArrayOf()
    private var userProperties: UserProperties = UserProperties.EMPTY

    /**
     * Sets payload bytes from UTF-8 [text].
     */
    fun payload(text: String) {
        val byteString = text.encodeToByteString()
        payload = byteString.toByteArray(0, byteString.size)
        payloadFormatIndicator =
            PayloadFormatIndicator.UTF_8
    }

    /**
     * Sets payload bytes from [bytes].
     */
    fun payload(bytes: ByteArray) {
        payload = bytes.copyOf()
    }

    /**
     * Configures user properties for the publish packet.
     */
    fun userProperties(init: UserPropertiesBuilder.() -> Unit) {
        userProperties = UserPropertiesBuilder().apply(init).build()
    }

    /**
     * Builds a [PublishRequest] from the current builder state.
     */
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
