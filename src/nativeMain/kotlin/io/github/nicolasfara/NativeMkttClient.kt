package io.github.nicolasfara

import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private data class ActiveConnection(val transport: NativeTransportSession, val ioScope: CoroutineScope)

private enum class AckType {
    PubAck,
    PubRec,
    PubComp,
    SubAck,
    UnsubAck,
    PingResp,
}

private data class AckKey(val type: AckType, val packetId: Int)

private sealed class ClientCommand {
    data class Connect(val result: CompletableDeferred<Unit>) : ClientCommand()

    data class Disconnect(val result: CompletableDeferred<Unit>) : ClientCommand()

    data class Publish(
        val topic: String,
        val payload: ByteArray,
        val qos: MqttQoS,
        val result: CompletableDeferred<Unit>,
    ) : ClientCommand()

    data class Subscribe(val topic: String, val qos: MqttQoS, val result: CompletableDeferred<Unit>) : ClientCommand()

    data class Unsubscribe(val topic: String, val result: CompletableDeferred<Unit>) : ClientCommand()

    data class ConnectionLost(val cause: Throwable) : ClientCommand()

    data class ReconnectAttempt(val result: CompletableDeferred<Boolean>) : ClientCommand()
}

private sealed class ConnectionCloseCause {
    data object Expected : ConnectionCloseCause()

    data class Unexpected(val error: Throwable) : ConnectionCloseCause()
}

/**
 * Native [MkttClient] using Ktor sockets and an internal MQTT 5 codec.
 *
 * A single command actor serializes client lifecycle and API operations.
 */
@Suppress("TooManyFunctions")
internal class NativeMkttClient(
    override val dispatcher: CoroutineDispatcher,
    private val configuration: MqttClientConfiguration,
    private val transportFactory: NativeTransportFactory = KtorNativeTransportFactory,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(2),
    private val timing: NativeMkttClientTiming = NativeMkttClientTiming(),
    private val isTransientFailure: (Throwable) -> Boolean = DEFAULT_TRANSIENT_FAILURE_DETECTOR,
) : MkttClient {
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private val commandScope = CoroutineScope(dispatcher + SupervisorJob())
    private val commandChannel = Channel<ClientCommand>(Channel.UNLIMITED)

    private val incomingMessages = MutableSharedFlow<MqttMessage>(extraBufferCapacity = 1_000)
    private val subscribedTopics = mutableMapOf<String, MqttQoS>()
    private val subscribedFlows = mutableMapOf<String, Flow<MqttMessage>>()

    private val pendingAcks = mutableMapOf<AckKey, CompletableDeferred<ByteArray>>()
    private val pendingQoS2Messages = mutableMapOf<Int, MqttMessage>()
    private val acksMutex = Mutex()
    private val writeMutex = Mutex()

    private var packetIdCounter = 0
    private var activeConnection: ActiveConnection? = null
    private var reconnectJob: kotlinx.coroutines.Job? = null
    private var reconnectEnabled = false
    private var userDisconnected = false

    init {
        commandScope.launch { commandLoop() }
    }

    override suspend fun connect() {
        submitCommand { result -> ClientCommand.Connect(result) }
    }

    override suspend fun disconnect() {
        submitCommand { result -> ClientCommand.Disconnect(result) }
    }

    override suspend fun publish(topic: String, message: ByteArray, qos: MqttQoS) {
        submitCommand { result -> ClientCommand.Publish(topic, message, qos, result) }
    }

    override fun subscribe(topic: String, qos: MqttQoS): Flow<MqttMessage> = subscribedFlows.getOrPut(topic) {
        flow {
            submitCommand { result -> ClientCommand.Subscribe(topic, qos, result) }
            emitAll(incomingMessages.filter { matchesTopicFilter(it.topic, topic) })
        }.flowOn(dispatcher)
    }

    override suspend fun unsubscribe(topic: String) {
        submitCommand { result -> ClientCommand.Unsubscribe(topic, result) }
    }

    private suspend fun commandLoop() {
        for (command in commandChannel) {
            when (command) {
                is ClientCommand.Connect -> complete(command.result) { connectInternal() }

                is ClientCommand.Disconnect -> complete(command.result) { disconnectInternal() }

                is ClientCommand.Publish -> complete(command.result) {
                    publishInternal(command.topic, command.payload, command.qos)
                }

                is ClientCommand.Subscribe -> complete(command.result) {
                    subscribeInternal(command.topic, command.qos)
                }

                is ClientCommand.Unsubscribe -> complete(command.result) {
                    unsubscribeInternal(command.topic)
                }

                is ClientCommand.ConnectionLost -> onConnectionLost(command.cause)

                is ClientCommand.ReconnectAttempt -> complete(command.result) { reconnectAttemptInternal() }
            }
        }
    }

    private suspend fun connectInternal() {
        check(_connectionState.value is MqttConnectionState.Disconnected) {
            "Client is already connected or connecting"
        }
        userDisconnected = false
        reconnectEnabled = false
        reconnectJob?.cancelAndJoin()
        reconnectJob = null
        _connectionState.value = MqttConnectionState.Connecting

        recoverNonCancellation(
            block = {
                establishConnectionWithRetry()
                _connectionState.value = MqttConnectionState.Connected
            },
            onFailure = { error ->
                closeActiveConnection(ConnectionCloseCause.Expected)
                _connectionState.value = MqttConnectionState.ConnectionError(error)
                throw error
            },
        )
    }

    private suspend fun disconnectInternal() {
        check(_connectionState.value is MqttConnectionState.Connected) {
            "Client is not connected"
        }

        userDisconnected = true
        reconnectEnabled = false
        reconnectJob?.cancelAndJoin()
        reconnectJob = null

        val connection = requireActiveConnection()
        ignoreNonCancellation { sendDisconnect(connection.transport.writeChannel) }

        closeActiveConnection(ConnectionCloseCause.Expected)
        subscribedTopics.clear()
        subscribedFlows.clear()
        _connectionState.value = MqttConnectionState.Disconnected
    }

    private suspend fun publishInternal(topic: String, payload: ByteArray, qos: MqttQoS) {
        val connection = activeConnection
        if (_connectionState.value !is MqttConnectionState.Connected || connection == null) {
            if (userDisconnected) {
                return
            }
            error("Not connected")
        }

        when (qos) {
            MqttQoS.AtMostOnce -> {
                sendPublish(connection.transport.writeChannel, topic, payload, qos, packetId = 0)
            }

            MqttQoS.AtLeastOnce -> {
                val packetId = nextPacketId()
                awaitAck(AckKey(AckType.PubAck, packetId), ackTimeoutMs()) {
                    sendPublish(connection.transport.writeChannel, topic, payload, qos, packetId)
                }
            }

            MqttQoS.ExactlyOnce -> {
                val packetId = nextPacketId()
                awaitAck(AckKey(AckType.PubRec, packetId), ackTimeoutMs()) {
                    sendPublish(connection.transport.writeChannel, topic, payload, qos, packetId)
                }
                awaitAck(AckKey(AckType.PubComp, packetId), ackTimeoutMs()) {
                    sendPubRel(connection.transport.writeChannel, packetId)
                }
            }
        }
    }

    private suspend fun subscribeInternal(topic: String, qos: MqttQoS) {
        requireActiveConnection()
        if (subscribedTopics.containsKey(topic)) {
            return
        }

        sendSubscribeAndAwait(topic, qos)
        subscribedTopics[topic] = qos
    }

    private suspend fun unsubscribeInternal(topic: String) {
        check(subscribedTopics.containsKey(topic)) { "Topic not subscribed: $topic" }
        requireActiveConnection()

        sendUnsubscribeAndAwait(topic)
        subscribedTopics.remove(topic)
        subscribedFlows.remove(topic)
    }

    private suspend fun reconnectAttemptInternal(): Boolean {
        if (!reconnectEnabled || _connectionState.value !is MqttConnectionState.Connecting) {
            return true
        }

        return recoverNonCancellation(
            block = {
                establishConnectionWithRetry()
                resubscribeAll()
                _connectionState.value = MqttConnectionState.Connected
                reconnectEnabled = false
                true
            },
            onFailure = { error ->
                closeActiveConnection(ConnectionCloseCause.Expected)
                _connectionState.value = MqttConnectionState.ConnectionError(error)
                _connectionState.value = MqttConnectionState.Connecting
                false
            },
        )
    }

    private suspend fun onConnectionLost(cause: Throwable) {
        if (_connectionState.value is MqttConnectionState.Disconnected || activeConnection == null) {
            return
        }

        closeActiveConnection(ConnectionCloseCause.Unexpected(cause))

        _connectionState.value = MqttConnectionState.ConnectionError(cause)
        if (configuration.automaticReconnect) {
            userDisconnected = false
            _connectionState.value = MqttConnectionState.Connecting
            scheduleReconnect()
        } else {
            _connectionState.value = MqttConnectionState.Disconnected
        }
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) {
            return
        }
        reconnectEnabled = true

        reconnectJob = commandScope.launch {
            var backoff = timing.reconnectInitialDelayMs.coerceAtLeast(1L)
            val maxBackoff = timing.reconnectMaxDelayMs.coerceAtLeast(backoff)

            while (isActive && reconnectEnabled) {
                delay(backoff)
                val result = CompletableDeferred<Boolean>()
                commandChannel.send(ClientCommand.ReconnectAttempt(result))
                if (result.await()) {
                    return@launch
                }
                backoff = (backoff * 2).coerceAtMost(maxBackoff)
            }
        }
    }

    private suspend fun establishConnectionWithRetry() {
        var attempt = 0
        var backoff = timing.connectRetryInitialDelayMs.coerceAtLeast(1L)
        val maxBackoff = timing.connectRetryMaxDelayMs.coerceAtLeast(backoff)

        while (true) {
            attempt += 1
            val established = recoverNonCancellation(
                block = {
                    establishConnectionOnce()
                    true
                },
                onFailure = { error ->
                    closeActiveConnection(ConnectionCloseCause.Expected)
                    val shouldRetry =
                        isTransientFailure(error) &&
                            attempt < timing.connectRetryAttempts.coerceAtLeast(1)
                    if (!shouldRetry) {
                        throw error
                    }
                    delay(backoff)
                    backoff = (backoff * 2).coerceAtMost(maxBackoff)
                    false
                },
            )
            if (established) {
                return
            }
        }
    }

    private suspend fun establishConnectionOnce() {
        val timeoutMs = connectTimeoutMs()
        val transport = withIoTimeout(timeoutMs) {
            transportFactory.open(configuration, ioDispatcher)
        }

        recoverNonCancellation(
            block = {
                withIoTimeout(timeoutMs) {
                    sendConnect(transport.writeChannel)
                    receiveConnAck(transport.readChannel)
                }
            },
            onFailure = { error ->
                ignoreNonCancellation { transport.close() }
                throw error
            },
        )

        val ioScope = CoroutineScope(ioDispatcher + SupervisorJob())
        activeConnection = ActiveConnection(transport = transport, ioScope = ioScope)

        ioScope.launch {
            recoverNonCancellation(
                block = { readLoop(transport.readChannel) },
                onFailure = { error ->
                    notifyConnectionLost(error)
                },
            )
        }

        if (configuration.keepAliveInterval > 0) {
            ioScope.launch {
                recoverNonCancellation(
                    block = { keepAliveLoop(transport.writeChannel) },
                    onFailure = { error ->
                        notifyConnectionLost(error)
                    },
                )
            }
        }
    }

    private suspend fun resubscribeAll() {
        val topics = subscribedTopics.toMap()
        for ((topic, qos) in topics) {
            sendSubscribeAndAwait(topic, qos)
        }
    }

    private suspend fun sendSubscribeAndAwait(topic: String, qos: MqttQoS) {
        val connection = activeConnection ?: error("Not connected")
        val packetId = nextPacketId()
        awaitAck(AckKey(AckType.SubAck, packetId), ackTimeoutMs()) {
            sendSubscribe(connection.transport.writeChannel, topic, qos, packetId)
        }
    }

    private suspend fun sendUnsubscribeAndAwait(topic: String) {
        val connection = activeConnection ?: error("Not connected")
        val packetId = nextPacketId()
        awaitAck(AckKey(AckType.UnsubAck, packetId), ackTimeoutMs()) {
            sendUnsubscribe(connection.transport.writeChannel, topic, packetId)
        }
    }

    private suspend fun keepAliveLoop(writeChannel: ByteWriteChannel) {
        val intervalMs = configuration.keepAliveInterval.coerceAtLeast(1L) * 1_000L
        while (true) {
            delay(intervalMs)
            awaitAck(AckKey(AckType.PingResp, 0), ackTimeoutMs()) {
                sendPingReq(writeChannel)
            }
        }
    }

    private suspend fun readLoop(readChannel: ByteReadChannel) {
        while (true) {
            val packet = readChannel.readMqttPacket()
            when {
                isPublishPacket(packet.fixedHeader) -> handleIncomingPublish(packet)

                packet.fixedHeader == MQTT_PUBACK -> {
                    handleAckPacket(AckKey(AckType.PubAck, packet.payload.readMqttPacketId()), packet.payload, "PUBACK")
                }

                packet.fixedHeader == MQTT_PUBREC -> {
                    handleAckPacket(AckKey(AckType.PubRec, packet.payload.readMqttPacketId()), packet.payload, "PUBREC")
                }

                packet.fixedHeader == MQTT_PUBCOMP -> {
                    handleAckPacket(
                        AckKey(AckType.PubComp, packet.payload.readMqttPacketId()),
                        packet.payload,
                        "PUBCOMP",
                    )
                }

                packet.fixedHeader == MQTT_SUBACK -> handleSubAck(packet.payload)

                packet.fixedHeader == MQTT_UNSUBACK -> handleUnsubAck(packet.payload)

                isPubRelPacket(packet.fixedHeader) -> handleIncomingPubRel(packet.payload)

                packet.fixedHeader == MQTT_PINGRESP -> completeAck(AckKey(AckType.PingResp, 0), packet.payload)

                packet.fixedHeader == MQTT_DISCONNECT -> throw packet.payload.toDisconnectException()
            }
        }
    }

    private suspend fun handleAckPacket(ackKey: AckKey, payload: ByteArray, packetName: String) {
        val reasonCode = payload.readAckReasonCode(packetName)
        if (reasonCode >= MQTT_REASON_ERROR_THRESHOLD) {
            failAck(ackKey, IllegalStateException("$packetName failed: ${describeReasonCode(reasonCode)}"))
            return
        }
        completeAck(ackKey, payload)
    }

    private suspend fun handleSubAck(payload: ByteArray) {
        val ackKey = AckKey(AckType.SubAck, payload.readMqttPacketId())
        val failureReason = payload.readReasonCodes("SUBACK").firstOrNull { it >= MQTT_REASON_ERROR_THRESHOLD }
        if (failureReason != null) {
            failAck(ackKey, IllegalStateException("Subscription rejected: ${describeReasonCode(failureReason)}"))
            return
        }
        completeAck(ackKey, payload)
    }

    private suspend fun handleUnsubAck(payload: ByteArray) {
        val ackKey = AckKey(AckType.UnsubAck, payload.readMqttPacketId())
        val failureReason = payload.readReasonCodes("UNSUBACK").firstOrNull { it >= MQTT_REASON_ERROR_THRESHOLD }
        if (failureReason != null) {
            failAck(ackKey, IllegalStateException("Unsubscription rejected: ${describeReasonCode(failureReason)}"))
            return
        }
        completeAck(ackKey, payload)
    }

    private suspend fun completeAck(ackKey: AckKey, payload: ByteArray) {
        val deferred = acksMutex.withLock { pendingAcks.remove(ackKey) }
        deferred?.complete(payload)
    }

    private suspend fun failAck(ackKey: AckKey, error: Throwable) {
        val deferred = acksMutex.withLock { pendingAcks.remove(ackKey) }
        deferred?.completeExceptionally(error)
    }

    private suspend fun handleIncomingPublish(packet: NativeMqttPacket) {
        val publish = packet.payload.parseIncomingPublish(packet.fixedHeader)
        val mqttMessage = MqttMessage(
            topic = publish.topic,
            payload = publish.payload,
            qos = publish.qos,
            retained = publish.retain,
        )

        when (publish.qos) {
            MqttQoS.AtMostOnce -> incomingMessages.emit(mqttMessage)

            MqttQoS.AtLeastOnce -> {
                incomingMessages.emit(mqttMessage)
                activeConnection?.let { sendPubAck(it.transport.writeChannel, publish.packetId) }
            }

            MqttQoS.ExactlyOnce -> {
                acksMutex.withLock { pendingQoS2Messages[publish.packetId] = mqttMessage }
                activeConnection?.let { sendPubRec(it.transport.writeChannel, publish.packetId) }
            }
        }
    }

    private suspend fun handleIncomingPubRel(payload: ByteArray) {
        val packetId = payload.readMqttPacketId()
        val reasonCode = payload.readAckReasonCode("PUBREL")
        val message = acksMutex.withLock { pendingQoS2Messages.remove(packetId) }
        if (reasonCode < MQTT_REASON_ERROR_THRESHOLD && message != null) {
            incomingMessages.emit(message)
        }
        activeConnection?.let { sendPubComp(it.transport.writeChannel, packetId) }
    }

    private suspend fun awaitAck(ackKey: AckKey, timeoutMs: Long, onSend: suspend () -> Unit) {
        val deferred = CompletableDeferred<ByteArray>()
        acksMutex.withLock { pendingAcks[ackKey] = deferred }
        try {
            onSend()
            withIoTimeout(timeoutMs) {
                deferred.await()
            }
        } finally {
            acksMutex.withLock { pendingAcks.remove(ackKey) }
        }
    }

    private suspend fun closeActiveConnection(cause: ConnectionCloseCause) {
        val connection = activeConnection ?: return
        activeConnection = null

        connection.ioScope.cancel()
        val pendingError = when (cause) {
            ConnectionCloseCause.Expected -> IllegalStateException("Connection closed")
            is ConnectionCloseCause.Unexpected -> cause.error
        }

        acksMutex.withLock {
            pendingAcks.values.forEach { it.completeExceptionally(pendingError) }
            pendingAcks.clear()
            pendingQoS2Messages.clear()
        }

        ignoreNonCancellation { connection.transport.close() }
    }

    private fun notifyConnectionLost(error: Throwable) {
        commandChannel.trySend(ClientCommand.ConnectionLost(error))
    }

    private fun requireActiveConnection(): ActiveConnection {
        val connection = activeConnection
        check(_connectionState.value is MqttConnectionState.Connected && connection != null) {
            "Not connected"
        }
        return connection
    }

    private fun nextPacketId(): Int {
        packetIdCounter = (packetIdCounter % MQTT_MAX_PACKET_ID) + 1
        return packetIdCounter
    }

    private suspend fun <T> submitCommand(builder: (CompletableDeferred<T>) -> ClientCommand): T {
        val result = CompletableDeferred<T>()
        commandChannel.send(builder(result))
        return result.await()
    }

    private suspend fun <T> complete(result: CompletableDeferred<T>, block: suspend () -> T) {
        recoverNonCancellation(
            block = {
                result.complete(block())
            },
            onFailure = { error ->
                result.completeExceptionally(error)
            },
        )
    }

    private fun connectTimeoutMs(): Long = configuration.connectionTimeout.coerceAtLeast(1L) * 1_000L

    private fun ackTimeoutMs(): Long = timing.ackTimeoutMs.coerceAtLeast(1L)

    private suspend fun <T> withIoTimeout(timeoutMs: Long, block: suspend () -> T): T = withContext(ioDispatcher) {
        withTimeout(timeoutMs) {
            block()
        }
    }

    private suspend fun receiveConnAck(readChannel: ByteReadChannel) {
        val packet = readChannel.readMqttPacket()
        check(packet.fixedHeader == MQTT_CONNACK) {
            "Expected CONNACK but received packet type ${packet.fixedHeader}"
        }
        val reasonCode = packet.payload.readConnAckReasonCode()
        if (reasonCode != MQTT_REASON_SUCCESS) {
            throw IllegalStateException("MQTT connection refused: ${describeConnectReasonCode(reasonCode)}")
        }
    }

    private suspend fun sendPacket(writeChannel: ByteWriteChannel, fixedHeader: Int, payload: ByteArray) {
        val packet = buildMqttPacket(fixedHeader, payload)
        withContext(ioDispatcher) {
            writeMutex.withLock {
                writeChannel.writeFully(packet)
                writeChannel.flush()
            }
        }
    }

    private suspend fun sendConnect(writeChannel: ByteWriteChannel) {
        sendPacket(writeChannel, MQTT_CONNECT, buildConnectPayload(configuration))
    }

    private suspend fun sendPublish(
        writeChannel: ByteWriteChannel,
        topic: String,
        payload: ByteArray,
        qos: MqttQoS,
        packetId: Int,
    ) {
        sendPacket(
            writeChannel = writeChannel,
            fixedHeader = buildPublishFixedHeader(qos),
            payload = buildPublishPayload(topic, payload, qos, packetId),
        )
    }

    private suspend fun sendPubAck(writeChannel: ByteWriteChannel, packetId: Int) {
        sendPacket(writeChannel, MQTT_PUBACK, buildReasonCodeAckPayload(packetId))
    }

    private suspend fun sendPubRec(writeChannel: ByteWriteChannel, packetId: Int) {
        sendPacket(writeChannel, MQTT_PUBREC, buildReasonCodeAckPayload(packetId))
    }

    private suspend fun sendPubRel(writeChannel: ByteWriteChannel, packetId: Int) {
        sendPacket(writeChannel, MQTT_PUBREL, buildReasonCodeAckPayload(packetId))
    }

    private suspend fun sendPubComp(writeChannel: ByteWriteChannel, packetId: Int) {
        sendPacket(writeChannel, MQTT_PUBCOMP, buildReasonCodeAckPayload(packetId))
    }

    private suspend fun sendSubscribe(writeChannel: ByteWriteChannel, topic: String, qos: MqttQoS, packetId: Int) {
        sendPacket(writeChannel, MQTT_SUBSCRIBE, buildSubscribePayload(topic, qos, packetId))
    }

    private suspend fun sendUnsubscribe(writeChannel: ByteWriteChannel, topic: String, packetId: Int) {
        sendPacket(writeChannel, MQTT_UNSUBSCRIBE, buildUnsubscribePayload(topic, packetId))
    }

    private suspend fun sendPingReq(writeChannel: ByteWriteChannel) {
        sendPacket(writeChannel, MQTT_PINGREQ, ByteArray(0))
    }

    private suspend fun sendDisconnect(writeChannel: ByteWriteChannel) {
        sendPacket(writeChannel, MQTT_DISCONNECT, buildDisconnectPayload())
    }
}
