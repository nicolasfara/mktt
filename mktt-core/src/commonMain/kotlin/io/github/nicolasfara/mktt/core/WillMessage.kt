package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.util.MqttDslMarker
import io.github.nicolasfara.mktt.core.util.readMqttByteString
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttByteString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

/**
 * Last-will message configuration sent in CONNECT packets.
 *
 * @property topic topic on which the will message will be published.
 * @property payload binary payload of the will message.
 * @property properties MQTT will properties.
 */
data class WillMessage(val topic: Topic, val payload: ByteString, val properties: WillProperties)

/**
 * Builds a [WillMessage] for the provided [topic].
 */
fun buildWillMessage(topic: String, init: WillMessageBuilder.() -> Unit): WillMessage {
    val builder = WillMessageBuilder(topic)
    builder.init()
    return builder.build()
}

/**
 * Will message builder.
 *
 * @property willOqS the QoS level to be used when publishing the will message.
 * @property retainWillMessage specifies whether the will message is retained when published.
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
     * Convenience method to define a text as payload.
     *
     * Also sets the
     * [io.github.nicolasfara.mktt.core.PayloadFormatIndicator] of the will properties
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

    /** Builds the configured [WillMessage]. */
    fun build(): WillMessage = WillMessage(
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
