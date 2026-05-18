@file:OptIn(ExperimentalTime::class)

package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.packet.Publish
import io.github.nicolasfara.mktt.core.packet.Pubrel
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a packet which is currently not completely acknowledged by the server.
 *
 * @property timestamp the time when this packet was created
 * @property key an integer value used for sorting instances of this
 */
sealed class InFlightPacket(val timestamp: Instant, val key: Long) : Comparable<InFlightPacket> {

    /**
     * The packet identifier of the underlying packet.
     */
    abstract val packetIdentifier: UShort

    /**
     * Determines whether this in-flight packet is expired due to its message expiry interval.
     */
    abstract fun isExpired(now: Instant): Boolean

    override fun compareTo(other: InFlightPacket): Int = this.key.compareTo(other.key)
}

/**
 * In-flight QoS 1/2 publish packet awaiting handshake completion.
 *
 * @property source publish packet that is currently in-flight.
 */
class InFlightPublish(val source: Publish, timestamp: Instant, id: Long) : InFlightPacket(timestamp, id) {

    init {
        require(source.qoS != QoS.AT_MOST_ONCE) {
            "PUBLISH packets with QoS 0 cannot be part of a transaction: $source"
        }
    }

    override val packetIdentifier: UShort
        get() = checkNotNull(source.packetIdentifier) {
            "In-flight publish requires a packet identifier: $source"
        }

    override fun isExpired(now: Instant): Boolean =
        source.messageExpiryInterval != null && timestamp + source.messageExpiryInterval.toDuration() < now
}

/**
 * In-flight PUBREL packet awaiting PUBCOMP completion.
 *
 * @property source PUBREL packet that is currently in-flight.
 */
class InFlightPubrel(val source: Pubrel, timestamp: Instant, id: Long) : InFlightPacket(timestamp, id) {

    constructor(inFlightPublish: InFlightPublish, id: Long) :
        this(
            Pubrel.from(inFlightPublish.source),
            inFlightPublish.timestamp,
            id,
        )

    override val packetIdentifier: UShort
        get() = source.packetIdentifier

    override fun isExpired(now: Instant): Boolean = false
}
