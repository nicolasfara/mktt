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
     * A shared flow of all incoming PUBLISH messages received from the server.
     * Subscribers will receive every message that arrives after they start collecting.
     * Use [messages] to filter by a specific [io.github.nicolasfara.mktt.core.TopicFilter].
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
     * The ID of this client or the value of the assigned client ID of the [io.github.nicolasfara.mktt.core.packet.Connack]
     * packet.
     */
    val clientId: String

    /**
     * The server topic alias maximum value from the CONNACK message (or the default value of 0).
     */
    val serverTopicAliasMaximum: TopicAliasMaximum

    /**
     * The value of 'Subscription Identifiers Available' from the CONNACK message of the server.
     */
    val subscriptionIdentifierAvailable: Boolean

    /**
     * The value of 'Receive Maximum' from the CONNACK message of the server.
     */
    val receiveMaximum: UShort

    /**
     * The value of 'Retain Available' from the CONNACK message of the server.
     */
    val isRetainAvailable: Boolean

    /**
     * The value of 'Wildcard Subscription Available' from the CONNACK message of the server.
     *
     * Note that this value is only reported here, the [subscribe] method merely logs a warning message if a wildcard
     * subscription is requested, when the server does not support it. The server should send a DISCONNECT with reason
     * [WildcardSubscriptionsNotSupported] when a wild card subscription was requested for a server who is not
     * supporting it.
     */
    val isWildcardSubscriptionAvailable: Boolean

    /**
     * The value of 'Shared Subscription Available' from the CONNACK message of the server.
     *
     * Note that this value is only reported here, the [subscribe] method merely logs a warning message if a shared
     * subscription is requested, when the server does not support it. The server should send a DISCONNECT with reason
     * [SharedSubscriptionsNotSupported] when a wild card subscription was requested for a server who is not supporting
     * it.
     */
    val isSharedSubscriptionAvailable: Boolean

    /**
     * The value of 'Maximum Packet Size' from the CONNACK message of the server.
     *
     * Note that this value is only reported here, packets are not checked for their size before being sent.
     */
    val maxPacketSize: UInt

    /**
     * Provides the connection state of this MQTT client. When the state is
     * [MqttConnectionState.Connected] this implies that an IP
     * connectivity has been established AND that the server responded with a success CONNACK message.
     */
    val connectionState: StateFlow<MqttConnectionState>

    /**
     * Tries to connect to the MQTT server and send a CONNECT message.
     *
     * @param cleanStart when set to `true` the `Clean Start` flag in the CONNACK packet will be set to `1`. Also, the
     *        [io.github.nicolasfara.mktt.core.SessionStore] is cleared.
     * @return the connection result. Note that even when the result returns a Connack packet, the client may still not
     *         be successfully connected, as the server may send a CONNACK with an error message.
     * @see connectionState
     */
    suspend fun connect(cleanStart: Boolean = true): ConnAck

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
     * Sends the specified [io.github.nicolasfara.mktt.client.PublishRequest] to the server.
     *
     * If the server announced a [io.github.nicolasfara.mktt.core.QoS] value lower
     * than the one requested, the QoS of the published packet will be
     * automatically downgraded. The actual QoS can be determined from either
     * [maxQos] or [qoS].
     *
     * When this method successfully returns, all handshake packets required by the
     * actual `QoS` will be exchanged between this client and the server. When the
     * server does not respond within
     * [ackMessageTimeout][de.kempmobil.ktor.mqtt.MqttClientConfigBuilder.ackMessageTimeout],
     * the result will be a failure with a
     * [io.github.nicolasfara.mktt.core.HandshakeFailedException].
     *
     * All returned exceptions are of type [MqttException] resp. its subtypes.
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
