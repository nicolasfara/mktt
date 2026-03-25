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

data class WillMessage(
    val topic: Topic,
    val payload: ByteString,
    val properties: WillProperties,
)

fun buildWillMessage(topic: String, init: WillMessageBuilder.() -> Unit): WillMessage {
    val builder = WillMessageBuilder(topic)
    builder.init()
    return builder.build()
}

/**
 * Will message builder
 *
 * @property willOqS the QoS level to be used when publishing the Will Message
 * @property retainWillMessage specifies if the Will Message is to be retained when it is published
 */
@MqttDslMarker
class WillMessageBuilder(private val topic: String) {
    private val propertiesBuilder = WillPropertiesBuilder()
    private var payload: ByteString = EMPTY_PAYLOAD
    var willOqS: QoS = QoS.AT_MOST_ONCE
    var retainWillMessage: Boolean = false

    /**
     * Configures the will properties.
     */
    fun properties(init: WillPropertiesBuilder.() -> Unit) {
        propertiesBuilder.init()
    }

    /**
     * Convenience method to define a text as payload, also sets the [io.github.nicolasfara.mktt.core.PayloadFormatIndicator] of the will properties
     * to `UTF_8`.
     */
    fun payload(text: String) {
        this.payload = text.encodeToByteString()
        this.propertiesBuilder.payloadFormatIndicator =
            PayloadFormatIndicator.UTF_8
    }

    /**
     * Defines the payload (without setting the payload format indicator of the will properties).
     *
     * @see payload
     */
    fun payload(byteString: ByteString) {
        this.payload = byteString
    }

    fun build(): WillMessage =
        WillMessage(
            Topic(topic),
            payload,
            propertiesBuilder.build(),
        )

    private companion object {
        val EMPTY_PAYLOAD = ByteString(ByteArray(0))
    }
}

internal fun Sink.write(willMessage: WillMessage) {
    writeProperties(*willMessage.properties.asArray())
    writeMqttString(willMessage.topic.name)
    writeMqttByteString(willMessage.payload)
}

internal fun Source.readWillMessage(): WillMessage {
    val properties = readProperties()
    val topic = readMqttString()
    val payload = readMqttByteString()
    return WillMessage(
        Topic(
            topic,
        ),
        payload,
        WillProperties.from(properties),
    )
}
