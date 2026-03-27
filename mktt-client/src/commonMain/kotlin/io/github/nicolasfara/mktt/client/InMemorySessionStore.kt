@file:OptIn(ExperimentalAtomicApi::class, ExperimentalTime::class)

package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.InFlightPacket
import io.github.nicolasfara.mktt.core.InFlightPublish
import io.github.nicolasfara.mktt.core.InFlightPubrel
import io.github.nicolasfara.mktt.core.SessionStore
import io.github.nicolasfara.mktt.core.packet.Publish
import io.github.nicolasfara.mktt.core.packet.Pubrel
import io.github.nicolasfara.mktt.core.util.Logger
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * In-memory [SessionStore] implementation used to track in-flight MQTT packets.
 */
class InMemorySessionStore(private val clock: Clock = Clock.System) : SessionStore {
    private val state = AtomicReference(SessionState())
    private val sequence = AtomicLong(0)

    override fun store(source: Publish): InFlightPublish {
        Logger.v { "Storing in-flight packet $source" }
        val packet = InFlightPublish(
            source,
            clock.now(),
            sequence.incrementAndFetch(),
        )
        updateState { current ->
            current.copy(outgoingPackets = current.outgoingPackets + (packet.packetIdentifier to packet))
        }
        return packet
    }

    override fun replace(source: InFlightPublish): InFlightPubrel {
        val packetIdentifier = source.packetIdentifier
        var replacement: InFlightPubrel? = null
        updateState { current ->
            if (!current.outgoingPackets.containsKey(packetIdentifier)) {
                throw NoSuchElementException("No PUBLISH packet found with identifier $packetIdentifier")
            }

            val inFlight = InFlightPubrel(source, sequence.incrementAndFetch())
            replacement = inFlight
            current.copy(outgoingPackets = current.outgoingPackets + (packetIdentifier to inFlight))
        }

        return requireNotNull(replacement).also { inFlight ->
            Logger.v {
                "Replacing PUBLISH packet with identifier $packetIdentifier with $inFlight"
            }
        }
    }

    override fun acknowledge(packet: InFlightPacket) {
        updateState { current ->
            current.copy(outgoingPackets = current.outgoingPackets - packet.packetIdentifier)
        }
        Logger.v { "Acknowledged packet $packet" }
    }

    override fun rememberIncomingPacketId(publish: Publish): Boolean {
        val packetIdentifier = requireNotNull(publish.packetIdentifier) {
            "Packets without packet identifier cannot be part of a transaction"
        }

        var wasAdded = false
        updateState { current ->
            if (packetIdentifier in current.incomingPacketIds) {
                return@updateState current
            }

            wasAdded = true
            current.copy(incomingPacketIds = current.incomingPacketIds + packetIdentifier)
        }
        return wasAdded
    }

    override fun hasIncomingPacketId(publish: Publish): Boolean {
        val packetIdentifier = publish.packetIdentifier ?: return false
        return packetIdentifier in state.load().incomingPacketIds
    }

    override fun releaseIncomingPacketId(pubrel: Pubrel) {
        updateState { current ->
            current.copy(incomingPacketIds = current.incomingPacketIds - pubrel.packetIdentifier)
        }
    }

    override fun unacknowledgedPackets(): List<InFlightPacket> {
        val now = clock.now()
        val updated = updateState { current ->
            val unexpired = current.outgoingPackets.filterValues { packet -> !packet.isExpired(now) }
            current.copy(outgoingPackets = unexpired)
        }
        return updated.outgoingPackets.values.sorted()
    }

    override fun clear() {
        state.store(SessionState())
    }

    private inline fun updateState(transform: (SessionState) -> SessionState): SessionState {
        var current: SessionState
        var updated: SessionState
        do {
            current = state.load()
            updated = transform(current)
        } while (!state.compareAndSet(current, updated))
        return updated
    }
}

private data class SessionState(
    val outgoingPackets: Map<UShort, InFlightPacket> = emptyMap(),
    val incomingPacketIds: Set<UShort> = emptySet(),
)
