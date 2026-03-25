package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.packet.Packet
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeMqttEngine : MqttEngine {
    private val _packetResults =
        MutableSharedFlow<Result<Packet>>(extraBufferCapacity = 32)
    private val _connected = MutableStateFlow(false)

    val sentPackets = mutableListOf<Packet>()
    var startHandler: suspend FakeMqttEngine.() -> Result<Unit> = {
        _connected.value = true
        Result.success(Unit)
    }
    var sendHandler: suspend FakeMqttEngine.(
        Packet,
    ) -> Result<Unit> = { packet ->
        sentPackets += packet
        Result.success(Unit)
    }

    override val packetResults = _packetResults.asSharedFlow()
    override val connected = _connected.asStateFlow()

    override suspend fun start(): Result<Unit> = startHandler()

    override suspend fun send(packet: Packet): Result<Unit> = sendHandler(packet)

    override suspend fun disconnect() {
        _connected.value = false
    }

    override fun close() = Unit

    suspend fun emit(packet: Packet) {
        _packetResults.emit(Result.success(packet))
    }

    suspend fun emitFailure(throwable: Throwable) {
        _packetResults.emit(Result.failure(throwable))
    }
}
