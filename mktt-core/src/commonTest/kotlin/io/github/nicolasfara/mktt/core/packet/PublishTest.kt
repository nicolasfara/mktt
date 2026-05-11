package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.CorrelationData
import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.github.nicolasfara.mktt.core.MessageExpiryInterval
import io.github.nicolasfara.mktt.core.PayloadFormatIndicator
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.SubscriptionIdentifier
import io.github.nicolasfara.mktt.core.TopicAlias
import io.github.nicolasfara.mktt.core.buildUserProperties
import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.readVariableByteInt
import io.github.nicolasfara.mktt.core.util.toResponseTopic
import io.github.nicolasfara.mktt.core.util.toTopic
import io.ktor.utils.io.core.toByteArray
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.bytestring.ByteString
import kotlinx.io.bytestring.encodeToByteString

class PublishTest {

    @Test
    fun `encode and decode returns same packet`() = runTest {
        assertEncodeDecodeOf(Publish(topic = "test/topic".toTopic(), payload = "123".encodeToByteString()))
        assertEncodeDecodeOf(
            Publish(
                isDupMessage = true,
                qoS = QoS.EXACTLY_ONE,
                isRetainMessage = true,
                packetIdentifier = 74u,
                topic = "test/topic".toTopic(),
                payloadFormatIndicator = PayloadFormatIndicator.UTF_8,
                messageExpiryInterval = MessageExpiryInterval(60u),
                topicAlias = TopicAlias(3u),
                responseTopic = "response".toResponseTopic(),
                correlationData = CorrelationData("123".encodeToByteString()),
                userProperties = buildUserProperties { "user" to "value" },
                subscriptionIdentifier = SubscriptionIdentifier(5000),
                payload = ByteString("payload".toByteArray()),
            ),
        )
    }

    @Test
    fun `correlation data contributes its length prefix to properties length`() {
        val reader = Buffer().apply {
            write(
                Publish(
                    topic = "test/topic".toTopic(),
                    correlationData = CorrelationData("data".encodeToByteString()),
                    payload = ByteString(ByteArray(0)),
                ),
            )
        }

        assertEquals("test/topic", reader.readMqttString())
        assertEquals(7, reader.readVariableByteInt())
    }

    @Test
    fun `packet identifiers are not null when required by QoS`() {
        val topic = "abc/def".toTopic()
        val payload = "123".encodeToByteString()

        // Must NOT throw an exception
        Publish(qoS = QoS.AT_MOST_ONCE, packetIdentifier = null, topic = topic, payload = payload)
        Publish(qoS = QoS.AT_LEAST_ONCE, packetIdentifier = 1u, topic = topic, payload = payload)
        Publish(qoS = QoS.EXACTLY_ONE, packetIdentifier = 1u, topic = topic, payload = payload)

        assertFailsWith<MalformedPacketException> {
            Publish(qoS = QoS.AT_MOST_ONCE, packetIdentifier = 1u, topic = topic, payload = payload)
        }
        assertFailsWith<MalformedPacketException> {
            Publish(qoS = QoS.AT_LEAST_ONCE, packetIdentifier = null, topic = topic, payload = payload)
        }
        assertFailsWith<MalformedPacketException> {
            Publish(qoS = QoS.EXACTLY_ONE, packetIdentifier = null, topic = topic, payload = payload)
        }
    }

    @Test
    fun `either topic or topic alias must be set`() {
        assertFailsWith<MalformedPacketException> {
            Publish(
                qoS = QoS.AT_MOST_ONCE,
                topic = "".toTopic(),
                topicAlias = null,
                payload = "123".encodeToByteString(),
            )
        }
    }

    @Test
    fun `a topic alias of zero is not allowed`() {
        val payload = "123".encodeToByteString()
        assertFailsWith<MalformedPacketException> {
            Publish(qoS = QoS.AT_MOST_ONCE, topic = "abc".toTopic(), topicAlias = TopicAlias(0u), payload = payload)
        }
    }
}
