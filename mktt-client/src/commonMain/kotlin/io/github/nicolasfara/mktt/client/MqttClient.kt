package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.client.matches
import io.github.nicolasfara.mktt.client.toIncomingMessage
import io.github.nicolasfara.mktt.core.ConnectionException
import io.github.nicolasfara.mktt.core.HandshakeFailedException
import io.github.nicolasfara.mktt.core.InFlightPublish
import io.github.nicolasfara.mktt.core.InFlightPubrel
import io.github.nicolasfara.mktt.core.KeepAliveTimeout
import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.github.nicolasfara.mktt.core.NormalDisconnection
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
import io.github.nicolasfara.mktt.core.packet.*
import io.github.nicolasfara.mktt.core.packet.isResponseFor
import io.github.nicolasfara.mktt.core.toReasonString
import io.github.nicolasfara.mktt.core.util.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Semaphore
import kotlin.collections.map
import kotlin.collections.set
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.updateAndFetch
import kotlin.time.Duration.Companion.seconds

public class MqttClient internal constructor(
    private val config: io.github.nicolasfara.mktt.client.MqttClientConfig,
    private val engine: io.github.nicolasfara.mktt.client.MqttEngine,
    private val session: io.github.nicolasfara.mktt.core.SessionStore,
) : AutoCloseable {

    public constructor(config: io.github.nicolasfara.mktt.client.MqttClientConfig) :
        this(config, config.engine, config.sessionStoreProvider())

    private val _incomingPublishes = MutableSharedFlow<io.github.nicolasfara.mktt.client.MqttPublishMessage>()
    private val _connectionState =
        MutableStateFlow<io.github.nicolasfara.mktt.client.MqttConnectionState>(
            _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.Disconnected,
        )

    public val incomingPublishes: SharedFlow<io.github.nicolasfara.mktt.client.MqttPublishMessage>
        get() = _incomingPublishes.asSharedFlow()

    /**
     * Returns the maximum QoS level allowed by the server, defaults to [io.github.nicolasfara.mktt.core.QoS.EXACTLY_ONE] as long as no CONNACK packet
     * has been received.
     */
    public val maxQos: io.github.nicolasfara.mktt.core.QoS
        get() = _maxQos
    private var _maxQos = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.EXACTLY_ONE

    /**
     * The ID of this client as defined in [io.github.nicolasfara.mktt.client.MqttClientConfig] or the value of the assigned client ID of the [io.github.nicolasfara.mktt.core.packet.Connack]
     * packet, if [io.github.nicolasfara.mktt.client.MqttClientConfig] contained an empty string.
     */
    public val clientId: String
        get() = _clientId
    private var _clientId = config.clientId

    /**
     * The server topic alias maximum value as contained the CONNACK message from the server (or the default value of 0)
     */
    public val serverTopicAliasMaximum: io.github.nicolasfara.mktt.core.TopicAliasMaximum
        get() = _serverTopicAliasMaximum
    private var _serverTopicAliasMaximum: io.github.nicolasfara.mktt.core.TopicAliasMaximum =
        _root_ide_package_.io.github.nicolasfara.mktt.core.TopicAliasMaximum(0u)

    /**
     * The value of 'Subscription Identifiers Available' from the CONNACK message of the server.
     */
    public val subscriptionIdentifierAvailable: Boolean
        get() = _subscriptionIdentifierAvailable
    private var _subscriptionIdentifierAvailable = true

    /**
     * The value of 'Receive Maximum' from the CONNACK message of the server.
     */
    public val receiveMaximum: UShort
        get() = _receiveMaximum
    private var _receiveMaximum = UShort.MAX_VALUE
        set(value) {
            field = value
            sendQuota = Semaphore(value.toInt())
        }

    /**
     * The value of 'Retain Available' from the CONNACK message of the server.
     */
    public val isRetainAvailable: Boolean
        get() = _isRetainAvailable
    private var _isRetainAvailable = true

    /**
     * The value of 'Wildcard Subscription Available' from the CONNACK message of the server.
     *
     * Note that this value is only reported here, the [subscribe] method merely logs a warning message if a wildcard
     * subscription is requested, when the server does not support it. The server should send a DISCONNECT with reason
     * [WildcardSubscriptionsNotSupported] when a wild card subscription was requested for a server who is not
     * supporting it.
     */
    public val isWildcardSubscriptionAvailable: Boolean
        get() = _isWildcardSubscriptionAvailable
    private var _isWildcardSubscriptionAvailable = true

    /**
     * The value of 'Shared Subscription Available' from the CONNACK message of the server.
     *
     * Note that this value is only reported here, the [subscribe] method merely logs a warning message if a shared
     * subscription is requested, when the server does not support it. The server should send a DISCONNECT with reason
     * [SharedSubscriptionsNotSupported] when a wild card subscription was requested for a server who is not supporting
     * it.
     */
    public val isSharedSubscriptionAvailable: Boolean
        get() = _isSharedSubscriptionAvailable
    private var _isSharedSubscriptionAvailable = true

    /**
     * The value of 'Maximum Packet Size' from the CONNACK message of the server.
     *
     * Note that this value is only reported here, packets are not checked for their size before being sent.
     */
    public val maxPacketSize: UInt
        get() = _maxPacketSize
    private var _maxPacketSize = UInt.MAX_VALUE

    /**
     * Provides the connection state of this MQTT client. When the state is [Connected] this implies that an IP
     * connectivity has been established AND that the server responded with a success CONNACK message.
     */
    public val connectionState: StateFlow<io.github.nicolasfara.mktt.client.MqttConnectionState>
        get() = _connectionState.asStateFlow()

    private val connackFlow = MutableStateFlow<io.github.nicolasfara.mktt.core.packet.Connack?>(null)

    private val scope = CoroutineScope(config.dispatcher)

    // A replay cache is crucial here to prevent a race condition where a response packet arrives
    // before the corresponding `awaitResponseOf` call is able to subscribe to the flow. Without a
    // replay cache, such a packet would be lost. A capacity of 16 is chosen to safely handle
    // bursts of responses from concurrent requests.
    private val receivedPackets = MutableSharedFlow<io.github.nicolasfara.mktt.core.packet.Packet>(replay = 16)

    @OptIn(ExperimentalAtomicApi::class)
    private val packetIdentifier = AtomicInt(0)

    // Initialize with the default receive maximum
    private var sendQuota = Semaphore(65535)

    private val publishReceivedPackets = mutableMapOf<UShort, io.github.nicolasfara.mktt.core.packet.Pubrec>()

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
                    _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.Connecting
                ) {
                    _connectionState.value =
                        _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.Disconnected
                }
            }
        }
    }

    /**
     * Tries to connect to the MQTT server and send a CONNECT message.
     *
     * @param isCleanStart when set to `true` the `Clean Start` flag in the CONNACK packet will be set to `1`. Also, the
     *        [io.github.nicolasfara.mktt.core.SessionStore] is cleared.
     * @return the connection result. Note that even when the result returns a Connack packet, the client may still not
     *         be successfully connected, as the server may send a CONNACK with an error message.
     * @see connectionState
     */
    public suspend fun connect(cleanStart: Boolean = true): io.github.nicolasfara.mktt.client.ConnAck {
        _connectionState.value = _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.Connecting
        connackFlow.emit(null)

        if (cleanStart) {
            session.clear()
        }
        return try {
            engine.start().getOrElse { throw it }
            val connack = awaitResponseOf<io.github.nicolasfara.mktt.core.packet.Connack>(
                _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.CONNACK,
            ) {
                engine.send(createConnect(cleanStart))
            }.getOrElse { throw it }
            inspectConnack(connack)

            if (connack.isSessionPresent) {
                resumeSession()
            } else {
                session.clear()
            }
            connack
        } catch (throwable: Throwable) {
            _connectionState.value =
                _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.ConnectionError(throwable)
            throw throwable
        }
    }

    /**
     * Sends a SUBSCRIBE request to the MQTT server for the list of topics contained in [filters].
     *
     * @param filters the filters to subscribe to
     * @param subscriptionIdentifier an optional subscription identifier for this subscribe request. Note that a
     *        non-null value will be ignored, when the server does not support subscription identifiers.
     * @return the SUBACK packet if the subscribe request was answered by the server. Note that the SUBACK may still
     *         contain error messages for each of the subscribed topics.
     * @see io.github.nicolasfara.mktt.core.packet.hasFailure
     * @see subscriptionIdentifierAvailable
     */
    public suspend fun subscribe(
        filters: List<io.github.nicolasfara.mktt.core.TopicFilter>,
        subscriptionIdentifier: io.github.nicolasfara.mktt.core.SubscriptionIdentifier? = null,
        userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
    ): io.github.nicolasfara.mktt.client.SubAck {
        if (!_isWildcardSubscriptionAvailable && filters.hasWildcard()) {
            _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.w {
                "Requesting at least one wildcard subscription ($filters), but the server does not support it. " +
                    "This will likely result in a DISCONNECT message from the server."
            }
        }
        if (!_isSharedSubscriptionAvailable && filters.hasSharedTopic()) {
            _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.w {
                "Requesting at least one shared subscription ($filters), but the server does not support it. " +
                    "This will likely result in a DISCONNECT message from the server."
            }
        }
        val identifier = if ((subscriptionIdentifier != null) && !_subscriptionIdentifierAvailable) {
            _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.w(
                throwable = IllegalArgumentException("Ignoring $subscriptionIdentifier"),
            ) {
                "Ignoring subscription identifier, as the server doesn't support it"
            }
            null
        } else {
            subscriptionIdentifier
        }
        val subscribe = createSubscribe(filters, identifier, userProperties)

        return awaitResponseOf<io.github.nicolasfara.mktt.core.packet.Suback>({
            it.isResponseFor<io.github.nicolasfara.mktt.core.packet.Suback>(subscribe)
        }, {
            engine.send(subscribe)
        }).getOrElse { throw it }
    }

    public suspend fun unsubscribe(
        filters: List<io.github.nicolasfara.mktt.core.TopicFilter>,
        userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
    ): io.github.nicolasfara.mktt.client.UnsubAck {
        val unsubscribe =
            createUnsubscribe(
                filters.map(_root_ide_package_.io.github.nicolasfara.mktt.core.TopicFilter::filter),
                userProperties,
            )

        return awaitResponseOf<io.github.nicolasfara.mktt.core.packet.Unsuback>({
            it.isResponseFor<io.github.nicolasfara.mktt.core.packet.Unsuback>(unsubscribe)
        }, {
            engine.send(unsubscribe)
        }).getOrElse { throw it }
    }

    /**
     * Sends the specified [io.github.nicolasfara.mktt.client.PublishRequest] to the server.
     *
     * In case the server announced a [io.github.nicolasfara.mktt.core.QoS] value lower than the one requested, the QoS of the published packet will be
     * automatically downgraded. The actual QoS can be determined from either [maxQos] or from [io.github.nicolasfara.mktt.client.qoS]
     *
     * When this method successfully returns, all handshake packets required by the actual `QoS` will be exchanged
     * between this client and the server. When the server does not respond within
     * [ackMessageTimeout][de.kempmobil.ktor.mqtt.MqttClientConfigBuilder.ackMessageTimeout] the result will be a
     * failure with a [io.github.nicolasfara.mktt.core.HandshakeFailedException].
     *
     * All returned exceptions are of type [MqttException] resp. its subtypes.
     */
    public suspend fun publish(
        request: io.github.nicolasfara.mktt.client.PublishRequest,
    ): io.github.nicolasfara.mktt.client.PublishResult {
        if (!engine.connected.value) {
            throw _root_ide_package_.io.github.nicolasfara.mktt.core.ConnectionException(
                "Cannot send PUBLISH packet while not connected",
            )
        }

        return createPublish(request).mapCatching { publish ->
            when (publish.qoS) {
                _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE -> sendAtMostOnceMessage(publish)

                _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_LEAST_ONCE -> sendAtLeastOnceMessage(
                    session.store(publish),
                )

                _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.EXACTLY_ONE -> sendExactlyOnceMessage(
                    session.store(publish),
                )
            }
        }.getOrElse { throw it }
    }

    public suspend fun disconnect(
        reasonCode: io.github.nicolasfara.mktt.client.DisconnectReason = _root_ide_package_.io.github.nicolasfara.mktt.core.NormalDisconnection,
        reasonString: String? = null,
        sessionExpiryInterval: io.github.nicolasfara.mktt.core.SessionExpiryInterval? = config.sessionExpiryInterval,
    ) {
        engine.send(createDisconnect(reasonCode, reasonString, sessionExpiryInterval))
        engine.disconnect()
        _connectionState.value = _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.Disconnected
    }

    public fun messages(
        filter: io.github.nicolasfara.mktt.core.TopicFilter,
    ): Flow<io.github.nicolasfara.mktt.client.MqttPublishMessage> = incomingPublishes.filter { publish ->
        filter.matches(publish.topic)
    }

    public override fun close() {
        engine.close()
        scope.cancel()
    }

    // ---- Helper methods ---------------------------------------------------------------------------------------------

    private fun createConnect(isCleanStart: Boolean): io.github.nicolasfara.mktt.core.packet.Connect =
        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Connect(
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
        filters: List<io.github.nicolasfara.mktt.core.TopicFilter>,
        subscriptionIdentifier: io.github.nicolasfara.mktt.core.SubscriptionIdentifier?,
        userProperties: io.github.nicolasfara.mktt.core.UserProperties,
    ): io.github.nicolasfara.mktt.core.packet.Subscribe =
        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Subscribe(
            packetIdentifier = nextPacketIdentifier(),
            filters = filters,
            subscriptionIdentifier = subscriptionIdentifier,
            userProperties = userProperties,
        )

    private fun createUnsubscribe(
        topics: List<io.github.nicolasfara.mktt.core.Topic>,
        userProperties: io.github.nicolasfara.mktt.core.UserProperties,
    ): io.github.nicolasfara.mktt.core.packet.Unsubscribe =
        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Unsubscribe(
            packetIdentifier = nextPacketIdentifier(),
            topics = topics,
            userProperties = userProperties,
        )

    private fun createPublish(
        request: io.github.nicolasfara.mktt.client.PublishRequest,
        isDupMessage: Boolean = false,
    ): Result<io.github.nicolasfara.mktt.core.packet.Publish> =
        if (request.topicAlias != null && request.topicAlias.value > serverTopicAliasMaximum.value) {
            Result.failure(
                _root_ide_package_.io.github.nicolasfara.mktt.core.TopicAliasException(
                    "Server maximum topic alias is: $serverTopicAliasMaximum, but you requested: ${request.topicAlias}",
                ),
            )
        } else {
            val actualQoS = request.desiredQoS.coerceAtMost(maxQos) // MQTT-3.2.2-11
            if (actualQoS != request.desiredQoS) {
                _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.i {
                    "Publish QoS for ${request.topic} was ${request.desiredQoS} but was downgraded to $actualQoS due to server requirements"
                }
            }
            Result.success(
                _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Publish(
                    isDupMessage = if (actualQoS ==
                        _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE
                    ) {
                        false
                    } else {
                        isDupMessage
                    }, // MQTT-3.3.1-2
                    qoS = actualQoS,
                    isRetainMessage = _isRetainAvailable && request.isRetainMessage,
                    packetIdentifier = if (actualQoS ==
                        _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE
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
                    subscriptionIdentifier = null, // A PUBLISH packet sent from a Client to a Server MUST NOT contain a Subscription Identifier [MQTT-3.3.4-6]
                    contentType = request.contentType,
                    payload = request.payloadAsByteString(),
                ),
            )
        }

    private suspend fun sendAtMostOnceMessage(
        publish: io.github.nicolasfara.mktt.core.packet.Publish,
    ): io.github.nicolasfara.mktt.client.PublishResponse {
        engine.send(publish).onFailure { throw it }
        return _root_ide_package_.io.github.nicolasfara.mktt.client.AtMostOncePublishResponse(publish)
    }

    private suspend fun sendAtLeastOnceMessage(
        inFlight: io.github.nicolasfara.mktt.core.InFlightPublish,
    ): io.github.nicolasfara.mktt.client.PublishResponse {
        acquireSendQuotaSafe()
        val publish = inFlight.source

        val puback = awaitResponseOf<io.github.nicolasfara.mktt.core.packet.Puback>({
            it.isResponseFor<io.github.nicolasfara.mktt.core.packet.Puback>(publish)
        }) {
            engine.send(publish)
        }.getOrElse {
            it.throwHandshakeExceptionForTimeout("PUBACK", publish)
        }

        session.acknowledge(inFlight)
        return _root_ide_package_.io.github.nicolasfara.mktt.client.AtLeastOncePublishResponse(publish, puback)
    }

    private suspend fun sendExactlyOnceMessage(
        inFlight: io.github.nicolasfara.mktt.core.InFlightPublish,
    ): io.github.nicolasfara.mktt.client.PublishResponse {
        acquireSendQuotaSafe()
        val publish = inFlight.source

        awaitResponseOf<io.github.nicolasfara.mktt.core.packet.Pubrec>({
            it.isResponseFor<io.github.nicolasfara.mktt.core.packet.Pubrec>(publish)
        }) {
            engine.send(publish)
        }.getOrElse {
            it.throwHandshakeExceptionForTimeout("PUBREC", publish)
        }

        val pubrel = session.replace(inFlight)
        val pubcomp = awaitResponseOf<io.github.nicolasfara.mktt.core.packet.Pubcomp>({
            it.isResponseFor<io.github.nicolasfara.mktt.core.packet.Pubcomp>(pubrel.source)
        }) {
            engine.send(pubrel.source)
        }.getOrElse {
            it.throwHandshakeExceptionForTimeout("PUBCOMP", publish)
        }

        session.acknowledge(pubrel)
        return _root_ide_package_.io.github.nicolasfara.mktt.client.ExactlyOnePublishResponse(publish, pubcomp)
    }

    private suspend fun sendPubrel(
        pubrel: io.github.nicolasfara.mktt.core.packet.Pubrel,
    ): io.github.nicolasfara.mktt.core.packet.Pubcomp? =
        awaitResponseOf<io.github.nicolasfara.mktt.core.packet.Pubcomp>({
            it.isResponseFor<io.github.nicolasfara.mktt.core.packet.Pubcomp>(pubrel)
        }) {
            engine.send(pubrel)
        }.getOrElse {
            null
        }

    private fun Throwable.throwHandshakeExceptionForTimeout(
        expected: String,
        publish: io.github.nicolasfara.mktt.core.packet.Publish,
    ): Nothing {
        if (this is io.github.nicolasfara.mktt.core.TimeoutException) {
            throw _root_ide_package_.io.github.nicolasfara.mktt.core.HandshakeFailedException(
                "Did not receive $expected for $publish",
                publish,
            )
        } else {
            throw this
        }
    }

    private fun createDisconnect(
        reasonCode: io.github.nicolasfara.mktt.core.ReasonCode,
        reason: String?,
        sessionExpiryInterval: io.github.nicolasfara.mktt.core.SessionExpiryInterval?,
    ): io.github.nicolasfara.mktt.core.packet.Disconnect =
        _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Disconnect(
            reason = reasonCode,
            sessionExpiryInterval = sessionExpiryInterval,
            reasonString = reason.toReasonString(),
        )

    private suspend fun inspectConnack(
        connack: io.github.nicolasfara.mktt.core.packet.Connack,
    ): io.github.nicolasfara.mktt.core.packet.Connack {
        connackFlow.emit(connack)

        if (!connack.isSuccess) {
            _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.i {
                "Server sent CONNACK packet with ${connack.reason}, hence terminating the connection"
            }
            engine.disconnect()
            _connectionState.value =
                _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.Disconnected
        } else {
            _connectionState.value =
                _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.Connected(connack)
            connack.maximumQoS?.let {
                _maxQos = it.qoS
            }
            _serverTopicAliasMaximum =
                connack.topicAliasMaximum ?: _root_ide_package_.io.github.nicolasfara.mktt.core.TopicAliasMaximum(
                    0u,
                )

            val keepAlive = (connack.serverKeepAlive?.value ?: config.keepAliveSeconds).toInt().seconds
            if (keepAlive.inWholeSeconds > 0) {
                scope.launch {
                    while (_connectionState.value ==
                        _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.Connected(connack)
                    ) {
                        delay(keepAlive)
                        val result =
                            awaitResponseOf<io.github.nicolasfara.mktt.core.packet.Pingresp>(
                                _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PINGRESP,
                            ) {
                                engine.send(_root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pingreq)
                            }

                        if (result.isFailure) {
                            _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.e { "Keep Alive failure" }
                            disconnect(_root_ide_package_.io.github.nicolasfara.mktt.core.KeepAliveTimeout)
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

            _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.i {
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
                    is io.github.nicolasfara.mktt.core.InFlightPublish -> {
                        when (packet.source.qoS) {
                            _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE -> _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.e {
                                "Unexpected packet in session store: $packet"
                            }

                            _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_LEAST_ONCE -> sendAtLeastOnceMessage(
                                packet,
                            )

                            _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.EXACTLY_ONE -> sendExactlyOnceMessage(
                                packet,
                            )
                        }
                    }

                    is io.github.nicolasfara.mktt.core.InFlightPubrel -> {
                        sendPubrel(packet.source)?.also {
                            session.acknowledge(packet)
                        }
                    }
                }
            } catch (ex: io.github.nicolasfara.mktt.core.HandshakeFailedException) {
                _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.w(ex) {
                    "Error resuming session, will try next time: $packet"
                }
            } catch (ex: Exception) {
                _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.e(ex) {
                    "Error resuming session, re-trying next time"
                }
                return
            }
        }
    }

    private suspend fun handlePacketResult(result: Result<io.github.nicolasfara.mktt.core.packet.Packet>) {
        result.onSuccess { packet ->
            handlePacket(packet)
        }.onFailure { throwable ->
            if (throwable is io.github.nicolasfara.mktt.core.MalformedPacketException) {
                _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.w {
                    "Received malformed packet: '${throwable.message}', disconnecting..."
                }
            } else {
                _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.e(throwable = throwable) {
                    "Unexpected error while parsing a packet, disconnecting..."
                }
            }
            _connectionState.value =
                _root_ide_package_.io.github.nicolasfara.mktt.client.MqttConnectionState.ConnectionError(throwable)
            engine.disconnect()
        }
    }

    private suspend fun handlePacket(packet: io.github.nicolasfara.mktt.core.packet.Packet) {
        _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.d { "Received packet: $packet" }
        when (packet) {
            is io.github.nicolasfara.mktt.core.packet.Disconnect -> {
                _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.i {
                    "Received DISCONNECT (${packet.reasonString.ifNull(packet.reason)}) from server, disconnecting..."
                }
                engine.disconnect()
            }

            is io.github.nicolasfara.mktt.core.packet.Publish -> {
                when (packet.qoS) {
                    _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE -> {
                        _incomingPublishes.emit(packet.toIncomingMessage())
                    }

                    _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_LEAST_ONCE -> {
                        _incomingPublishes.emit(packet.toIncomingMessage())
                        engine.send(
                            _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Puback.Companion.from(packet),
                        )
                    }

                    _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.EXACTLY_ONE -> {
                        val id = packet.packetIdentifier!!
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
                            val pubrec = _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubrec.Companion.from(
                                packet,
                            )
                            publishReceivedPackets[id] = pubrec
                            engine.send(pubrec)
                        }
                    }
                }
            }

            is io.github.nicolasfara.mktt.core.packet.Pubrel -> {
                engine.send(_root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubcomp.Companion.from(packet))
                publishReceivedPackets.remove(packet.packetIdentifier)
                session.releaseIncomingPacketId(packet)
            }

            is io.github.nicolasfara.mktt.core.packet.Puback -> {
                releaseSendQuotaSafe() // See chapter 4.9 Flow Control
                receivedPackets.emit(packet)
            }

            is io.github.nicolasfara.mktt.core.packet.Pubcomp -> {
                releaseSendQuotaSafe() // See chapter 4.9 Flow Control
                receivedPackets.emit(packet)
            }

            is io.github.nicolasfara.mktt.core.packet.Pubrec -> {
                // See chapter 4.9 Flow Control
                if (packet.reason >= _root_ide_package_.io.github.nicolasfara.mktt.core.UnspecifiedError) {
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
        try {
            sendQuota.acquire()
        } catch (ex: CancellationException) {
            throw _root_ide_package_.io.github.nicolasfara.mktt.core.ConnectionException(
                "PUBLISH cancelled while waiting for send quota",
                ex,
            )
        }
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

    private suspend inline fun <reified P : io.github.nicolasfara.mktt.core.packet.Packet> awaitResponseOf(
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
                    _root_ide_package_.io.github.nicolasfara.mktt.core.TimeoutException(
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

    private suspend inline fun <reified P : io.github.nicolasfara.mktt.core.packet.Packet> awaitResponseOf(
        type: io.github.nicolasfara.mktt.core.packet.PacketType,
        crossinline request: suspend () -> Result<Unit>,
    ): Result<P> = awaitResponseOf({ packet: P -> packet.type == type }, request)

    @OptIn(ExperimentalAtomicApi::class)
    internal fun nextPacketIdentifier(): UShort = packetIdentifier.updateAndFetch { p ->
        val next = p + 1
        if (next > UShort.MAX_VALUE.toInt()) {
            1 // Zero is not allowed as packet identifier
        } else {
            next
        }
    }.also {
        _root_ide_package_.io.github.nicolasfara.mktt.core.util.Logger.v { "Next packet identifier: $it" }
    }.toUShort()
}

/**
 * Creates a new MQTT client, connecting to the specified host on the specified port.
 *
 * To enable TLS on the connection, use the following code snippet:
 * ```
 * MqttClient("test.mosquitto.org", 8886) {
 *     connection {
 *         tls { }
 *     }
 *     ...
 * }
 * ```
 *
 * @sample de.kempmobil.ktor.mqtt.ClientSample.createClient
 */
public fun MqttClient(
    host: String,
    port: Int,
    init:
    io.github.nicolasfara.mktt.client.MqttClientConfigBuilder<io.github.nicolasfara.mktt.client.DefaultEngineConfig>.() -> Unit,
): io.github.nicolasfara.mktt.client.MqttClient = _root_ide_package_.io.github.nicolasfara.mktt.client.MqttClient(
    _root_ide_package_.io.github.nicolasfara.mktt.client.MqttClientConfigBuilder(
        _root_ide_package_.io.github.nicolasfara.mktt.client.DefaultEngineFactory(
            host,
            port,
        ),
    ).apply(init).build(),
)
