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

public data class WillProperties(
    public val willDelayInterval: io.github.nicolasfara.mktt.core.WillDelayInterval,
    public val payloadFormatIndicator: io.github.nicolasfara.mktt.core.PayloadFormatIndicator?,
    public val messageExpiryInterval: io.github.nicolasfara.mktt.core.MessageExpiryInterval?,
    public val contentType: io.github.nicolasfara.mktt.core.ContentType?,
    public val responseTopic: io.github.nicolasfara.mktt.core.ResponseTopic?,
    public val correlationData: io.github.nicolasfara.mktt.core.CorrelationData?,
    public val userProperties: io.github.nicolasfara.mktt.core.UserProperties,
) {
    internal companion object {

        internal fun from(
            properties: List<io.github.nicolasfara.mktt.core.Property<*>>,
        ): io.github.nicolasfara.mktt.core.WillProperties =
            _root_ide_package_.io.github.nicolasfara.mktt.core.WillProperties(
                willDelayInterval = properties.single<io.github.nicolasfara.mktt.core.WillDelayInterval>(),
                payloadFormatIndicator = properties.singleOrNull<io.github.nicolasfara.mktt.core.PayloadFormatIndicator>(),
                messageExpiryInterval = properties.singleOrNull<io.github.nicolasfara.mktt.core.MessageExpiryInterval>(),
                contentType = properties.singleOrNull<io.github.nicolasfara.mktt.core.ContentType>(),
                responseTopic = properties.singleOrNull<io.github.nicolasfara.mktt.core.ResponseTopic>(),
                correlationData = properties.singleOrNull<io.github.nicolasfara.mktt.core.CorrelationData>(),
                userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(
                    properties,
                ),
            )
    }
}

public fun buildWillProperties(
    init: io.github.nicolasfara.mktt.core.WillPropertiesBuilder.() -> Unit,
): io.github.nicolasfara.mktt.core.WillProperties {
    val builder = _root_ide_package_.io.github.nicolasfara.mktt.core.WillPropertiesBuilder()
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
@io.github.nicolasfara.mktt.core.util.MqttDslMarker
public class WillPropertiesBuilder {
    public var willDelayInterval: Duration = ZERO
    public var payloadFormatIndicator: io.github.nicolasfara.mktt.core.PayloadFormatIndicator? = null
    public var messageExpiryInterval: Duration? = null
    public var contentType: String? = null
    public var responseTopic: String? = null
    public var correlationData: ByteString? = null

    private val userPropertiesBuilder = _root_ide_package_.io.github.nicolasfara.mktt.core.UserPropertiesBuilder()

    /**
     * Creates the user properties of this will properties.
     */
    public fun userProperties(init: io.github.nicolasfara.mktt.core.UserPropertiesBuilder.() -> Unit) {
        userPropertiesBuilder.init()
    }

    public fun build(): io.github.nicolasfara.mktt.core.WillProperties =
        _root_ide_package_.io.github.nicolasfara.mktt.core.WillProperties(
            willDelayInterval = willDelayInterval.toWillDelayInterval(),
            payloadFormatIndicator = payloadFormatIndicator,
            messageExpiryInterval = messageExpiryInterval?.toMessageExpiryInterval(),
            contentType = contentType?.let { _root_ide_package_.io.github.nicolasfara.mktt.core.ContentType(it) },
            responseTopic = responseTopic?.let { _root_ide_package_.io.github.nicolasfara.mktt.core.ResponseTopic(it) },
            correlationData = correlationData?.let {
                _root_ide_package_.io.github.nicolasfara.mktt.core.CorrelationData(
                    it,
                )
            },
            userProperties = userPropertiesBuilder.build(),
        )
}

internal fun io.github.nicolasfara.mktt.core.WillProperties.asArray(): Array<io.github.nicolasfara.mktt.core.Property<*>?> =
    arrayOf(
        willDelayInterval,
        payloadFormatIndicator,
        messageExpiryInterval,
        contentType,
        responseTopic,
        correlationData,
        *userProperties.asArray,
    )
