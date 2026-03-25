package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.single
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.toMessageExpiryInterval
import io.github.nicolasfara.mktt.core.toWillDelayInterval
import io.github.nicolasfara.mktt.core.util.MqttDslMarker
import kotlinx.io.bytestring.ByteString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

data class WillProperties(
    val willDelayInterval: WillDelayInterval,
    val payloadFormatIndicator: PayloadFormatIndicator?,
    val messageExpiryInterval: MessageExpiryInterval?,
    val contentType: ContentType?,
    val responseTopic: ResponseTopic?,
    val correlationData: CorrelationData?,
    val userProperties: UserProperties,
) {
    internal companion object {
        internal fun from(properties: List<Property<*>>): WillProperties =
            WillProperties(
                willDelayInterval = properties.single<WillDelayInterval>(),
                payloadFormatIndicator = properties.singleOrNull<PayloadFormatIndicator>(),
                messageExpiryInterval = properties.singleOrNull<MessageExpiryInterval>(),
                contentType = properties.singleOrNull<ContentType>(),
                responseTopic = properties.singleOrNull<ResponseTopic>(),
                correlationData = properties.singleOrNull<CorrelationData>(),
                userProperties = UserProperties.from(properties),
            )
    }
}

fun buildWillProperties(init: WillPropertiesBuilder.() -> Unit): WillProperties {
    val builder = WillPropertiesBuilder()
    builder.init()
    return builder.build()
}

/**
 * DSL for building will properties.
 *
 * @property willDelayInterval the Server delays publishing the Client’s Will Message until the Will Delay Interval has
 *           passed or the Session ends, whichever happens first; defaults to 0
 * @property payloadFormatIndicator specifies the format of the will message payload
 * @property messageExpiryInterval the lifetime of the will message and is sent as the publication expiry interval when
 *           the Server publishes the Will Message
 * @property contentType string describing the content of the Will Message
 * @property responseTopic topic name for a response message
 * @property correlationData used by the sender of the Request Message to identify which request the Response Message is
 *           for when it is received
 */
@MqttDslMarker
class WillPropertiesBuilder {
    var willDelayInterval: Duration = ZERO
    var payloadFormatIndicator: PayloadFormatIndicator? = null
    var messageExpiryInterval: Duration? = null
    var contentType: String? = null
    var responseTopic: String? = null
    var correlationData: ByteString? = null

    private val userPropertiesBuilder = UserPropertiesBuilder()

    /**
     * Creates the user properties of this will properties.
     */
    fun userProperties(init: UserPropertiesBuilder.() -> Unit) {
        userPropertiesBuilder.init()
    }

    fun build(): WillProperties =
        WillProperties(
            willDelayInterval = willDelayInterval.toWillDelayInterval(),
            payloadFormatIndicator = payloadFormatIndicator,
            messageExpiryInterval = messageExpiryInterval?.toMessageExpiryInterval(),
            contentType = contentType?.let { ContentType(it) },
            responseTopic = responseTopic?.let { ResponseTopic(it) },
            correlationData = correlationData?.let {
                CorrelationData(
                    it,
                )
            },
            userProperties = userPropertiesBuilder.build(),
        )
}

internal fun WillProperties.asArray(): Array<Property<*>?> =
    arrayOf(
        willDelayInterval,
        payloadFormatIndicator,
        messageExpiryInterval,
        contentType,
        responseTopic,
        correlationData,
        *userProperties.asArray,
    )
