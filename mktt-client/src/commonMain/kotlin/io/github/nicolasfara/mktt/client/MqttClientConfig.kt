package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.AuthenticationData
import io.github.nicolasfara.mktt.core.AuthenticationMethod
import io.github.nicolasfara.mktt.core.MaximumPacketSize
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.ReceiveMaximum
import io.github.nicolasfara.mktt.core.RequestProblemInformation
import io.github.nicolasfara.mktt.core.RequestResponseInformation
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.SessionStore
import io.github.nicolasfara.mktt.core.TopicAliasMaximum
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.UserPropertiesBuilder
import io.github.nicolasfara.mktt.core.WillMessage
import io.github.nicolasfara.mktt.core.WillMessageBuilder
import io.github.nicolasfara.mktt.core.toSessionExpiryInterval
import io.github.nicolasfara.mktt.core.util.MqttDslMarker
import io.github.nicolasfara.mktt.engine.MqttEngine
import io.github.nicolasfara.mktt.engine.MqttEngineConfig
import io.github.nicolasfara.mktt.engine.MqttEngineFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.io.bytestring.ByteString

/**
 * Immutable MQTT client configuration used by [MqttClient].
 */
interface MqttClientConfig {
    /**
     * Engine responsible for socket I/O.
     */
    val engine: MqttEngine

    /**
     * MQTT client identifier.
     */
    val clientId: String

    /**
     * Timeout used while waiting for acknowledgment packets.
     */
    val ackMessageTimeout: Duration

    /**
     * Optional Last Will and Testament message.
     */
    val willMessage: WillMessage?

    /**
     * QoS used for the will message.
     */
    val willQqS: QoS

    /**
     * Whether the will message is sent as retained.
     */
    val retainWillMessage: Boolean

    /**
     * Keep-alive interval in seconds.
     */
    val keepAliveSeconds: UShort

    /**
     * Optional username for authentication.
     */
    val username: String?

    /**
     * Optional password for authentication.
     */
    val password: String?

    /**
     * Session expiry interval advertised in CONNECT.
     */
    val sessionExpiryInterval: SessionExpiryInterval?

    /**
     * Optional receive maximum advertised in CONNECT.
     */
    val receiveMaximum: ReceiveMaximum?

    /**
     * Optional maximum packet size advertised in CONNECT.
     */
    val maximumPacketSize: MaximumPacketSize?

    /**
     * Topic alias maximum advertised in CONNECT.
     */
    val topicAliasMaximum: TopicAliasMaximum

    /**
     * Whether response information is requested from the server.
     */
    val requestResponseInformation: RequestResponseInformation

    /**
     * Whether problem information is requested from the server.
     */
    val requestProblemInformation: RequestProblemInformation

    /**
     * Optional enhanced authentication method.
     */
    val authenticationMethod: AuthenticationMethod?

    /**
     * Optional enhanced authentication data.
     */
    val authenticationData: AuthenticationData?

    /**
     * User properties included in CONNECT.
     */
    val userProperties: UserProperties

    /**
     * Provider used to allocate a new session store instance.
     */
    val sessionStoreProvider: () -> SessionStore
}
