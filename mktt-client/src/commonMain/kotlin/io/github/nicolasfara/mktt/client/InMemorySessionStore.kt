@file:OptIn(ExperimentalAtomicApi::class, ExperimentalTime::class)

package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.InFlightPacket
import io.github.nicolasfara.mktt.core.InFlightPublish
import io.github.nicolasfara.mktt.core.InFlightPubrel
import io.github.nicolasfara.mktt.core.SessionStore
import io.github.nicolasfara.mktt.core.packet.Publish
import io.github.nicolasfara.mktt.core.packet.Pubrel
import io.github.nicolasfara.mktt.core.util.Logger
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * In-memory [SessionStore] implementation used to track in-flight MQTT packets.
 */
class InMemorySessionStore(private val clock: Clock = Clock.System) : SessionStore {
    private val state = AtomicReference(SessionState())

    override fun store(source: Publish): InFlightPublish {
        Logger.v { "Storing in-flight packet $source" }
        return updateState { current ->
            val nextSequence = current.sequence + 1
            val packet = InFlightPublish(
                source,
                clock.now(),
                nextSequence,
            )
            current.copy(
                sequence = nextSequence,
                outgoingPackets = current.outgoingPackets + (packet.packetIdentifier to packet),
            ) to packet
        }
    }

    override fun replace(source: InFlightPublish): InFlightPubrel {
        val packetIdentifier = source.packetIdentifier
        val replacement = updateState { current ->
            if (!current.outgoingPackets.containsKey(packetIdentifier)) {
                throw NoSuchElementException("No PUBLISH packet found with identifier $packetIdentifier")
            }

            val nextSequence = current.sequence + 1
            val inFlight = InFlightPubrel(source, nextSequence)
            current.copy(
                sequence = nextSequence,
                outgoingPackets = current.outgoingPackets + (packetIdentifier to inFlight),
            ) to inFlight
        }

        return replacement.also { inFlight ->
            Logger.v {
                "Replacing PUBLISH packet with identifier $packetIdentifier with $inFlight"
            }
        }
    }

    override fun acknowledge(packet: InFlightPacket) {
        updateState { current ->
            current.copy(outgoingPackets = current.outgoingPackets - packet.packetIdentifier) to Unit
        }
        Logger.v { "Acknowledged packet $packet" }
    }

    override fun rememberIncomingPacketId(publish: Publish): Boolean {
        val packetIdentifier = requireNotNull(publish.packetIdentifier) {
            "Packets without packet identifier cannot be part of a transaction"
        }

        return updateState { current ->
            if (packetIdentifier in current.incomingPacketIds) {
                current to false
            } else {
                current.copy(incomingPacketIds = current.incomingPacketIds + packetIdentifier) to true
            }
        }
    }

    override fun hasIncomingPacketId(publish: Publish): Boolean {
        val packetIdentifier = publish.packetIdentifier ?: return false
        return packetIdentifier in state.load().incomingPacketIds
    }

    override fun releaseIncomingPacketId(pubrel: Pubrel) {
        updateState { current ->
            current.copy(incomingPacketIds = current.incomingPacketIds - pubrel.packetIdentifier) to Unit
        }
    }

    override fun unacknowledgedPackets(): List<InFlightPacket> {
        val now = clock.now()
        updateState { current ->
            val unexpired = current.outgoingPackets.filterValues { packet -> !packet.isExpired(now) }
            current.copy(outgoingPackets = unexpired) to Unit
        }
        return state.load().outgoingPackets.values.sorted()
    }

    override fun clear() {
        state.store(SessionState())
    }

    private inline fun <T> updateState(transform: (SessionState) -> Pair<SessionState, T>): T {
        var current: SessionState
        var updated: SessionState
        var result: T
        do {
            current = state.load()
            val update = transform(current)
            updated = update.first
            result = update.second
        } while (!state.compareAndSet(current, updated))
        return result
    }
}

private data class SessionState(
    val sequence: Long = 0,
    val outgoingPackets: Map<UShort, InFlightPacket> = emptyMap(),
    val incomingPacketIds: Set<UShort> = emptySet(),
)
