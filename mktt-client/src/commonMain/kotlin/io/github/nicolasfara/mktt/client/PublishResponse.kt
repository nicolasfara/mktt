package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.packet.Puback
import io.github.nicolasfara.mktt.core.packet.Pubcomp
import io.github.nicolasfara.mktt.core.packet.Publish

/**
 * Return value of a successful PUBLISH request.
 *
 * @see io.github.nicolasfara.mktt.client.MqttClient.publish
 * @see io.github.nicolasfara.mktt.client.AtMostOncePublishResponse
 * @see io.github.nicolasfara.mktt.client.AtLeastOncePublishResponse
 * @see io.github.nicolasfara.mktt.client.ExactlyOnePublishResponse
 */
sealed class PublishResponse {

    /**
     * The packet which was successfully delivered to the server.
     */
    abstract val source: Publish

    /**
     * The reason code of the final packet that was returned by the server. Its source depends on the quality of service
     * which was actually used for publishing:
     *
     * - for AT_MOST_ONCE this will always be [io.github.nicolasfara.mktt.core.Success]
     * - for AT_LEAST_ONCE this will be taken from the [io.github.nicolasfara.mktt.core.packet.Puback] packet
     * - for EXACTLY_ONE this will be taken from the [io.github.nicolasfara.mktt.core.packet.Pubcomp] packet
     *
     * Note that even for a successful delivery to the server, the reason code might not always be zero (success). For
     * example, when publishing with [io.github.nicolasfara.mktt.core.QoS.AT_LEAST_ONCE] to a topic without subscribers, the returned reason might be
     * [io.github.nicolasfara.mktt.core.NoMatchingSubscribers].
     */
    abstract val reason: ReasonCode
}

/**
 * Returns the quality of service that was used for sending the PUBLISH request.
 */
val PublishResponse.qoS: QoS
    get() = source.qoS

/**
 * The publish response for [io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE].
 */
data class AtMostOncePublishResponse(override val source: Publish) : PublishResponse() {

    override val reason: ReasonCode =
        Success
}

/**
 * The publish response for [io.github.nicolasfara.mktt.core.QoS.AT_LEAST_ONCE].
 */
data class AtLeastOncePublishResponse(override val source: Publish, val puback: Puback) : PublishResponse() {

    override val reason: ReasonCode = puback.reason
}

/**
 * The publish response for [io.github.nicolasfara.mktt.core.QoS.EXACTLY_ONE].
 */
data class ExactlyOnePublishResponse(override val source: Publish, val pubcomp: Pubcomp) : PublishResponse() {

    override val reason: ReasonCode = pubcomp.reason
}
