package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.ConnectionException
import io.github.nicolasfara.mktt.core.HandshakeFailedException
import io.github.nicolasfara.mktt.core.InFlightPublish
import io.github.nicolasfara.mktt.core.InFlightPubrel
import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.github.nicolasfara.mktt.core.MqttException
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.SessionStore
import io.github.nicolasfara.mktt.core.SubscriptionIdentifier
import io.github.nicolasfara.mktt.core.TimeoutException
import io.github.nicolasfara.mktt.core.TopicAliasMaximum
import io.github.nicolasfara.mktt.core.TopicFilter
import io.github.nicolasfara.mktt.core.UnspecifiedError
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.hasSharedTopic
import io.github.nicolasfara.mktt.core.hasWildcard
import io.github.nicolasfara.mktt.core.ifNull
import io.github.nicolasfara.mktt.core.packet.Connack
import io.github.nicolasfara.mktt.core.packet.Disconnect
import io.github.nicolasfara.mktt.core.packet.Packet
import io.github.nicolasfara.mktt.core.packet.PacketType
import io.github.nicolasfara.mktt.core.packet.Pingreq
import io.github.nicolasfara.mktt.core.packet.Pingresp
import io.github.nicolasfara.mktt.core.packet.Puback
import io.github.nicolasfara.mktt.core.packet.Pubcomp
import io.github.nicolasfara.mktt.core.packet.Publish
import io.github.nicolasfara.mktt.core.packet.Pubrec
import io.github.nicolasfara.mktt.core.packet.Pubrel
import io.github.nicolasfara.mktt.core.packet.Suback
import io.github.nicolasfara.mktt.core.packet.Unsuback
import io.github.nicolasfara.mktt.core.packet.isResponseFor
import io.github.nicolasfara.mktt.core.util.Logger
import io.github.nicolasfara.mktt.engine.MqttEngine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Default [MqttClient] implementation backed by a [MqttEngine] and a [SessionStore].
 */
class DefaultMqttClient(
    private val config: MqttClientConfig,
    private val engine: MqttEngine,
    private val session: SessionStore,
) : MqttClient {

    constructor(config: MqttClientConfig) : this(config, config.engine, config.sessionStoreProvider())

    private val _incomingPublishes = MutableSharedFlow<MqttPublishMessage>()
    private val _connectionState = MutableStateFlow<MqttConnectionState>(MqttConnectionState.Disconnected)

    override val incomingPublishes: SharedFlow<MqttPublishMessage>
        get() = _incomingPublishes.asSharedFlow()

    override val maxQos: QoS
        get() = capabilities.maxQos

    override val clientId: String
        get() = capabilities.clientId

    override val serverTopicAliasMaximum: TopicAliasMaximum
        get() = capabilities.serverTopicAliasMaximum

    override val subscriptionIdentifierAvailable: Boolean
        get() = capabilities.subscriptionIdentifierAvailable

    override val receiveMaximum: UShort
        get() = capabilities.receiveMaximum

    override val isRetainAvailable: Boolean
        get() = capabilities.retainAvailable

    override val isWildcardSubscriptionAvailable: Boolean
        get() = capabilities.wildcardSubscriptionAvailable

    override val isSharedSubscriptionAvailable: Boolean
        get() = capabilities.sharedSubscriptionAvailable

    override val maxPacketSize: UInt
        get() = capabilities.maxPacketSize

    override val connectionState: StateFlow<MqttConnectionState>
        get() = _connectionState.asStateFlow()

    private val scope = CoroutineScope(engine.dispatcher)
    private val capabilities = DefaultMqttClientCapabilities(config.clientId)
    private val pendingResponses = PendingResponseRegistry(config.ackMessageTimeout)

    private val packetIdentifierMutex = Mutex()
    private val allocatedPacketIdentifiers = mutableSetOf<UShort>()
    private var packetIdentifier = 0
    private val sendQuota = SendQuotaController()

    private val publishReceivedPackets = mutableMapOf<UShort, Pubrec>()
    private var keepAliveJob: kotlinx.coroutines.Job? = null

    init {
        scope.launch {
            engine.packetResults.collect { result -> handlePacketResult(result) }
        }
        scope.launch {
            engine.connected.collect { connected ->
                if (!connected && _connectionState.value != MqttConnectionState.Connecting) {
                    pendingResponses.reset()
                    sendQuota.reset()
                    restorePacketIdentifiersFromSession()
                    _connectionState.value = MqttConnectionState.Disconnected
                }
            }
        }
    }

    override suspend fun connect(cleanStart: Boolean): ConnAck {
        _connectionState.value = MqttConnectionState.Connecting
        pendingResponses.reset()
        sendQuota.reset()
        if (cleanStart) {
            clearSessionState()
        } else {
            restorePacketIdentifiersFromSession()
        }
        try {
            engine.start().fold(
                onSuccess = {},
                onFailure = { throw it },
            )
            val connack = awaitResponseOf<Connack>({ packet -> packet.type == PacketType.CONNACK }) {
                engine.send(DefaultMqttClientPackets.createConnect(config, cleanStart))
            }.fold(
                onSuccess = { it },
                onFailure = { throw it },
            )
            inspectConnack(connack)
            if (connack.isSessionPresent) {
                restorePacketIdentifiersFromSession()
                resumeSession()
            } else {
                clearSessionState()
            }
            return connack
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: MqttException) {
            _connectionState.value = MqttConnectionState.ConnectionError(ex)
            throw ex
        }
    }

    override suspend fun subscribe(
        filters: List<TopicFilter>,
        subscriptionIdentifier: SubscriptionIdentifier?,
        userProperties: UserProperties,
    ): SubAck {
        if (!isWildcardSubscriptionAvailable && filters.hasWildcard()) {
            Logger.w {
                "Requesting at least one wildcard subscription ($filters), but the server does not support it. " +
                    "This will likely result in a DISCONNECT message from the server."
            }
        }
        if (!isSharedSubscriptionAvailable && filters.hasSharedTopic()) {
            Logger.w {
                "Requesting at least one shared subscription ($filters), but the server does not support it. " +
                    "This will likely result in a DISCONNECT message from the server."
            }
        }
        val identifier = if ((subscriptionIdentifier != null) && !subscriptionIdentifierAvailable) {
            Logger.w(IllegalArgumentException("Ignoring $subscriptionIdentifier")) {
                "Ignoring subscription identifier, as the server doesn't support it"
            }
            null
        } else {
            subscriptionIdentifier
        }
        val packetIdentifier = nextPacketIdentifier()
        val subscribe = runCatching {
            DefaultMqttClientPackets.createSubscribe(
                filters = filters,
                subscriptionIdentifier = identifier,
                userProperties = userProperties,
                packetIdentifier = packetIdentifier,
            )
        }.getOrElse {
            releasePacketIdentifier(packetIdentifier)
            throw it
        }
        return awaitResponseOf<Suback>({ it.isResponseFor<Suback>(subscribe) }, {
            engine.send(subscribe)
        }).getOrElse {
            disconnectAfterHandshakeFailure()
            throw it
        }.also {
            releasePacketIdentifier(subscribe.packetIdentifier)
        }
    }

    override suspend fun unsubscribe(filters: List<TopicFilter>, userProperties: UserProperties): UnsubAck {
        val packetIdentifier = nextPacketIdentifier()
        val unsubscribe = runCatching {
            DefaultMqttClientPackets.createUnsubscribe(
                topics = filters.map(TopicFilter::filter),
                userProperties = userProperties,
                packetIdentifier = packetIdentifier,
            )
        }.getOrElse {
            releasePacketIdentifier(packetIdentifier)
            throw it
        }

        return awaitResponseOf<Unsuback>({
            it.isResponseFor<Unsuback>(unsubscribe)
        }, {
            engine.send(unsubscribe)
        }).getOrElse {
            disconnectAfterHandshakeFailure()
            throw it
        }.also {
            releasePacketIdentifier(unsubscribe.packetIdentifier)
        }
    }

    override suspend fun publish(request: PublishRequest): PublishResult {
        if (!engine.connected.value) {
            throw ConnectionException("Cannot send PUBLISH packet while not connected")
        }
        val packetIdentifier = if (request.desiredQoS.coerceAtMost(maxQos) == QoS.AT_MOST_ONCE) {
            null
        } else {
            nextPacketIdentifier()
        }
        return DefaultMqttClientPackets.createPublish(
            request = request,
            capabilities = capabilities,
            packetIdentifier = packetIdentifier,
        ).onFailure {
            releasePacketIdentifier(packetIdentifier)
        }.mapCatching { publish ->
            var sessionOwnsPacketIdentifier = false
            try {
                when (publish.qoS) {
                    QoS.AT_MOST_ONCE -> {
                        engine.send(publish).onFailure { throw it }
                        AtMostOncePublishResponse(publish)
                    }

                    QoS.AT_LEAST_ONCE -> {
                        val inFlight = session.store(publish)
                        sessionOwnsPacketIdentifier = true
                        sendAtLeastOnceMessage(inFlight)
                    }

                    QoS.EXACTLY_ONE -> {
                        val inFlight = session.store(publish)
                        sessionOwnsPacketIdentifier = true
                        sendExactlyOnceMessage(inFlight)
                    }
                }
            } catch (ex: MqttException) {
                if (!sessionOwnsPacketIdentifier) {
                    releasePacketIdentifier(publish.packetIdentifier)
                }
                throw ex
            } catch (ex: IllegalArgumentException) {
                releasePacketIdentifier(publish.packetIdentifier)
                throw ex
            }
        }.getOrElse { throw it }
    }

    override suspend fun disconnect(
        sessionExpiryInterval: SessionExpiryInterval?,
        reasonCode: DisconnectReason,
        reasonString: String?,
    ) {
        keepAliveJob?.cancel()
        engine.send(DefaultMqttClientPackets.createDisconnect(reasonCode, reasonString, sessionExpiryInterval))
        engine.disconnect()
        _connectionState.value = MqttConnectionState.Disconnected
    }

    override fun messages(filter: TopicFilter): Flow<MqttPublishMessage> = incomingPublishes.filter { publish ->
        filter.matches(publish.topic)
    }

    override fun close() {
        engine.close()
        scope.cancel()
    }

    private suspend fun sendAtLeastOnceMessage(inFlight: InFlightPublish): PublishResponse {
        sendQuota.acquire()
        val publish = inFlight.source

        val puback = awaitResponseOf<Puback>({
            it.isResponseFor<Puback>(publish)
        }) {
            engine.send(publish)
        }.getOrElse {
            it.throwHandshakeFailure("PUBACK", publish)
        }

        session.acknowledge(inFlight)
        releasePacketIdentifier(publish.packetIdentifier)
        return AtLeastOncePublishResponse(publish, puback)
    }

    private suspend fun sendExactlyOnceMessage(inFlight: InFlightPublish): PublishResponse {
        sendQuota.acquire()
        val publish = inFlight.source

        awaitResponseOf<Pubrec>({
            it.isResponseFor<Pubrec>(publish)
        }) {
            engine.send(publish)
        }.getOrElse {
            it.throwHandshakeFailure("PUBREC", publish)
        }

        val pubrel = session.replace(inFlight)
        val pubcomp = awaitResponseOf<Pubcomp>({
            it.isResponseFor<Pubcomp>(pubrel.source)
        }) {
            engine.send(pubrel.source)
        }.getOrElse {
            it.throwHandshakeFailure("PUBCOMP", publish)
        }

        session.acknowledge(pubrel)
        releasePacketIdentifier(publish.packetIdentifier)
        return ExactlyOnePublishResponse(publish, pubcomp)
    }

    private suspend fun sendPubrel(pubrel: Pubrel): Pubcomp? = awaitResponseOf<Pubcomp>({
        it.isResponseFor<Pubcomp>(pubrel)
    }) {
        engine.send(pubrel)
    }.getOrElse {
        null
    }

    private suspend fun Throwable.throwHandshakeFailure(expected: String, publish: Publish): Nothing {
        disconnectAfterHandshakeFailure()
        if (this is TimeoutException) {
            throw HandshakeFailedException(
                "Did not receive $expected for $publish",
                publish,
            )
        }
        throw this
    }

    private suspend fun inspectConnack(connack: Connack): Connack {
        if (!connack.isSuccess) {
            Logger.i {
                "Server sent CONNACK packet with ${connack.reason}, hence terminating the connection"
            }
            engine.disconnect()
            _connectionState.value =
                MqttConnectionState.Disconnected
        } else {
            _connectionState.value =
                MqttConnectionState.Connected(connack)
            capabilities.updateFrom(connack, config.clientId, ::updateReceiveMaximum)

            val keepAlive = (connack.serverKeepAlive?.value ?: config.keepAliveSeconds).toInt().seconds
            if (keepAlive.inWholeSeconds > 0) {
                keepAliveJob?.cancel()
                keepAliveJob = scope.launch {
                    while (_connectionState.value ==
                        MqttConnectionState.Connected(connack)
                    ) {
                        delay(keepAlive)
                        val result =
                            awaitResponseOf<Pingresp>(
                                { packet -> packet.type == PacketType.PINGRESP },
                            ) {
                                engine.send(Pingreq)
                            }

                        if (result.isFailure) {
                            Logger.e { "Keep Alive failure" }
                            disconnect(config.sessionExpiryInterval)
                        }
                    }
                }
            }

            Logger.i {
                "Received server parameters: " +
                    "maxQoS=$maxQos, " +
                    "keepAlive=$keepAlive, " +
                    "serverTopicAliasMaximum=${serverTopicAliasMaximum.value}, " +
                    "assignedClientIdentifier=${connack.assignedClientIdentifier?.value ?: "''"}, " +
                    "subscriptionIdentifierAvailable=$subscriptionIdentifierAvailable, " +
                    "receiveMaximum=$receiveMaximum, " +
                    "retainAvailable=$isRetainAvailable, " +
                    "maximumPacketSize=$maxPacketSize, " +
                    "wildcardSubscriptionAvailable=$isWildcardSubscriptionAvailable, " +
                    "sharedSubscriptionAvailable=$isSharedSubscriptionAvailable"
            }
        }

        return connack
    }

    private suspend fun resumeSession() {
        session.unacknowledgedPackets().forEach { packet ->
            try {
                when (packet) {
                    is InFlightPublish -> {
                        when (packet.source.qoS) {
                            QoS.AT_MOST_ONCE -> Logger.e {
                                "Unexpected packet in session store: $packet"
                            }

                            QoS.AT_LEAST_ONCE -> sendAtLeastOnceMessage(
                                packet,
                            )

                            QoS.EXACTLY_ONE -> sendExactlyOnceMessage(
                                packet,
                            )
                        }
                    }

                    is InFlightPubrel -> {
                        sendPubrel(packet.source)?.also {
                            session.acknowledge(packet)
                            releasePacketIdentifier(packet.packetIdentifier)
                        }
                    }
                }
            } catch (ex: HandshakeFailedException) {
                Logger.w(ex) {
                    "Error resuming session, will try next time: $packet"
                }
            } catch (ex: MqttException) {
                Logger.e(ex) {
                    "Error resuming session, re-trying next time"
                }
                return
            }
        }
    }

    private suspend fun handlePacketResult(result: Result<Packet>) {
        result.onSuccess { packet ->
            handlePacket(packet)
        }.onFailure { throwable ->
            if (throwable is MalformedPacketException) {
                Logger.w {
                    "Received malformed packet: '${throwable.message}', disconnecting..."
                }
            } else {
                Logger.e(throwable = throwable) {
                    "Unexpected error while parsing a packet, disconnecting..."
                }
            }
            _connectionState.value =
                MqttConnectionState.ConnectionError(throwable)
            pendingResponses.reset()
            sendQuota.reset()
            engine.disconnect()
        }
    }

    private suspend fun handlePacket(packet: Packet) {
        Logger.d { "Received packet: $packet" }
        when (packet) {
            is Disconnect -> {
                Logger.i {
                    "Received DISCONNECT (${packet.reasonString.ifNull(packet.reason)}) from server, disconnecting..."
                }
                engine.disconnect()
            }

            is Publish -> {
                when (packet.qoS) {
                    QoS.AT_MOST_ONCE -> {
                        _incomingPublishes.emit(packet.toIncomingMessage())
                    }

                    QoS.AT_LEAST_ONCE -> {
                        _incomingPublishes.emit(packet.toIncomingMessage())
                        engine.send(
                            Puback.from(packet),
                        )
                    }

                    QoS.EXACTLY_ONE -> {
                        val id = requireNotNull(packet.packetIdentifier) {
                            "QoS EXACTLY_ONE PUBLISH packet must have a packet identifier"
                        }
                        if (publishReceivedPackets.containsKey(id)) {
                            // Must resend the PUBREC packet
                            publishReceivedPackets[id]?.let {
                                engine.send(it)
                            }
                        } else {
                            if (!session.hasIncomingPacketId(packet)) {
                                session.rememberIncomingPacketId(packet)
                                _incomingPublishes.emit(packet.toIncomingMessage())
                            }
                            val pubrec = Pubrec.from(
                                packet,
                            )
                            publishReceivedPackets[id] = pubrec
                            engine.send(pubrec)
                        }
                    }
                }
            }

            is Pubrel -> {
                engine.send(Pubcomp.from(packet))
                publishReceivedPackets.remove(packet.packetIdentifier)
                session.releaseIncomingPacketId(packet)
            }

            is Puback -> {
                sendQuota.release() // See chapter 4.9 Flow Control
                pendingResponses.dispatch(packet)
            }

            is Pubcomp -> {
                sendQuota.release() // See chapter 4.9 Flow Control
                pendingResponses.dispatch(packet)
            }

            is Pubrec -> {
                // See chapter 4.9 Flow Control
                if (packet.reason >= UnspecifiedError) {
                    sendQuota.release()
                }
                pendingResponses.dispatch(packet)
            }

            else -> {
                pendingResponses.dispatch(packet)
            }
        }
    }

    private suspend fun updateReceiveMaximum(value: UShort) {
        sendQuota.updateLimit(capabilities.receiveMaximum, value)
    }

    private suspend inline fun <reified P : Packet> awaitResponseOf(
        noinline predicate: (P) -> Boolean,
        crossinline request: suspend () -> Result<Unit>,
    ): Result<P> = pendingResponses.awaitResponseOf(predicate, request)

    private suspend fun disconnectAfterHandshakeFailure() {
        keepAliveJob?.cancel()
        pendingResponses.reset()
        sendQuota.reset()
        engine.disconnect()
        restorePacketIdentifiersFromSession()
        _connectionState.value = MqttConnectionState.Disconnected
    }

    private suspend fun clearSessionState() {
        session.clear()
        publishReceivedPackets.clear()
        clearAllocatedPacketIdentifiers()
    }

    /**
     * Returns the next MQTT packet identifier in the valid range `1..65535`.
     *
     * The sequence wraps back to `1` after reaching `65535`.
     */
    private suspend fun nextPacketIdentifier(): UShort = packetIdentifierMutex.withLock {
        if (allocatedPacketIdentifiers.size >= MAX_PACKET_IDENTIFIER) {
            throw ConnectionException("No MQTT packet identifiers available")
        }
        var candidate: UShort
        do {
            packetIdentifier = nextPacketIdentifierAfter(packetIdentifier)
            candidate = packetIdentifier.toUShort()
        } while (candidate in allocatedPacketIdentifiers)
        allocatedPacketIdentifiers += candidate
        Logger.v { "Next packet identifier: $packetIdentifier" }
        candidate
    }

    private suspend fun releasePacketIdentifier(packetIdentifier: UShort?) {
        if (packetIdentifier == null) return
        packetIdentifierMutex.withLock {
            allocatedPacketIdentifiers -= packetIdentifier
        }
    }

    private suspend fun clearAllocatedPacketIdentifiers() {
        packetIdentifierMutex.withLock {
            allocatedPacketIdentifiers.clear()
        }
    }

    private suspend fun restorePacketIdentifiersFromSession() {
        val unacknowledgedPacketIdentifiers = session.unacknowledgedPackets().mapTo(mutableSetOf()) {
            it.packetIdentifier
        }
        packetIdentifierMutex.withLock {
            allocatedPacketIdentifiers.clear()
            allocatedPacketIdentifiers += unacknowledgedPacketIdentifiers
        }
    }

    private fun nextPacketIdentifierAfter(packetIdentifier: Int): Int {
        val next = packetIdentifier + 1
        return if (next > MAX_PACKET_IDENTIFIER) {
            1 // Zero is not allowed as packet identifier
        } else {
            next
        }
    }

    private companion object {
        private val MAX_PACKET_IDENTIFIER = UShort.MAX_VALUE.toInt()
    }
}
