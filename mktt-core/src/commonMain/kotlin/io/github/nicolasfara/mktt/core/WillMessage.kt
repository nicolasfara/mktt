package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.util.*
import io.github.nicolasfara.mktt.core.util.readMqttByteString
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttByteString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

public data class WillMessage(
    public val topic: io.github.nicolasfara.mktt.core.Topic,
    public val payload: ByteString,
    public val properties: io.github.nicolasfara.mktt.core.WillProperties,
)

public fun buildWillMessage(
    topic: String,
    init: io.github.nicolasfara.mktt.core.WillMessageBuilder.() -> Unit,
): io.github.nicolasfara.mktt.core.WillMessage {
    val builder = _root_ide_package_.io.github.nicolasfara.mktt.core.WillMessageBuilder(topic)
    builder.init()
    return builder.build()
}

/**
 * Will message builder
 *
 * @property willOqS the QoS level to be used when publishing the Will Message
 * @property retainWillMessage specifies if the Will Message is to be retained when it is published
 */
@io.github.nicolasfara.mktt.core.util.MqttDslMarker
public class WillMessageBuilder(private val topic: String) {

    private val propertiesBuilder = _root_ide_package_.io.github.nicolasfara.mktt.core.WillPropertiesBuilder()

    private var payload: ByteString =
        _root_ide_package_.io.github.nicolasfara.mktt.core.WillMessageBuilder.Companion.EMPTY_PAYLOAD

    public var willOqS: io.github.nicolasfara.mktt.core.QoS = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE

    public var retainWillMessage: Boolean = false

    /**
     * Configures the will properties.
     */
    public fun properties(init: io.github.nicolasfara.mktt.core.WillPropertiesBuilder.() -> Unit) {
        propertiesBuilder.init()
    }

    /**
     * Convenience method to define a text as payload, also sets the [io.github.nicolasfara.mktt.core.PayloadFormatIndicator] of the will properties
     * to `UTF_8`.
     */
    public fun payload(text: String) {
        this.payload = text.encodeToByteString()
        this.propertiesBuilder.payloadFormatIndicator =
            _root_ide_package_.io.github.nicolasfara.mktt.core.PayloadFormatIndicator.Companion.UTF_8
    }

    /**
     * Defines the payload (without setting the payload format indicator of the will properties).
     *
     * @see payload
     */
    public fun payload(byteString: ByteString) {
        this.payload = byteString
    }

    public fun build(): io.github.nicolasfara.mktt.core.WillMessage =
        _root_ide_package_.io.github.nicolasfara.mktt.core.WillMessage(
            _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(topic),
            payload,
            propertiesBuilder.build(),
        )

    private companion object {

        val EMPTY_PAYLOAD = ByteString(ByteArray(0))
    }
}

internal fun Sink.write(willMessage: io.github.nicolasfara.mktt.core.WillMessage) {
    writeProperties(*willMessage.properties.asArray())
    writeMqttString(willMessage.topic.name)
    writeMqttByteString(willMessage.payload)
}

internal fun Source.readWillMessage(): io.github.nicolasfara.mktt.core.WillMessage {
    val properties = readProperties()
    val topic = readMqttString()
    val payload = readMqttByteString()

    return _root_ide_package_.io.github.nicolasfara.mktt.core.WillMessage(
        _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(
            topic,
        ),
        payload,
        _root_ide_package_.io.github.nicolasfara.mktt.core.WillProperties.Companion.from(properties),
    )
}
