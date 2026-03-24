@file:OptIn(ExperimentalTime::class)

package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.packet.Publish
import io.github.nicolasfara.mktt.core.packet.Pubrel
import io.github.nicolasfara.mktt.core.toDuration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a packet which is currently not completely acknowledged by the server.
 *
 * @property timestamp the time when this packet was created
 * @property key an integer value used for sorting instances of this
 */
public sealed class InFlightPacket(public val timestamp: Instant, public val key: Long) :
    Comparable<io.github.nicolasfara.mktt.core.InFlightPacket> {

    /**
     * The packet identifier of the underlying packet.
     */
    public abstract val packetIdentifier: UShort

    /**
     * Determines whether this in-flight packet is expired due to its message expiry interval
     */
    public abstract fun isExpired(now: Instant): Boolean

    override fun compareTo(other: io.github.nicolasfara.mktt.core.InFlightPacket): Int = this.key.compareTo(other.key)
}

public class InFlightPublish(
    public val source: io.github.nicolasfara.mktt.core.packet.Publish,
    timestamp: Instant,
    id: Long,
) : io.github.nicolasfara.mktt.core.InFlightPacket(timestamp, id) {

    init {
        require(source.qoS != _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE) {
            "PUBLISH packets with QoS 0 cannot be part of a transaction: $source"
        }
    }

    override val packetIdentifier: UShort
        get() = source.packetIdentifier!!

    override fun isExpired(now: Instant): Boolean =
        source.messageExpiryInterval != null && timestamp + source.messageExpiryInterval.toDuration() < now
}

public class InFlightPubrel(
    public val source: io.github.nicolasfara.mktt.core.packet.Pubrel,
    timestamp: Instant,
    id: Long,
) : io.github.nicolasfara.mktt.core.InFlightPacket(timestamp, id) {

    public constructor(inFlightPublish: io.github.nicolasfara.mktt.core.InFlightPublish, id: Long) :
        this(
            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubrel.Companion.from(inFlightPublish.source),
            inFlightPublish.timestamp,
            id,
        )

    override val packetIdentifier: UShort
        get() = source.packetIdentifier

    override fun isExpired(now: Instant): Boolean = false
}
