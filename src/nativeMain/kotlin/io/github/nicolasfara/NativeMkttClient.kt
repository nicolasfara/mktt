package io.github.nicolasfara

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.errors.PosixException
import io.ktor.utils.io.readFully
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.CancellationException
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

// MQTT 3.1.1 fixed-header byte constants
private const val MQTT_CONNECT = 0x10
private const val MQTT_CONNACK = 0x20
private const val MQTT_PUBLISH_TYPE = 0x30
private const val MQTT_PUBACK = 0x40
private const val MQTT_PUBREC = 0x50
private const val MQTT_PUBREL = 0x62 // type bits 0x60 | reserved bits 0x02
private const val MQTT_PUBCOMP = 0x70
private const val MQTT_SUBSCRIBE = 0x82 // type bits 0x80 | reserved bits 0x02
private const val MQTT_SUBACK = 0x90
private const val MQTT_UNSUBSCRIBE = 0xa2 // type bits 0xa0 | reserved bits 0x02
private const val MQTT_UNSUBACK = 0xb0
private const val MQTT_PINGREQ = 0xc0
private const val MQTT_PINGRESP = 0xd0
private const val MQTT_DISCONNECT = 0xe0

// MQTT 3.1.1 protocol name and version
private const val MQTT_PROTOCOL_NAME = "MQTT"
private const val MQTT_PROTOCOL_LEVEL = 4

// CONNACK return codes
private const val CONNACK_ACCEPTED = 0
private const val CONNACK_UNACCEPTABLE_PROTOCOL = 1
private const val CONNACK_IDENTIFIER_REJECTED = 2
private const val CONNACK_SERVER_UNAVAILABLE = 3
private const val CONNACK_BAD_CREDENTIALS = 4
private const val CONNACK_NOT_AUTHORIZED = 5

private const val MAX_REMAINING_LENGTH_BYTES = 4
private const val MAX_REMAINING_LENGTH = 268_435_455
private const val DEFAULT_CONNECT_RETRY_ATTEMPTS = 3
private const val DEFAULT_CONNECT_RETRY_INITIAL_DELAY_MS = 100L
private const val DEFAULT_CONNECT_RETRY_MAX_DELAY_MS = 1_000L
private const val DEFAULT_RECONNECT_INITIAL_DELAY_MS = 500L
private const val DEFAULT_RECONNECT_MAX_DELAY_MS = 10_000L
private const val DEFAULT_ACK_TIMEOUT_MS = 10_000L

internal data class NativeMkttClientTiming(
    val connectRetryAttempts: Int = DEFAULT_CONNECT_RETRY_ATTEMPTS,
    val connectRetryInitialDelayMs: Long = DEFAULT_CONNECT_RETRY_INITIAL_DELAY_MS,
    val connectRetryMaxDelayMs: Long = DEFAULT_CONNECT_RETRY_MAX_DELAY_MS,
    val reconnectInitialDelayMs: Long = DEFAULT_RECONNECT_INITIAL_DELAY_MS,
    val reconnectMaxDelayMs: Long = DEFAULT_RECONNECT_MAX_DELAY_MS,
    val ackTimeoutMs: Long = DEFAULT_ACK_TIMEOUT_MS,
)

internal fun interface NativeTransportFactory {
    suspend fun open(configuration: MqttClientConfiguration, ioDispatcher: CoroutineDispatcher): NativeTransportSession
}

internal interface NativeTransportSession {
    val readChannel: ByteReadChannel
    val writeChannel: ByteWriteChannel

    suspend fun close()
}

internal val DEFAULT_TRANSIENT_FAILURE_DETECTOR: (Throwable) -> Boolean = { error ->
    error is PosixException.TryAgainException
}

private object KtorNativeTransportFactory : NativeTransportFactory {
    override suspend fun open(
        configuration: MqttClientConfiguration,
        ioDispatcher: CoroutineDispatcher,
    ): NativeTransportSession = withContext(ioDispatcher) {
        val selectorManager = SelectorManager(ioDispatcher)
        var socket: Socket? = null
        try {
            val rawSocket = aSocket(selectorManager).tcp().connect(
                hostname = configuration.brokerUrl,
                port = configuration.port,
            )
            socket = if (configuration.ssl) rawSocket.tls(coroutineContext) else rawSocket
            val connectedSocket = socket
            KtorNativeTransportSession(
                selectorManager = selectorManager,
                socket = connectedSocket,
                readChannel = connectedSocket.openReadChannel(),
                writeChannel = connectedSocket.openWriteChannel(autoFlush = false),
            )
        } catch (error: Throwable) {
            socket?.close()
            selectorManager.close()
            throw error
        }
    }
}

private class KtorNativeTransportSession(
    private val selectorManager: SelectorManager,
    private val socket: Socket,
    override val readChannel: ByteReadChannel,
    override val writeChannel: ByteWriteChannel,
) : NativeTransportSession {
    override suspend fun close() {
        socket.close()
        selectorManager.close()
    }
}

private data class ActiveConnection(
    val transport: NativeTransportSession,
    val ioScope: CoroutineScope,
)

private enum class AckType {
    PubAck,
    PubRec,
    PubComp,
    SubAck,
    UnsubAck,
    PingResp,
}

private data class AckKey(
    val type: AckType,
    val packetId: Int,
)

private sealed class ClientCommand {
    data class Connect(val result: CompletableDeferred<Unit>) : ClientCommand()

    data class Disconnect(val result: CompletableDeferred<Unit>) : ClientCommand()

    data class Publish(
        val topic: String,
        val payload: ByteArray,
        val qos: MqttQoS,
        val result: CompletableDeferred<Unit>,
    ) : ClientCommand()

    data class Subscribe(
        val topic: String,
        val qos: MqttQoS,
        val result: CompletableDeferred<Unit>,
    ) : ClientCommand()

    data class Unsubscribe(
        val topic: String,
        val result: CompletableDeferred<Unit>,
    ) : ClientCommand()

    data class ConnectionLost(val cause: Throwable) : ClientCommand()

    data class ReconnectAttempt(val result: CompletableDeferred<Boolean>) : ClientCommand()
}

/**
 * Native [MkttClient] using Ktor sockets and an internal MQTT 3.1.1 codec.
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

    private val incomingMessages = MutableSharedFlow<MqttMessage>(extraBufferCapacity = 1000)
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

    override fun subscribe(topic: String, qos: MqttQoS): Flow<MqttMessage> {
        return subscribedFlows.getOrPut(topic) {
            flow {
                submitCommand { result -> ClientCommand.Subscribe(topic, qos, result) }
                emitAll(incomingMessages.filter { matchesTopicFilter(it.topic, topic) })
            }.flowOn(dispatcher)
        }
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
                is ClientCommand.ReconnectAttempt -> complete(command.result) {
                    reconnectAttemptInternal()
                }
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

        try {
            establishConnectionWithRetry()
            _connectionState.value = MqttConnectionState.Connected
        } catch (error: Throwable) {
            closeActiveConnection(ConnectionCloseCause.Expected)
            _connectionState.value = MqttConnectionState.ConnectionError(error)
            throw error
        }
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
        runCatching {
            sendDisconnect(connection.transport.writeChannel)
        }

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
            MqttQoS.AtMostOnce -> sendPublish(connection.transport.writeChannel, topic, payload, qos, packetId = 0)
            MqttQoS.AtLeastOnce -> {
                val packetId = nextPacketId()
                awaitAck(
                    ackKey = AckKey(AckType.PubAck, packetId),
                    timeoutMs = ackTimeoutMs(),
                ) {
                    sendPublish(connection.transport.writeChannel, topic, payload, qos, packetId)
                }
            }
            MqttQoS.ExactlyOnce -> {
                val packetId = nextPacketId()
                awaitAck(
                    ackKey = AckKey(AckType.PubRec, packetId),
                    timeoutMs = ackTimeoutMs(),
                ) {
                    sendPublish(connection.transport.writeChannel, topic, payload, qos, packetId)
                }
                awaitAck(
                    ackKey = AckKey(AckType.PubComp, packetId),
                    timeoutMs = ackTimeoutMs(),
                ) {
                    sendPubRel(connection.transport.writeChannel, packetId)
                }
            }
        }
    }

    private suspend fun subscribeInternal(topic: String, qos: MqttQoS) {
        requireActiveConnection()

        val existingQoS = subscribedTopics[topic]
        if (existingQoS != null) {
            return
        }

        try {
            sendSubscribeAndAwait(topic, qos)
            subscribedTopics[topic] = qos
        } catch (error: Throwable) {
            throw error
        }
    }

    private suspend fun unsubscribeInternal(topic: String) {
        val removed = subscribedTopics[topic] ?: error("Topic not subscribed: $topic")
        requireActiveConnection()

        try {
            sendUnsubscribeAndAwait(topic)
            if (subscribedTopics[topic] == removed) {
                subscribedTopics.remove(topic)
            }
            subscribedFlows.remove(topic)
        } catch (error: Throwable) {
            throw error
        }
    }

    private suspend fun reconnectAttemptInternal(): Boolean {
        if (!reconnectEnabled || _connectionState.value !is MqttConnectionState.Connecting) {
            return true
        }

        return try {
            establishConnectionWithRetry()
            resubscribeAll()
            _connectionState.value = MqttConnectionState.Connected
            reconnectEnabled = false
            true
        } catch (error: Throwable) {
            closeActiveConnection(ConnectionCloseCause.Expected)
            _connectionState.value = MqttConnectionState.ConnectionError(error)
            _connectionState.value = MqttConnectionState.Connecting
            false
        }
    }

    private suspend fun onConnectionLost(cause: Throwable) {
        if (_connectionState.value is MqttConnectionState.Disconnected) {
            return
        }

        if (activeConnection == null) {
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
            try {
                establishConnectionOnce()
                return
            } catch (error: Throwable) {
                closeActiveConnection(ConnectionCloseCause.Expected)
                val shouldRetry =
                    isTransientFailure(error) && attempt < timing.connectRetryAttempts.coerceAtLeast(1)
                if (!shouldRetry) {
                    throw error
                }
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(maxBackoff)
            }
        }
    }

    private suspend fun establishConnectionOnce() {
        val timeoutMs = connectTimeoutMs()
        val transport = withIoTimeout(timeoutMs) {
            transportFactory.open(configuration, ioDispatcher)
        }

        try {
            withIoTimeout(timeoutMs) {
                sendConnect(transport.writeChannel)
                receiveConnAck(transport.readChannel)
            }
        } catch (error: Throwable) {
            transport.close()
            throw error
        }

        val ioScope = CoroutineScope(ioDispatcher + SupervisorJob())
        activeConnection = ActiveConnection(transport = transport, ioScope = ioScope)

        ioScope.launch {
            try {
                readLoop(transport.readChannel)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                notifyConnectionLost(error)
            }
        }

        if (configuration.keepAliveInterval > 0) {
            ioScope.launch {
                try {
                    keepAliveLoop(transport.writeChannel)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    notifyConnectionLost(error)
                }
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
        awaitAck(
            ackKey = AckKey(AckType.SubAck, packetId),
            timeoutMs = ackTimeoutMs(),
        ) {
            sendSubscribe(connection.transport.writeChannel, topic, qos, packetId)
        }
    }

    private suspend fun sendUnsubscribeAndAwait(topic: String) {
        val connection = activeConnection ?: error("Not connected")
        val packetId = nextPacketId()
        awaitAck(
            ackKey = AckKey(AckType.UnsubAck, packetId),
            timeoutMs = ackTimeoutMs(),
        ) {
            sendUnsubscribe(connection.transport.writeChannel, topic, packetId)
        }
    }

    private suspend fun keepAliveLoop(writeChannel: ByteWriteChannel) {
        val intervalMs = configuration.keepAliveInterval.coerceAtLeast(1L) * 1_000L
        while (true) {
            delay(intervalMs)
            awaitAck(
                ackKey = AckKey(AckType.PingResp, packetId = 0),
                timeoutMs = ackTimeoutMs(),
            ) {
                sendPingReq(writeChannel)
            }
        }
    }

    private suspend fun readLoop(readChannel: ByteReadChannel) {
        while (true) {
            val (firstByte, payload) = readPacket(readChannel)
            val packetType = firstByte.toInt() and 0xFF
            when {
                packetType and 0xF0 == MQTT_PUBLISH_TYPE -> handleIncomingPublish(packetType, payload)
                packetType == MQTT_PUBACK -> completeAck(AckKey(AckType.PubAck, readPacketId(payload)), payload)
                packetType == MQTT_PUBREC -> completeAck(AckKey(AckType.PubRec, readPacketId(payload)), payload)
                packetType == MQTT_PUBCOMP -> completeAck(AckKey(AckType.PubComp, readPacketId(payload)), payload)
                packetType == MQTT_SUBACK -> {
                    validateSubAck(payload)
                    completeAck(AckKey(AckType.SubAck, readPacketId(payload)), payload)
                }
                packetType == MQTT_UNSUBACK -> completeAck(AckKey(AckType.UnsubAck, readPacketId(payload)), payload)
                packetType and 0xF0 == 0x60 -> handleIncomingPubRel(payload)
                packetType == MQTT_PINGRESP -> completeAck(AckKey(AckType.PingResp, 0), payload)
                packetType == MQTT_DISCONNECT -> throw IllegalStateException("Broker sent DISCONNECT")
                else -> {
                    // Ignore unsupported packet types for now.
                }
            }
        }
    }

    private suspend fun completeAck(ackKey: AckKey, payload: ByteArray) {
        val deferred = acksMutex.withLock { pendingAcks.remove(ackKey) }
        deferred?.complete(payload)
    }

    private suspend fun handleIncomingPublish(packetType: Int, payload: ByteArray) {
        val qosBits = (packetType shr 1) and 0x03
        val retain = (packetType and 0x01) != 0
        val qos = MqttQoS.from(qosBits)

        var offset = 0
        val topicLength = readUInt16(payload, offset)
        offset += 2
        check(offset + topicLength <= payload.size) { "Malformed PUBLISH packet topic field" }
        val topic = payload.decodeToString(offset, offset + topicLength)
        offset += topicLength

        val packetId = if (qos != MqttQoS.AtMostOnce) {
            val id = readUInt16(payload, offset)
            offset += 2
            id
        } else {
            0
        }

        check(offset <= payload.size) { "Malformed PUBLISH payload" }
        val messagePayload = payload.copyOfRange(offset, payload.size)
        val mqttMessage = MqttMessage(topic, messagePayload, qos, retain)

        when (qos) {
            MqttQoS.AtMostOnce -> incomingMessages.emit(mqttMessage)
            MqttQoS.AtLeastOnce -> {
                incomingMessages.emit(mqttMessage)
                activeConnection?.let { sendPubAck(it.transport.writeChannel, packetId) }
            }
            MqttQoS.ExactlyOnce -> {
                acksMutex.withLock { pendingQoS2Messages[packetId] = mqttMessage }
                activeConnection?.let { sendPubRec(it.transport.writeChannel, packetId) }
            }
        }
    }

    private suspend fun handleIncomingPubRel(payload: ByteArray) {
        val packetId = readPacketId(payload)
        val message = acksMutex.withLock { pendingQoS2Messages.remove(packetId) }
        if (message != null) {
            incomingMessages.emit(message)
            activeConnection?.let { sendPubComp(it.transport.writeChannel, packetId) }
        }
    }

    private suspend fun awaitAck(
        ackKey: AckKey,
        timeoutMs: Long,
        onSend: suspend () -> Unit,
    ) {
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

        runCatching { connection.transport.close() }
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
        packetIdCounter = (packetIdCounter % 0xFFFF) + 1
        return packetIdCounter
    }

    private suspend fun <T> submitCommand(builder: (CompletableDeferred<T>) -> ClientCommand): T {
        val result = CompletableDeferred<T>()
        commandChannel.send(builder(result))
        return result.await()
    }

    private suspend fun <T> complete(result: CompletableDeferred<T>, block: suspend () -> T) {
        try {
            result.complete(block())
        } catch (error: Throwable) {
            result.completeExceptionally(error)
        }
    }

    private fun connectTimeoutMs(): Long = configuration.connectionTimeout.coerceAtLeast(1L) * 1_000L

    private fun ackTimeoutMs(): Long = timing.ackTimeoutMs.coerceAtLeast(1L)

    private suspend fun <T> withIoTimeout(timeoutMs: Long, block: suspend () -> T): T =
        withContext(ioDispatcher) {
            withTimeout(timeoutMs) {
                block()
            }
        }

    // ---- MQTT packet reading ----

    private suspend fun ByteReadChannel.readOneByte(): Byte {
        val buf = ByteArray(1)
        readFully(buf, 0, 1)
        return buf[0]
    }

    private suspend fun readPacket(readChannel: ByteReadChannel): Pair<Byte, ByteArray> {
        val firstByte = readChannel.readOneByte()
        val remainingLength = readVariableLength(readChannel)
        val payload = ByteArray(remainingLength)
        if (remainingLength > 0) {
            readChannel.readFully(payload, 0, remainingLength)
        }
        return firstByte to payload
    }

    private suspend fun readVariableLength(readChannel: ByteReadChannel): Int {
        var multiplier = 1
        var value = 0
        var count = 0

        do {
            val byte = readChannel.readOneByte().toInt() and 0xFF
            value += (byte and 0x7F) * multiplier
            check(value <= MAX_REMAINING_LENGTH) { "MQTT remaining length exceeds maximum" }
            multiplier *= 128
            count += 1
            check(count <= MAX_REMAINING_LENGTH_BYTES) { "Malformed MQTT remaining length" }
        } while (byte and 0x80 != 0)

        return value
    }

    private suspend fun receiveConnAck(readChannel: ByteReadChannel) {
        val (firstByte, payload) = readPacket(readChannel)
        if (firstByte.toInt() and 0xFF != MQTT_CONNACK) {
            error("Expected CONNACK but received packet type ${firstByte.toInt() and 0xFF}")
        }
        check(payload.size >= 2) { "CONNACK packet is too short" }
        val returnCode = payload[1].toInt() and 0xFF
        if (returnCode != CONNACK_ACCEPTED) {
            throw IllegalStateException(
                "MQTT connection refused: " + when (returnCode) {
                    CONNACK_UNACCEPTABLE_PROTOCOL -> "Unacceptable protocol version"
                    CONNACK_IDENTIFIER_REJECTED -> "Client identifier rejected"
                    CONNACK_SERVER_UNAVAILABLE -> "Server unavailable"
                    CONNACK_BAD_CREDENTIALS -> "Bad username or password"
                    CONNACK_NOT_AUTHORIZED -> "Not authorized"
                    else -> "Unknown return code $returnCode"
                },
            )
        }
    }

    private fun readPacketId(payload: ByteArray): Int {
        check(payload.size >= 2) { "ACK packet is too short" }
        return readUInt16(payload, 0)
    }

    private fun validateSubAck(payload: ByteArray) {
        check(payload.size >= 3) { "SUBACK packet is too short" }
        val granted = payload.copyOfRange(2, payload.size)
        check(granted.none { (it.toInt() and 0xFF) == 0x80 }) { "Subscription rejected by broker" }
    }

    // ---- MQTT packet writing ----

    private suspend fun sendPacket(writeChannel: ByteWriteChannel, fixedHeader: Int, payload: ByteArray) {
        val packet = buildFullPacket(fixedHeader, payload)
        withContext(ioDispatcher) {
            writeMutex.withLock {
                writeChannel.writeFully(packet)
                writeChannel.flush()
            }
        }
    }

    private fun buildFullPacket(fixedHeader: Int, payload: ByteArray): ByteArray {
        val packet = MqttBuffer()
        packet.writeByte(fixedHeader)
        var remaining = payload.size
        do {
            var digit = remaining % 128
            remaining /= 128
            if (remaining > 0) {
                digit = digit or 0x80
            }
            packet.writeByte(digit)
        } while (remaining > 0)
        packet.writeBytes(payload)
        return packet.toByteArray()
    }

    private suspend fun sendConnect(writeChannel: ByteWriteChannel) {
        val buf = MqttBuffer()

        // Variable header
        buf.writeUtf8String(MQTT_PROTOCOL_NAME)
        buf.writeByte(MQTT_PROTOCOL_LEVEL)

        // Connect flags
        var flags = 0
        if (configuration.cleanSession) {
            flags = flags or 0x02
        }
        configuration.will?.let { will ->
            flags = flags or 0x04
            flags = flags or (will.qos.code shl 3)
            if (will.retained) {
                flags = flags or 0x20
            }
        }
        if (configuration.username != null) {
            flags = flags or 0x80
        }
        if (configuration.password != null && configuration.username != null) {
            flags = flags or 0x40
        }
        buf.writeByte(flags)
        buf.writeUInt16(configuration.keepAliveInterval.toInt())

        // Payload
        buf.writeUtf8String(configuration.clientId)
        configuration.will?.let { will ->
            buf.writeUtf8String(will.topic)
            buf.writeBinaryData(will.message)
        }
        configuration.username?.let { buf.writeUtf8String(it) }
        configuration.password?.let { buf.writeBinaryData(it.encodeToByteArray()) }

        sendPacket(writeChannel, MQTT_CONNECT, buf.toByteArray())
    }

    private suspend fun sendPublish(
        writeChannel: ByteWriteChannel,
        topic: String,
        payload: ByteArray,
        qos: MqttQoS,
        packetId: Int,
    ) {
        val buf = MqttBuffer()
        buf.writeUtf8String(topic)
        if (qos != MqttQoS.AtMostOnce) {
            buf.writeUInt16(packetId)
        }
        buf.writeBytes(payload)

        val fixedHeader = MQTT_PUBLISH_TYPE or (qos.code shl 1)
        sendPacket(writeChannel, fixedHeader, buf.toByteArray())
    }

    private suspend fun sendPubAck(writeChannel: ByteWriteChannel, packetId: Int) =
        sendPacket(writeChannel, MQTT_PUBACK, uInt16ToBytes(packetId))

    private suspend fun sendPubRec(writeChannel: ByteWriteChannel, packetId: Int) =
        sendPacket(writeChannel, MQTT_PUBREC, uInt16ToBytes(packetId))

    private suspend fun sendPubRel(writeChannel: ByteWriteChannel, packetId: Int) =
        sendPacket(writeChannel, MQTT_PUBREL, uInt16ToBytes(packetId))

    private suspend fun sendPubComp(writeChannel: ByteWriteChannel, packetId: Int) =
        sendPacket(writeChannel, MQTT_PUBCOMP, uInt16ToBytes(packetId))

    private suspend fun sendSubscribe(
        writeChannel: ByteWriteChannel,
        topic: String,
        qos: MqttQoS,
        packetId: Int,
    ) {
        val buf = MqttBuffer()
        buf.writeUInt16(packetId)
        buf.writeUtf8String(topic)
        buf.writeByte(qos.code)
        sendPacket(writeChannel, MQTT_SUBSCRIBE, buf.toByteArray())
    }

    private suspend fun sendUnsubscribe(writeChannel: ByteWriteChannel, topic: String, packetId: Int) {
        val buf = MqttBuffer()
        buf.writeUInt16(packetId)
        buf.writeUtf8String(topic)
        sendPacket(writeChannel, MQTT_UNSUBSCRIBE, buf.toByteArray())
    }

    private suspend fun sendPingReq(writeChannel: ByteWriteChannel) =
        sendPacket(writeChannel, MQTT_PINGREQ, ByteArray(0))

    private suspend fun sendDisconnect(writeChannel: ByteWriteChannel) =
        sendPacket(writeChannel, MQTT_DISCONNECT, ByteArray(0))

    // ---- Topic filter matching ----

    private fun matchesTopicFilter(topic: String, filter: String): Boolean {
        val topicParts = topic.split("/")
        val filterParts = filter.split("/")

        fun match(topicIndex: Int, filterIndex: Int): Boolean {
            if (filterIndex == filterParts.size) {
                return topicIndex == topicParts.size
            }
            if (filterParts[filterIndex] == "#") {
                return true
            }
            if (topicIndex == topicParts.size) {
                return false
            }
            if (filterParts[filterIndex] != "+" && filterParts[filterIndex] != topicParts[topicIndex]) {
                return false
            }
            return match(topicIndex + 1, filterIndex + 1)
        }

        return match(0, 0)
    }

    // ---- Byte utilities ----

    private fun readUInt16(data: ByteArray, offset: Int): Int {
        check(offset + 1 < data.size) { "Malformed MQTT packet: expected UInt16 at offset $offset" }
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    private fun uInt16ToBytes(value: Int): ByteArray =
        byteArrayOf((value shr 8 and 0xFF).toByte(), (value and 0xFF).toByte())

    private sealed class ConnectionCloseCause {
        data object Expected : ConnectionCloseCause()

        data class Unexpected(val error: Throwable) : ConnectionCloseCause()
    }
}

/**
 * Minimal mutable byte buffer for constructing MQTT packet payloads.
 */
private class MqttBuffer {
    private val buffer = mutableListOf<Byte>()

    fun writeByte(value: Int) {
        buffer.add((value and 0xFF).toByte())
    }

    fun writeUInt16(value: Int) {
        writeByte(value shr 8)
        writeByte(value)
    }

    fun writeBytes(bytes: ByteArray) {
        buffer.addAll(bytes.toList())
    }

    fun writeUtf8String(value: String) {
        val bytes = value.encodeToByteArray()
        writeUInt16(bytes.size)
        writeBytes(bytes)
    }

    fun writeBinaryData(bytes: ByteArray) {
        writeUInt16(bytes.size)
        writeBytes(bytes)
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()
}
