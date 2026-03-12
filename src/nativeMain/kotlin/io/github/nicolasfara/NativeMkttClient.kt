package io.github.nicolasfara

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.network.tls.tls
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readFully
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

/**
 * A native implementation of [MkttClient] using ktor-network for TCP/TLS transport
 * and the MQTT 3.1.1 protocol for message exchange.
 */
@Suppress("TooManyFunctions")
internal class NativeMkttClient(
    override val dispatcher: CoroutineDispatcher,
    private val configuration: MqttClientConfiguration,
) : MkttClient {

    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)
    override val connectionState: StateFlow<MqttConnectionState> = _connectionState.asStateFlow()

    private var selectorManager: SelectorManager? = null
    private var openSocket: Socket? = null
    private var writeChannel: ByteWriteChannel? = null
    private val writeMutex = Mutex()

    private val incomingMessages = MutableSharedFlow<MqttMessage>(extraBufferCapacity = 1000)
    private val subscribedTopics = mutableMapOf<String, Flow<MqttMessage>>()

    private var packetIdCounter = 0
    private val pendingAcks = mutableMapOf<Int, CompletableDeferred<ByteArray>>()
    private val pendingQos2Receive = mutableMapOf<Int, MqttMessage>()
    private val acksMutex = Mutex()

    private var ioScope: CoroutineScope? = null

    private fun nextPacketId(): Int {
        packetIdCounter = (packetIdCounter % 65535) + 1
        return packetIdCounter
    }

    override suspend fun connect(): Unit = withContext(dispatcher) {
        check(_connectionState.value is MqttConnectionState.Disconnected) {
            "Client is already connected or connecting"
        }
        _connectionState.value = MqttConnectionState.Connecting

        val sm = SelectorManager(dispatcher)
        selectorManager = sm

        try {
            val rawSocket = aSocket(sm).tcp().connect(
                hostname = configuration.brokerUrl,
                port = configuration.port,
            )

            val connectedSocket = if (configuration.ssl) {
                rawSocket.tls(coroutineContext)
            } else {
                rawSocket
            }

            openSocket = connectedSocket
            val rc = connectedSocket.openReadChannel()
            val wc = connectedSocket.openWriteChannel(autoFlush = false)
            writeChannel = wc

            // Send CONNECT packet and wait for CONNACK
            sendConnect(wc)
            receiveConnAck(rc)

            _connectionState.value = MqttConnectionState.Connected

            val scope = CoroutineScope(dispatcher + SupervisorJob())
            ioScope = scope

            // Start background read loop
            scope.launch {
                try {
                    readLoop(rc)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (_connectionState.value is MqttConnectionState.Connected) {
                        _connectionState.value = if (configuration.automaticReconnect) {
                            MqttConnectionState.Connecting
                        } else {
                            MqttConnectionState.Disconnected
                        }
                    }
                }
            }

            // Start keep-alive ping loop
            if (configuration.keepAliveInterval > 0) {
                scope.launch {
                    try {
                        keepAliveLoop()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        // Keep-alive failures are non-fatal; the read loop will detect disconnection
                    }
                }
            }
        } catch (e: Exception) {
            _connectionState.value = MqttConnectionState.ConnectionError(e)
            closeResources()
            throw e
        }
    }

    override suspend fun disconnect(): Unit = withContext(dispatcher) {
        check(_connectionState.value is MqttConnectionState.Connected) {
            "Client is not connected"
        }
        try {
            writeChannel?.let { sendDisconnect(it) }
        } catch (_: Exception) {
            // Ignore send errors during intentional disconnect
        }
        closeResources()
        _connectionState.value = MqttConnectionState.Disconnected
    }

    override suspend fun publish(topic: String, message: ByteArray, qos: MqttQoS): Unit = withContext(dispatcher) {
        val wc = writeChannel ?: error("Not connected")
        when (qos) {
            MqttQoS.AtMostOnce -> {
                sendPublish(wc, topic, message, qos, packetId = 0)
            }

            MqttQoS.AtLeastOnce -> {
                val pid = nextPacketId()
                val deferred = CompletableDeferred<ByteArray>()
                acksMutex.withLock { pendingAcks[pid] = deferred }
                try {
                    sendPublish(wc, topic, message, qos, packetId = pid)
                    deferred.await()
                } finally {
                    acksMutex.withLock { pendingAcks.remove(pid) }
                }
            }

            MqttQoS.ExactlyOnce -> {
                val pid = nextPacketId()
                // Phase 1: PUBLISH → wait for PUBREC
                val pubrecDeferred = CompletableDeferred<ByteArray>()
                acksMutex.withLock { pendingAcks[pid] = pubrecDeferred }
                sendPublish(wc, topic, message, qos, packetId = pid)
                pubrecDeferred.await()
                acksMutex.withLock { pendingAcks.remove(pid) }
                // Phase 2: PUBREL → wait for PUBCOMP
                val pubcompDeferred = CompletableDeferred<ByteArray>()
                acksMutex.withLock { pendingAcks[pid] = pubcompDeferred }
                try {
                    sendPubRel(wc, pid)
                    pubcompDeferred.await()
                } finally {
                    acksMutex.withLock { pendingAcks.remove(pid) }
                }
            }
        }
    }

    override fun subscribe(topic: String, qos: MqttQoS): Flow<MqttMessage> = subscribedTopics.getOrPut(topic) {
        flow {
            val wc = writeChannel ?: error("Not connected")
            val pid = nextPacketId()
            val deferred = CompletableDeferred<ByteArray>()
            acksMutex.withLock { pendingAcks[pid] = deferred }
            try {
                sendSubscribe(wc, topic, qos, pid)
                deferred.await()
            } finally {
                acksMutex.withLock { pendingAcks.remove(pid) }
            }
            emitAll(incomingMessages.filter { matchesTopicFilter(it.topic, topic) })
        }.flowOn(dispatcher)
    }

    override suspend fun unsubscribe(topic: String): Unit = withContext(dispatcher) {
        val wc = writeChannel ?: error("Not connected")
        val pid = nextPacketId()
        val deferred = CompletableDeferred<ByteArray>()
        acksMutex.withLock { pendingAcks[pid] = deferred }
        try {
            sendUnsubscribe(wc, topic, pid)
            deferred.await()
        } finally {
            acksMutex.withLock { pendingAcks.remove(pid) }
        }
        subscribedTopics.remove(topic) ?: error("Topic not subscribed: $topic")
    }

    // ---- Internal I/O loop ----

    private suspend fun readLoop(rc: ByteReadChannel) {
        while (true) {
            val (firstByte, payload) = readPacket(rc)
            val packetType = firstByte.toInt() and 0xFF
            when {
                packetType and 0xF0 == MQTT_PUBLISH_TYPE ->
                    handleIncomingPublish(packetType, payload)

                packetType == MQTT_PUBACK ||
                    packetType == MQTT_PUBREC ||
                    packetType == MQTT_PUBCOMP ||
                    packetType == MQTT_SUBACK ||
                    packetType == MQTT_UNSUBACK ->
                    handleAck(payload)

                // PUBREL from broker (QoS 2 receive, phase 2): type bits = 0x60, flags = 0x02
                packetType and 0xF0 == 0x60 ->
                    handlePubRel(payload)

                packetType == MQTT_PINGRESP -> { /* Pong received, nothing to do */ }

                packetType == MQTT_DISCONNECT -> throw Exception("Broker sent DISCONNECT")
            }
        }
    }

    private suspend fun handleIncomingPublish(packetType: Int, payload: ByteArray) {
        val qosBits = (packetType shr 1) and 0x03
        val retain = (packetType and 0x01) != 0
        val qos = MqttQoS.from(qosBits)

        var offset = 0
        val topicLength = readUInt16(payload, offset)
        offset += 2
        val topic = payload.decodeToString(offset, offset + topicLength)
        offset += topicLength

        val packetId = if (qos != MqttQoS.AtMostOnce) {
            val pid = readUInt16(payload, offset)
            offset += 2
            pid
        } else {
            0
        }

        val msgPayload = payload.copyOfRange(offset, payload.size)
        val mqttMessage = MqttMessage(topic, msgPayload, qos, retain)

        when (qos) {
            MqttQoS.AtMostOnce -> incomingMessages.emit(mqttMessage)
            MqttQoS.AtLeastOnce -> {
                incomingMessages.emit(mqttMessage)
                writeChannel?.let { sendPubAck(it, packetId) }
            }
            MqttQoS.ExactlyOnce -> {
                // Store the message until PUBREL is received, then deliver
                acksMutex.withLock { pendingQos2Receive[packetId] = mqttMessage }
                writeChannel?.let { sendPubRec(it, packetId) }
            }
        }
    }

    private suspend fun handleAck(payload: ByteArray) {
        if (payload.size < 2) return
        val packetId = readUInt16(payload, 0)
        val deferred = acksMutex.withLock { pendingAcks[packetId] }
        deferred?.complete(payload)
    }

    private suspend fun handlePubRel(payload: ByteArray) {
        if (payload.size < 2) return
        val packetId = readUInt16(payload, 0)
        val msg = acksMutex.withLock { pendingQos2Receive.remove(packetId) }
        if (msg != null) {
            incomingMessages.emit(msg)
            writeChannel?.let { sendPubComp(it, packetId) }
        }
    }

    private suspend fun keepAliveLoop() {
        while (true) {
            delay(configuration.keepAliveInterval * 1_000)
            writeChannel?.let { sendPingReq(it) } ?: break
        }
    }

    private fun closeResources() {
        ioScope?.cancel()
        ioScope = null
        // Fail all waiting coroutines so they don't hang
        pendingAcks.values.forEach {
            it.completeExceptionally(Exception("Connection closed"))
        }
        pendingAcks.clear()
        pendingQos2Receive.clear()
        subscribedTopics.clear()
        openSocket?.close()
        openSocket = null
        writeChannel = null
        selectorManager?.close()
        selectorManager = null
    }

    // ---- MQTT packet reading ----

    private suspend fun readPacket(rc: ByteReadChannel): Pair<Byte, ByteArray> {
        val firstByte = rc.readByte()
        val remainingLength = readVariableLength(rc)
        val payload = ByteArray(remainingLength)
        if (remainingLength > 0) {
            rc.readFully(payload, 0, remainingLength)
        }
        return Pair(firstByte, payload)
    }

    private suspend fun readVariableLength(rc: ByteReadChannel): Int {
        var multiplier = 1
        var value = 0
        do {
            val byte = rc.readByte().toInt() and 0xFF
            value += (byte and 0x7F) * multiplier
            multiplier *= 128
            if (multiplier > 128 * 128 * 128) error("Malformed remaining length in MQTT packet")
        } while (byte and 0x80 != 0)
        return value
    }

    private suspend fun receiveConnAck(rc: ByteReadChannel) {
        val (firstByte, payload) = readPacket(rc)
        if (firstByte.toInt() and 0xFF != MQTT_CONNACK) {
            error("Expected CONNACK but received packet type ${firstByte.toInt() and 0xFF}")
        }
        if (payload.size < 2) error("CONNACK packet is too short")
        val returnCode = payload[1].toInt() and 0xFF
        if (returnCode != CONNACK_ACCEPTED) {
            throw Exception(
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

    // ---- MQTT packet writing ----

    private suspend fun sendPacket(wc: ByteWriteChannel, fixedHeader: Int, payload: ByteArray) {
        writeMutex.withLock {
            wc.writeByte(fixedHeader.toByte())
            writeVariableLength(wc, payload.size)
            if (payload.isNotEmpty()) {
                wc.writeFully(payload, 0, payload.size)
            }
            wc.flush()
        }
    }

    private suspend fun writeVariableLength(wc: ByteWriteChannel, value: Int) {
        var remaining = value
        do {
            var digit = remaining % 128
            remaining /= 128
            if (remaining > 0) digit = digit or 0x80
            wc.writeByte(digit.toByte())
        } while (remaining > 0)
    }

    private suspend fun sendConnect(wc: ByteWriteChannel) {
        val buf = MqttBuffer()

        // Variable header
        buf.writeUtf8String(MQTT_PROTOCOL_NAME)
        buf.writeByte(MQTT_PROTOCOL_LEVEL)

        // Connect flags
        var flags = 0
        if (configuration.cleanSession) flags = flags or 0x02
        configuration.will?.let { will ->
            flags = flags or 0x04
            flags = flags or (will.qos.code shl 3)
            if (will.retained) flags = flags or 0x20
        }
        if (configuration.username != null) flags = flags or 0x80
        if (configuration.password != null && configuration.username != null) flags = flags or 0x40
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

        sendPacket(wc, MQTT_CONNECT, buf.toByteArray())
    }

    private suspend fun sendPublish(
        wc: ByteWriteChannel,
        topic: String,
        payload: ByteArray,
        qos: MqttQoS,
        packetId: Int,
    ) {
        val buf = MqttBuffer()
        buf.writeUtf8String(topic)
        if (qos != MqttQoS.AtMostOnce) buf.writeUInt16(packetId)
        buf.writeBytes(payload)
        val fixedHeader = MQTT_PUBLISH_TYPE or (qos.code shl 1)
        sendPacket(wc, fixedHeader, buf.toByteArray())
    }

    private suspend fun sendPubAck(wc: ByteWriteChannel, packetId: Int) =
        sendPacket(wc, MQTT_PUBACK, uInt16ToBytes(packetId))

    private suspend fun sendPubRec(wc: ByteWriteChannel, packetId: Int) =
        sendPacket(wc, MQTT_PUBREC, uInt16ToBytes(packetId))

    private suspend fun sendPubRel(wc: ByteWriteChannel, packetId: Int) =
        sendPacket(wc, MQTT_PUBREL, uInt16ToBytes(packetId))

    private suspend fun sendPubComp(wc: ByteWriteChannel, packetId: Int) =
        sendPacket(wc, MQTT_PUBCOMP, uInt16ToBytes(packetId))

    private suspend fun sendSubscribe(wc: ByteWriteChannel, topic: String, qos: MqttQoS, packetId: Int) {
        val buf = MqttBuffer()
        buf.writeUInt16(packetId)
        buf.writeUtf8String(topic)
        buf.writeByte(qos.code)
        sendPacket(wc, MQTT_SUBSCRIBE, buf.toByteArray())
    }

    private suspend fun sendUnsubscribe(wc: ByteWriteChannel, topic: String, packetId: Int) {
        val buf = MqttBuffer()
        buf.writeUInt16(packetId)
        buf.writeUtf8String(topic)
        sendPacket(wc, MQTT_UNSUBSCRIBE, buf.toByteArray())
    }

    private suspend fun sendPingReq(wc: ByteWriteChannel) =
        sendPacket(wc, MQTT_PINGREQ, ByteArray(0))

    private suspend fun sendDisconnect(wc: ByteWriteChannel) =
        sendPacket(wc, MQTT_DISCONNECT, ByteArray(0))

    // ---- Topic filter matching ----

    /**
     * Checks whether [topic] matches the given MQTT [filter].
     * `+` matches a single topic level; `#` matches any remaining levels.
     */
    private fun matchesTopicFilter(topic: String, filter: String): Boolean {
        val topicParts = topic.split("/")
        val filterParts = filter.split("/")

        fun match(ti: Int, fi: Int): Boolean {
            if (fi == filterParts.size) return ti == topicParts.size
            if (filterParts[fi] == "#") return true
            if (ti == topicParts.size) return false
            if (filterParts[fi] != "+" && filterParts[fi] != topicParts[ti]) return false
            return match(ti + 1, fi + 1)
        }

        return match(0, 0)
    }

    // ---- Byte utilities ----

    private fun readUInt16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun uInt16ToBytes(value: Int): ByteArray =
        byteArrayOf((value shr 8 and 0xFF).toByte(), (value and 0xFF).toByte())
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

    /** Writes a UTF-8 string prefixed with its 2-byte length (MQTT string encoding). */
    fun writeUtf8String(s: String) {
        val bytes = s.encodeToByteArray()
        writeUInt16(bytes.size)
        writeBytes(bytes)
    }

    /** Writes a binary blob prefixed with its 2-byte length (MQTT binary-data encoding). */
    fun writeBinaryData(bytes: ByteArray) {
        writeUInt16(bytes.size)
        writeBytes(bytes)
    }

    fun toByteArray(): ByteArray = buffer.toByteArray()
}
