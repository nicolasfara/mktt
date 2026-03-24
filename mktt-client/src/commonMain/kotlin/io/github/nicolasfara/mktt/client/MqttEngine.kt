package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.packet.Packet
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Interface for doing the network stuff of sending and receiving MQTT packets.
 */
public interface MqttEngine : AutoCloseable {

    /**
     * A shared flow of the received packets. Will contain a failure if a malformed packet has been received (together
     * with a [io.github.nicolasfara.mktt.core.MalformedPacketException]).
     */
    public val packetResults: SharedFlow<Result<io.github.nicolasfara.mktt.core.packet.Packet>>

    /**
     * Provides information about the connection state of this engine.
     */
    public val connected: StateFlow<Boolean>

    /**
     * Starts a connection of this engine and returns the result. When this method returns a success, the
     * [connected] state will be `true`.
     */
    public suspend fun start(): Result<Unit>

    /**
     * Send an MQTT packet.
     *
     * When the client is not connected, when this method is called or when sending the packet will
     * fail for some other reason, the returned result will be a failure.
     */
    public suspend fun send(packet: io.github.nicolasfara.mktt.core.packet.Packet): Result<Unit>

    /**
     * Disconnect this engine from its remote. The engine will be reusable after this call for reconnections.
     */
    public suspend fun disconnect()

    /**
     * Closes all resources of this engine, it will no longer be usable after this method!
     */
    public override fun close()
}
