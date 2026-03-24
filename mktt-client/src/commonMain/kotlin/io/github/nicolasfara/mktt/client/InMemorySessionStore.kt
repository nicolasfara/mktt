@file:OptIn(ExperimentalAtomicApi::class, ExperimentalTime::class)

package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.InFlightPacket
import io.github.nicolasfara.mktt.core.InFlightPublish
import io.github.nicolasfara.mktt.core.InFlightPubrel
import io.github.nicolasfara.mktt.core.SessionStore
import io.github.nicolasfara.mktt.core.packet.Publish
import io.github.nicolasfara.mktt.core.packet.Pubrel
import io.github.nicolasfara.mktt.core.util.Logger
import kotlin.collections.contains
import kotlin.collections.plus
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

public open class InMemorySessionStore(private val clock: Clock = Clock.System) :
    io.github.nicolasfara.mktt.core.SessionStore {

    private val outgoingPackets =
        _root_ide_package_.io.github.nicolasfara.mktt.client.AtomicMap<UShort, io.github.nicolasfara.mktt.core.InFlightPacket>()

    private val incomingPackets = mutableSetOf<UShort>()

    private val sequence = AtomicLong(0)

    override fun store(
        source: io.github.nicolasfara.mktt.core.packet.Publish,
    ): io.github.nicolasfara.mktt.core.InFlightPublish {
        _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.v { "Storing in-flight packet $source" }
        val packet = _root_ide_package_.io.github.nicolasfara.mktt.core.InFlightPublish(
            source,
            clock.now(),
            sequence.incrementAndFetch(),
        )
        outgoingPackets[packet.packetIdentifier] = packet
        return packet
    }

    override fun replace(
        source: io.github.nicolasfara.mktt.core.InFlightPublish,
    ): io.github.nicolasfara.mktt.core.InFlightPubrel {
        val packetIdentifier = source.packetIdentifier

        if (!outgoingPackets.containsKey(packetIdentifier)) {
            throw NoSuchElementException("No PUBLISH packet found with identifier $packetIdentifier")
        }

        return _root_ide_package_.io.github.nicolasfara.mktt.core.InFlightPubrel(source, sequence.incrementAndFetch())
            .also { inFlight ->
                _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.v {
                    "Replacing PUBLISH packet with identifier $packetIdentifier with $inFlight"
                }
                outgoingPackets[packetIdentifier] = inFlight
            }
    }

    override fun acknowledge(packet: io.github.nicolasfara.mktt.core.InFlightPacket) {
        outgoingPackets.remove(packet.packetIdentifier)
        _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.v { "Acknowledged PUBLISH packet $packet" }
    }

    override fun rememberIncomingPacketId(publish: io.github.nicolasfara.mktt.core.packet.Publish): Boolean {
        val packetIdentifier = publish.packetIdentifier
        require(packetIdentifier != null) { "Packets without packet identifier cannot be part of a transaction" }

        return !incomingPackets.add(packetIdentifier)
    }

    override fun hasIncomingPacketId(publish: io.github.nicolasfara.mktt.core.packet.Publish): Boolean =
        incomingPackets.contains(publish.packetIdentifier)

    override fun releaseIncomingPacketId(pubrel: io.github.nicolasfara.mktt.core.packet.Pubrel) {
        incomingPackets.remove(pubrel.packetIdentifier)
    }

    override fun unacknowledgedPackets(): List<io.github.nicolasfara.mktt.core.InFlightPacket> {
        // Does not need to be thread safe:
        val now = clock.now()
        val all = outgoingPackets.ref.load().filterNot { it.value.isExpired(now) }
        outgoingPackets.ref.store(all)

        return all.values.sorted()
    }

    override fun clear() {
        outgoingPackets.clear()
        incomingPackets.clear()
    }
}

private class AtomicMap<K, V> {

    val ref = AtomicReference(emptyMap<K, V>())

    fun containsKey(key: K): Boolean = ref.load().containsKey(key)

    operator fun set(key: K, value: V) {
        while (true) {
            val current = ref.load()
            val updated = current + (key to value)
            if (ref.compareAndSet(current, updated)) {
                break
            }
        }
    }

    fun remove(key: K) {
        while (true) {
            val current = ref.load()
            if (!current.containsKey(key)) {
                break // Nothing to remove
            }
            val updated = current - key
            if (ref.compareAndSet(current, updated)) {
                break
            }
        }
    }

    fun clear() {
        ref.store(emptyMap())
    }
}
