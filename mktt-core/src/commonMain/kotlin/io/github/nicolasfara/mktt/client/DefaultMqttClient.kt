package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.ConnectionException
import io.github.nicolasfara.mktt.core.HandshakeFailedException
import io.github.nicolasfara.mktt.core.InFlightPublish
import io.github.nicolasfara.mktt.core.InFlightPubrel
import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.github.nicolasfara.mktt.core.MqttException
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.SessionStore
import io.github.nicolasfara.mktt.core.SubscriptionIdentifier
import io.github.nicolasfara.mktt.core.TimeoutException
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicAliasException
import io.github.nicolasfara.mktt.core.TopicAliasMaximum
import io.github.nicolasfara.mktt.core.TopicFilter
import io.github.nicolasfara.mktt.core.UnspecifiedError
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.hasSharedTopic
import io.github.nicolasfara.mktt.core.hasWildcard
import io.github.nicolasfara.mktt.core.ifNull
import io.github.nicolasfara.mktt.core.isAvailable
import io.github.nicolasfara.mktt.core.packet.Connack
import io.github.nicolasfara.mktt.core.packet.Connect
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
import io.github.nicolasfara.mktt.core.packet.Subscribe
import io.github.nicolasfara.mktt.core.packet.Unsuback
import io.github.nicolasfara.mktt.core.packet.Unsubscribe
import io.github.nicolasfara.mktt.core.packet.isResponseFor
import io.github.nicolasfara.mktt.core.toReasonString
import io.github.nicolasfara.mktt.core.util.Logger
import io.github.nicolasfara.mktt.engine.MqttEngine
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
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
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull

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
        get() = _maxQos
    private var _maxQos = QoS.EXACTLY_ONE

    override val clientId: String
        get() = _clientId
    private var _clientId = config.clientId

    override val serverTopicAliasMaximum: TopicAliasMaximum
        get() = _serverTopicAliasMaximum
    private var _serverTopicAliasMaximum: TopicAliasMaximum = TopicAliasMaximum(0u)

    override val subscriptionIdentifierAvailable: Boolean
        get() = _subscriptionIdentifierAvailable
    private var _subscriptionIdentifierAvailable = true

    override val receiveMaximum: UShort
        get() = _receiveMaximum
    private var _receiveMaximum = UShort.MAX_VALUE
        set(value) {
            field = value
            sendQuota = Semaphore(value.toInt())
        }

    override val isRetainAvailable: Boolean
        get() = _isRetainAvailable
    private var _isRetainAvailable = true

    override val isWildcardSubscriptionAvailable: Boolean
        get() = _isWildcardSubscriptionAvailable
    private var _isWildcardSubscriptionAvailable = true

    override val isSharedSubscriptionAvailable: Boolean
        get() = _isSharedSubscriptionAvailable
    private var _isSharedSubscriptionAvailable = true

    override val maxPacketSize: UInt
        get() = _maxPacketSize
    private var _maxPacketSize = UInt.MAX_VALUE

    override val connectionState: StateFlow<MqttConnectionState>
        get() = _connectionState.asStateFlow()

    private val connackFlow = MutableStateFlow<Connack?>(null)

    private val scope = CoroutineScope(engine.dispatcher)

    // A replay cache is crucial here to prevent a race condition where a response packet arrives
    // before the corresponding `awaitResponseOf` call is able to subscribe to the flow. Without a
    // replay cache, such a packet would be lost. A capacity of RESPONSE_REPLAY_CACHE_CAPACITY is
    // chosen to safely handle bursts of responses from concurrent requests.
    private val receivedPackets = MutableSharedFlow<Packet>(replay = RESPONSE_REPLAY_CACHE_CAPACITY)

    @OptIn(ExperimentalAtomicApi::class)
    private val packetIdentifier = AtomicInt(0)

    // Initialize with the default receive maximum
    private var sendQuota = Semaphore(DEFAULT_RECEIVE_MAXIMUM)

    private val publishReceivedPackets = mutableMapOf<UShort, Pubrec>()

    init {
        scope.launch {
            engine.packetResults.collect { result ->
                handlePacketResult(result)
            }
        }
        scope.launch {
            engine.connected.collect { connected ->
                if (!connected &&
                    _connectionState.value !=
                    MqttConnectionState.Connecting
                ) {
                    _connectionState.value =
                        MqttConnectionState.Disconnected
                }
            }
        }
    }

    override suspend fun connect(cleanStart: Boolean): ConnAck {
        _connectionState.value = MqttConnectionState.Connecting
        connackFlow.emit(null)

        if (cleanStart) {
            session.clear()
        }
        try {
            engine.start().fold(
                onSuccess = {},
                onFailure = { throw it },
            )

            val connack = awaitResponseOf<Connack>(
                { packet -> packet.type == PacketType.CONNACK },
            ) {
                engine.send(createConnect(cleanStart))
            }.fold(
                onSuccess = { it },
                onFailure = { throw it },
            )

            inspectConnack(connack)

            if (connack.isSessionPresent) {
                resumeSession()
            } else {
                session.clear()
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
        if (!_isWildcardSubscriptionAvailable && filters.hasWildcard()) {
            Logger.w {
                "Requesting at least one wildcard subscription ($filters), but the server does not support it. " +
                    "This will likely result in a DISCONNECT message from the server."
            }
        }
        if (!_isSharedSubscriptionAvailable && filters.hasSharedTopic()) {
            Logger.w {
                "Requesting at least one shared subscription ($filters), but the server does not support it. " +
                    "This will likely result in a DISCONNECT message from the server."
            }
        }
        val identifier = if ((subscriptionIdentifier != null) && !_subscriptionIdentifierAvailable) {
            Logger.w(IllegalArgumentException("Ignoring $subscriptionIdentifier")) {
                "Ignoring subscription identifier, as the server doesn't support it"
            }
            null
        } else {
            subscriptionIdentifier
        }
        val subscribe = createSubscribe(filters, identifier, userProperties)

        return awaitResponseOf<Suback>({
            it.isResponseFor<Suback>(subscribe)
        }, {
            engine.send(subscribe)
        }).getOrElse { throw it }
    }

    override suspend fun unsubscribe(filters: List<TopicFilter>, userProperties: UserProperties): UnsubAck {
        val unsubscribe =
            createUnsubscribe(
                filters.map(TopicFilter::filter),
                userProperties,
            )

        return awaitResponseOf<Unsuback>({
            it.isResponseFor<Unsuback>(unsubscribe)
        }, {
            engine.send(unsubscribe)
        }).getOrElse { throw it }
    }

    override suspend fun publish(request: PublishRequest): PublishResult {
        if (!engine.connected.value) {
            throw ConnectionException("Cannot send PUBLISH packet while not connected")
        }
        return createPublish(request).mapCatching { publish ->
            when (publish.qoS) {
                QoS.AT_MOST_ONCE -> {
                    engine.send(publish).onFailure { throw it }
                    AtMostOncePublishResponse(publish)
                }

                QoS.AT_LEAST_ONCE -> sendAtLeastOnceMessage(
                    session.store(publish),
                )

                QoS.EXACTLY_ONE -> sendExactlyOnceMessage(
                    session.store(publish),
                )
            }
        }.getOrElse { throw it }
    }

    override suspend fun disconnect(
        sessionExpiryInterval: SessionExpiryInterval?,
        reasonCode: DisconnectReason,
        reasonString: String?,
    ) {
        engine.send(createDisconnect(reasonCode, reasonString, sessionExpiryInterval))
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

    // ---- Helper methods ---------------------------------------------------------------------------------------------

    private fun createConnect(isCleanStart: Boolean): Connect = Connect(
        isCleanStart = isCleanStart,
        willMessage = config.willMessage,
        willQqS = config.willQqS,
        retainWillMessage = config.retainWillMessage,
        keepAliveSeconds = config.keepAliveSeconds,
        clientId = config.clientId,
        userName = config.username,
        password = config.password,
        sessionExpiryInterval = config.sessionExpiryInterval,
        receiveMaximum = config.receiveMaximum,
        maximumPacketSize = config.maximumPacketSize,
        topicAliasMaximum = config.topicAliasMaximum,
        requestResponseInformation = config.requestResponseInformation,
        requestProblemInformation = config.requestProblemInformation,
        userProperties = config.userProperties,
        authenticationMethod = config.authenticationMethod,
        authenticationData = config.authenticationData,
    )

    private fun createSubscribe(
        filters: List<TopicFilter>,
        subscriptionIdentifier: SubscriptionIdentifier?,
        userProperties: UserProperties,
    ): Subscribe = Subscribe(
        packetIdentifier = nextPacketIdentifier(),
        filters = filters,
        subscriptionIdentifier = subscriptionIdentifier,
        userProperties = userProperties,
    )

    private fun createUnsubscribe(topics: List<Topic>, userProperties: UserProperties): Unsubscribe = Unsubscribe(
        packetIdentifier = nextPacketIdentifier(),
        topics = topics,
        userProperties = userProperties,
    )

    private fun createPublish(request: PublishRequest, isDupMessage: Boolean = false): Result<Publish> =
        if (request.topicAlias != null && request.topicAlias.value > serverTopicAliasMaximum.value) {
            Result.failure(
                TopicAliasException(
                    "Server maximum topic alias is: $serverTopicAliasMaximum, but you requested: ${request.topicAlias}",
                ),
            )
        } else {
            val actualQoS = request.desiredQoS.coerceAtMost(maxQos) // MQTT-3.2.2-11
            if (actualQoS != request.desiredQoS) {
                Logger.i {
                    "Publish QoS for ${request.topic} was ${request.desiredQoS} " +
                        "but was downgraded to $actualQoS due to server requirements"
                }
            }
            Result.success(
                Publish(
                    isDupMessage = if (actualQoS ==
                        QoS.AT_MOST_ONCE
                    ) {
                        false
                    } else {
                        isDupMessage
                    }, // MQTT-3.3.1-2
                    qoS = actualQoS,
                    isRetainMessage = _isRetainAvailable && request.isRetainMessage,
                    packetIdentifier = if (actualQoS ==
                        QoS.AT_MOST_ONCE
                    ) {
                        null
                    } else {
                        nextPacketIdentifier()
                    },
                    topic = request.topic,
                    payloadFormatIndicator = request.payloadFormatIndicator,
                    messageExpiryInterval = request.messageExpiryInterval,
                    topicAlias = request.topicAlias,
                    responseTopic = request.responseTopic,
                    correlationData = request.correlationData,
                    userProperties = request.userProperties,
                    // A PUBLISH packet sent from a Client to a Server MUST NOT
                    // contain a Subscription Identifier [MQTT-3.3.4-6]
                    subscriptionIdentifier = null,
                    contentType = request.contentType,
                    payload = request.payloadAsByteString(),
                ),
            )
        }

    private suspend fun sendAtLeastOnceMessage(inFlight: InFlightPublish): PublishResponse {
        acquireSendQuotaSafe()
        val publish = inFlight.source

        val puback = awaitResponseOf<Puback>({
            it.isResponseFor<Puback>(publish)
        }) {
            engine.send(publish)
        }.getOrElse {
            it.throwHandshakeExceptionForTimeout("PUBACK", publish)
        }

        session.acknowledge(inFlight)
        return AtLeastOncePublishResponse(publish, puback)
    }

    private suspend fun sendExactlyOnceMessage(inFlight: InFlightPublish): PublishResponse {
        acquireSendQuotaSafe()
        val publish = inFlight.source

        awaitResponseOf<Pubrec>({
            it.isResponseFor<Pubrec>(publish)
        }) {
            engine.send(publish)
        }.getOrElse {
            it.throwHandshakeExceptionForTimeout("PUBREC", publish)
        }

        val pubrel = session.replace(inFlight)
        val pubcomp = awaitResponseOf<Pubcomp>({
            it.isResponseFor<Pubcomp>(pubrel.source)
        }) {
            engine.send(pubrel.source)
        }.getOrElse {
            it.throwHandshakeExceptionForTimeout("PUBCOMP", publish)
        }

        session.acknowledge(pubrel)
        return ExactlyOnePublishResponse(publish, pubcomp)
    }

    private suspend fun sendPubrel(pubrel: Pubrel): Pubcomp? = awaitResponseOf<Pubcomp>({
        it.isResponseFor<Pubcomp>(pubrel)
    }) {
        engine.send(pubrel)
    }.getOrElse {
        null
    }

    private fun Throwable.throwHandshakeExceptionForTimeout(expected: String, publish: Publish): Nothing {
        if (this is TimeoutException) {
            throw HandshakeFailedException(
                "Did not receive $expected for $publish",
                publish,
            )
        } else {
            throw this
        }
    }

    private fun createDisconnect(
        reasonCode: ReasonCode,
        reason: String?,
        sessionExpiryInterval: SessionExpiryInterval?,
    ): Disconnect = Disconnect(
        reason = reasonCode,
        sessionExpiryInterval = sessionExpiryInterval,
        reasonString = reason.toReasonString(),
    )

    private suspend fun inspectConnack(connack: Connack): Connack {
        connackFlow.emit(connack)

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
            connack.maximumQoS?.let {
                _maxQos = it.qoS
            }
            _serverTopicAliasMaximum =
                connack.topicAliasMaximum ?: TopicAliasMaximum(
                    0u,
                )

            val keepAlive = (connack.serverKeepAlive?.value ?: config.keepAliveSeconds).toInt().seconds
            if (keepAlive.inWholeSeconds > 0) {
                scope.launch {
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

            // MQTT-3.2.2-16
            if (config.clientId.isEmpty()) {
                connack.assignedClientIdentifier?.let { _clientId = it.value }
            }

            _subscriptionIdentifierAvailable = connack.subscriptionIdentifierAvailable.isAvailable()
            _receiveMaximum = connack.receiveMaximum?.value ?: UShort.MAX_VALUE
            _isRetainAvailable = connack.retainAvailable?.value ?: true
            _isWildcardSubscriptionAvailable = connack.wildcardSubscriptionAvailable?.value ?: true
            _isSharedSubscriptionAvailable = connack.sharedSubscriptionAvailable?.value ?: true
            _maxPacketSize = connack.maximumPacketSize?.value ?: UInt.MAX_VALUE

            Logger.i {
                "Received server parameters: " +
                    "maxQoS=$maxQos, " +
                    "keepAlive=$keepAlive, " +
                    "serverTopicAliasMaximum=${serverTopicAliasMaximum.value}, " +
                    "assignedClientIdentifier=${connack.assignedClientIdentifier?.value ?: "''"}, " +
                    "subscriptionIdentifierAvailable=$_subscriptionIdentifierAvailable, " +
                    "receiveMaximum=$_receiveMaximum, " +
                    "retainAvailable=$_isRetainAvailable, " +
                    "maximumPacketSize=$_maxPacketSize, " +
                    "wildcardSubscriptionAvailable=$_isWildcardSubscriptionAvailable, " +
                    "sharedSubscriptionAvailable=$_isSharedSubscriptionAvailable"
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
                releaseSendQuotaSafe() // See chapter 4.9 Flow Control
                receivedPackets.emit(packet)
            }

            is Pubcomp -> {
                releaseSendQuotaSafe() // See chapter 4.9 Flow Control
                receivedPackets.emit(packet)
            }

            is Pubrec -> {
                // See chapter 4.9 Flow Control
                if (packet.reason >= UnspecifiedError) {
                    releaseSendQuotaSafe()
                }
                receivedPackets.emit(packet)
            }

            else -> {
                receivedPackets.emit(packet)
            }
        }
    }

    private suspend fun acquireSendQuotaSafe() {
        sendQuota.acquire()
    }

    private fun releaseSendQuotaSafe() {
        try {
            sendQuota.release()
        } catch (_: IllegalStateException) {
            // "The attempt to increment above the initial send quota might be caused by the
            // re-transmission of a PUBREL packet after a new Network Connection is established."
            // Hence, we might call release() too often, which results in this IllegalStateException.
        }
    }

    private suspend inline fun <reified P : Packet> awaitResponseOf(
        noinline predicate: suspend (P) -> Boolean,
        crossinline request: suspend () -> Result<Unit>,
    ): Result<P> {
        val waitForResponse = scope.async {
            val response = withTimeoutOrNull(config.ackMessageTimeout) {
                receivedPackets.filterIsInstance<P>().first(predicate)
            }
            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(
                    TimeoutException(
                        "Didn't receive requested packet within ${config.ackMessageTimeout}",
                    ),
                )
            }
        }
        request().onFailure {
            waitForResponse.cancel()
            return Result.failure(it)
        }

        return waitForResponse.await()
    }

    @OptIn(ExperimentalAtomicApi::class)
    fun nextPacketIdentifier(): UShort = packetIdentifier.updateAndFetch { p ->
        val next = p + 1
        if (next > UShort.MAX_VALUE.toInt()) {
            1 // Zero is not allowed as packet identifier
        } else {
            next
        }
    }.also {
        Logger.v { "Next packet identifier: $it" }
    }.toUShort()

    /** Holds internal constants used by [MqttClient]. */
    companion object {
        /**
         * Replay cache capacity for the shared flow of received packets.
         * Sized to safely absorb bursts of responses from concurrent requests.
         */
        private const val RESPONSE_REPLAY_CACHE_CAPACITY = 16

        /**
         * The default receive maximum as defined by the MQTT 5 specification (section 3.3.4).
         * When the server does not specify a receive maximum in its CONNACK, this value is used.
         */
        private const val DEFAULT_RECEIVE_MAXIMUM = 65535
    }
}
