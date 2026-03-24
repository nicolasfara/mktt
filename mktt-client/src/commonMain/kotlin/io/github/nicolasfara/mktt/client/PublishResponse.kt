package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.*
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
public sealed class PublishResponse {

    /**
     * The packet which was successfully delivered to the server.
     */
    public abstract val source: io.github.nicolasfara.mktt.core.packet.Publish

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
    public abstract val reason: io.github.nicolasfara.mktt.core.ReasonCode
}

/**
 * Returns the quality of service that was used for sending the PUBLISH request.
 */
public val io.github.nicolasfara.mktt.client.PublishResponse.qoS: io.github.nicolasfara.mktt.core.QoS
    get() = source.qoS

/**
 * The publish response for [io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE].
 */
public data class AtMostOncePublishResponse(
    public override val source: io.github.nicolasfara.mktt.core.packet.Publish,
) : io.github.nicolasfara.mktt.client.PublishResponse() {

    override val reason: io.github.nicolasfara.mktt.core.ReasonCode =
        _root_ide_package_.io.github.nicolasfara.mktt.core.Success
}

/**
 * The publish response for [io.github.nicolasfara.mktt.core.QoS.AT_LEAST_ONCE].
 */
public data class AtLeastOncePublishResponse(
    public override val source: io.github.nicolasfara.mktt.core.packet.Publish,
    public val puback: io.github.nicolasfara.mktt.core.packet.Puback,
) : io.github.nicolasfara.mktt.client.PublishResponse() {

    override val reason: io.github.nicolasfara.mktt.core.ReasonCode = puback.reason
}

/**
 * The publish response for [io.github.nicolasfara.mktt.core.QoS.EXACTLY_ONE].
 */
public data class ExactlyOnePublishResponse(
    public override val source: io.github.nicolasfara.mktt.core.packet.Publish,
    public val pubcomp: io.github.nicolasfara.mktt.core.packet.Pubcomp,
) : io.github.nicolasfara.mktt.client.PublishResponse() {

    override val reason: io.github.nicolasfara.mktt.core.ReasonCode = pubcomp.reason
}
