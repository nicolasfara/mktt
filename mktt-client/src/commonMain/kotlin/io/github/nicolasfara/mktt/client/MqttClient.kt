package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.MqttException
import io.github.nicolasfara.mktt.core.NormalDisconnection
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.SubscriptionIdentifier
import io.github.nicolasfara.mktt.core.TopicAliasMaximum
import io.github.nicolasfara.mktt.core.TopicFilter
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.engine.MqttEngineConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Public MQTT client API for connecting, publishing, subscribing, and observing connection state.
 */
interface MqttClient : AutoCloseable {
    /**
     * Shared stream of incoming PUBLISH messages received from the server.
     *
     * Collectors receive messages emitted after collection starts. Use [messages] to observe only messages matching a
     * specific [TopicFilter].
     */
    val incomingPublishes: SharedFlow<MqttPublishMessage>

    /**
     * Returns the maximum QoS level allowed by the server.
     *
     * Defaults to [io.github.nicolasfara.mktt.core.QoS.EXACTLY_ONE] as long as no
     * CONNACK packet has been received.
     */
    val maxQos: QoS

    /**
     * Client identifier currently used by this client.
     *
     * Before a broker assigns an identifier, this is the configured client identifier. After a successful CONNACK with
     * an assigned client identifier, this value is the broker-assigned identifier.
     */
    val clientId: String

    /**
     * Server topic alias maximum value from CONNACK, or `0` when the server did not provide one.
     */
    val serverTopicAliasMaximum: TopicAliasMaximum

    /**
     * Value of the `Subscription Identifiers Available` CONNACK property.
     */
    val subscriptionIdentifierAvailable: Boolean

    /**
     * Value of the `Receive Maximum` CONNACK property.
     */
    val receiveMaximum: UShort

    /**
     * Value of the `Retain Available` CONNACK property.
     */
    val isRetainAvailable: Boolean

    /**
     * Value of the `Wildcard Subscription Available` CONNACK property.
     *
     * Note that this value is only reported here, the [subscribe] method merely logs a warning message if a wildcard
     * subscription is requested, when the server does not support it. The server should send DISCONNECT with
     * [io.github.nicolasfara.mktt.core.WildcardSubscriptionsNotSupported] when a wildcard subscription is requested
     * from a server that does not support it.
     */
    val isWildcardSubscriptionAvailable: Boolean

    /**
     * Value of the `Shared Subscription Available` CONNACK property.
     *
     * Note that this value is only reported here, the [subscribe] method merely logs a warning message if a shared
     * subscription is requested, when the server does not support it. The server should send DISCONNECT with
     * [io.github.nicolasfara.mktt.core.SharedSubscriptionsNotSupported] when a shared subscription is requested from a
     * server that does not support it.
     */
    val isSharedSubscriptionAvailable: Boolean

    /**
     * Value of the `Maximum Packet Size` CONNACK property.
     *
     * Note that this value is only reported here, packets are not checked for their size before being sent.
     */
    val maxPacketSize: UInt

    /**
     * Observable connection state of this MQTT client.
     *
     * [MqttConnectionState.Connected] means that transport connectivity is established and the broker accepted CONNECT
     * with a successful CONNACK.
     */
    val connectionState: StateFlow<MqttConnectionState>

    /**
     * Connects to the MQTT server and sends CONNECT.
     *
     * @param cleanStart when `true`, sets the CONNECT `Clean Start` flag and clears the
     * [io.github.nicolasfara.mktt.core.SessionStore].
     * @return the CONNACK packet returned by the broker.
     * @see connectionState
     */
    suspend fun connect(cleanStart: Boolean = true): ConnAck

    /**
     * Sends a SUBSCRIBE request to the MQTT server for the list of topics contained in [filters].
     *
     * @param filters topic filters to subscribe to.
     * @param subscriptionIdentifier an optional subscription identifier for this subscribe request. Note that a
     *        non-null value will be ignored, when the server does not support subscription identifiers.
     * @param userProperties optional user properties to include in the SUBSCRIBE packet.
     * @return the SUBACK packet if the subscribe request was answered by the server. Note that the SUBACK may still
     *         contain error messages for each of the subscribed topics.
     * @see io.github.nicolasfara.mktt.core.packet.hasFailure
     * @see subscriptionIdentifierAvailable
     */
    suspend fun subscribe(
        filters: List<TopicFilter>,
        subscriptionIdentifier: SubscriptionIdentifier? = null,
        userProperties: UserProperties = UserProperties.EMPTY,
    ): SubAck

    /**
     * Sends an UNSUBSCRIBE request to the MQTT server for the list of topic filters contained in [filters].
     *
     * @param filters the topic filters to unsubscribe from.
     * @param userProperties optional user properties to include in the UNSUBSCRIBE packet.
     * @return the UNSUBACK packet sent by the server in response to the unsubscribe request.
     */
    suspend fun unsubscribe(
        filters: List<TopicFilter>,
        userProperties: UserProperties = UserProperties.EMPTY,
    ): UnsubAck

    /**
     * Sends the specified [PublishRequest] to the server.
     *
     * If the server announced a [QoS] lower than the requested one, the published packet is automatically downgraded.
     * The actual QoS is exposed by [PublishResponse.qoS].
     *
     * When this method successfully returns, all handshake packets required by the
     * actual `QoS` will be exchanged between this client and the server. When the
     * server does not respond within [MqttClientConfig.ackMessageTimeout],
     * the result will be a failure with a
     * [io.github.nicolasfara.mktt.core.HandshakeFailedException].
     *
     * @param request publish request to send.
     * @return publish acknowledgement details for the effective QoS.
     * @throws MqttException when the publish flow fails.
     */
    suspend fun publish(request: PublishRequest): PublishResult

    /**
     * Sends a DISCONNECT packet to the server and closes the connection.
     *
     * @param reasonCode the reason code for the disconnection. Defaults to [NormalDisconnection].
     * @param reasonString an optional human-readable string describing the reason for disconnection.
     * @param sessionExpiryInterval an optional session expiry interval to override the one set in the CONNECT packet.
     */
    suspend fun disconnect(
        sessionExpiryInterval: SessionExpiryInterval? = null,
        reasonCode: DisconnectReason = NormalDisconnection,
        reasonString: String? = null,
    )

    /**
     * Returns a [Flow] of incoming PUBLISH messages whose topic matches the given [filter].
     *
     * @param filter the topic filter used to select matching incoming messages.
     * @return a filtered flow of [MqttPublishMessage] matching [filter].
     */
    fun messages(filter: TopicFilter): Flow<MqttPublishMessage>
}
