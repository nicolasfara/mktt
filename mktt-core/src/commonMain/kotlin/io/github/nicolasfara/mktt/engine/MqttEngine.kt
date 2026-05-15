package io.github.nicolasfara.mktt.engine

import io.github.nicolasfara.mktt.core.packet.Packet
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Transport abstraction for sending and receiving MQTT packets.
 */
interface MqttEngine : AutoCloseable {
    /**
     * Dispatcher used by the engine's coroutines.
     */
    val dispatcher: CoroutineDispatcher

    /**
     * Shared stream of decoded packets received from the remote peer.
     *
     * A malformed packet is emitted as a failed [Result] whose cause is usually
     * [io.github.nicolasfara.mktt.core.MalformedPacketException].
     */
    val packetResults: SharedFlow<Result<Packet>>

    /**
     * Current transport connection state.
     */
    val connected: StateFlow<Boolean>

    /**
     * Starts the transport connection.
     *
     * @return success when the transport is connected and [connected] has become `true`; otherwise a failure with the
     * cause of the connection failure.
     */
    suspend fun start(): Result<Unit>

    /**
     * Sends an MQTT packet.
     *
     * When the client is not connected, when this method is called or when sending the packet will
     * fail for some other reason, the returned result will be a failure.
     *
     * @param packet packet to send.
     * @return success when the packet was written to the transport; otherwise a failure.
     */
    suspend fun send(packet: Packet): Result<Unit>

    /**
     * Disconnects this engine from its remote peer.
     *
     * The engine remains reusable for later reconnections.
     */
    suspend fun disconnect()

    /**
     * Closes all engine resources.
     *
     * The engine cannot be reused after this method returns.
     */
    override fun close()
}
