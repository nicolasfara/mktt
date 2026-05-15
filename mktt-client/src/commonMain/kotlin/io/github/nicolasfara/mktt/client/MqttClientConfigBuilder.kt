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
 * Builds an immutable [MqttClientConfig] using the provided [connectionFactory] and DSL [init] block.
 *
 * @param connectionFactory factory that creates the transport engine.
 * @param init configuration block applied to the builder.
 * @return the built immutable client configuration.
 */
fun <T : MqttEngineConfig> buildConfig(
    connectionFactory: MqttEngineFactory<T>,
    init: MqttClientConfigBuilder<T>.() -> Unit,
): MqttClientConfig = MqttClientConfigBuilder(connectionFactory).apply(init).build()

/**
 * DSL builder used to assemble [MqttClientConfig].
 */
@MqttDslMarker
class MqttClientConfigBuilder<out T : MqttEngineConfig>(private val engineFactory: MqttEngineFactory<T>) {
    private var userPropertiesBuilder: UserPropertiesBuilder? = null
    private var willMessageBuilder: WillMessageBuilder? = null
    private var connectionBlock: (T.() -> Unit)? = null

    /**
     * Internal constants used while generating default MQTT client identifiers.
     */
    companion object {
        private const val MAX_CLIENT_ID_LENGTH = 6
        private val ALLOWED_CHARS = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    }

    /**
     * Timeout for MQTT acknowledgement handshakes.
     *
     * This timeout is used while waiting for broker responses such as CONNACK, SUBACK, PUBACK, PUBREC, PUBCOMP, and
     * UNSUBACK.
     */
    var ackMessageTimeout: Duration = 7.seconds

    /**
     * MQTT client identifier.
     *
     * An empty value requests a broker-assigned client identifier when the broker supports it.
     */
    var clientId: String =
        "mktt-" + (0..MAX_CLIENT_ID_LENGTH).map { ALLOWED_CHARS.random() }.joinToString(separator = "")

    /**
     * MQTT keep-alive interval in seconds.
     *
     * A value of `0` disables keep-alive.
     */
    var keepAliveSeconds: UShort = 0u

    /**
     * Optional username included in CONNECT.
     */
    var username: String? = null

    /**
     * Optional password included in CONNECT.
     */
    var password: String? = null

    /**
     * Optional session expiry interval requested in CONNECT.
     */
    var sessionExpiryInterval: Duration? = null

    /**
     * Optional receive maximum value requested in CONNECT.
     */
    var receiveMaximum: UShort? = null

    /**
     * Optional maximum packet size requested in CONNECT.
     */
    var maximumPacketSize: UInt? = null

    /**
     * Topic alias maximum value requested in CONNECT.
     */
    var topicAliasMaximum: UShort = 0u

    /**
     * Whether CONNECT should request response information from the broker.
     */
    var requestResponseInformation: Boolean = false

    /**
     * Whether CONNECT should request problem information from the broker.
     */
    var requestProblemInformation: Boolean = true

    /**
     * Optional enhanced authentication method included in CONNECT.
     */
    var authenticationMethod: String? = null

    /**
     * Optional enhanced authentication data included in CONNECT.
     */
    var authenticationData: ByteString? = null

    /**
     * Creates a session store for each client instance.
     */
    var sessionStoreProvider: () -> SessionStore = {
        InMemorySessionStore()
    }

    /**
     * Configures the underlying transport engine.
     *
     * @param init configuration block applied to the engine-specific configuration.
     */
    fun connection(init: T.() -> Unit) {
        connectionBlock = init
    }

    /**
     * Adds CONNECT user properties.
     *
     * @param init configuration block used to add user properties.
     */
    fun userProperties(init: UserPropertiesBuilder.() -> Unit) {
        userPropertiesBuilder = UserPropertiesBuilder().apply(init)
    }

    /**
     * Configures the Last Will and Testament message for the given [topic].
     *
     * @param topic will topic name.
     * @param init configuration block applied to the will message builder.
     */
    fun willMessage(topic: String, init: WillMessageBuilder.() -> Unit) {
        willMessageBuilder = WillMessageBuilder(topic).apply(init)
    }

    /**
     * Builds an immutable [MqttClientConfig].
     *
     * @return a configuration object ready to pass to [MqttClient].
     */
    fun build(): MqttClientConfig {
        val resolvedEngine = engineFactory.create {
            connectionBlock?.invoke(this)
        }
        return MqttClientConfigImpl(
            engine = resolvedEngine,
            clientId = clientId,
            ackMessageTimeout = ackMessageTimeout,
            willMessage = willMessageBuilder?.build(),
            willQqS = willMessageBuilder?.willOqS
                ?: QoS.AT_MOST_ONCE,
            retainWillMessage = willMessageBuilder?.retainWillMessage ?: false,
            keepAliveSeconds = keepAliveSeconds,
            username = username,
            password = password,
            sessionExpiryInterval = sessionExpiryInterval?.toSessionExpiryInterval(),
            receiveMaximum = receiveMaximum?.let(::ReceiveMaximum),
            maximumPacketSize = maximumPacketSize?.let(::MaximumPacketSize),
            topicAliasMaximum = TopicAliasMaximum(topicAliasMaximum),
            requestResponseInformation = RequestResponseInformation(
                requestResponseInformation,
            ),
            requestProblemInformation = RequestProblemInformation(
                requestProblemInformation,
            ),
            authenticationMethod = authenticationMethod?.let(::AuthenticationMethod),
            authenticationData = authenticationData?.let(::AuthenticationData),
            userProperties = userPropertiesBuilder?.build()
                ?: UserProperties.EMPTY,
            sessionStoreProvider = sessionStoreProvider,
        )
    }
}

private data class MqttClientConfigImpl(
    override val engine: MqttEngine,
    override val clientId: String,
    override val ackMessageTimeout: Duration,
    override val willMessage: WillMessage?,
    override val willQqS: QoS,
    override val retainWillMessage: Boolean,
    override val keepAliveSeconds: UShort,
    override val username: String?,
    override val password: String?,
    override val sessionExpiryInterval: SessionExpiryInterval?,
    override val receiveMaximum: ReceiveMaximum?,
    override val maximumPacketSize: MaximumPacketSize?,
    override val topicAliasMaximum: TopicAliasMaximum,
    override val requestResponseInformation: RequestResponseInformation,
    override val requestProblemInformation: RequestProblemInformation,
    override val authenticationMethod: AuthenticationMethod?,
    override val authenticationData: AuthenticationData?,
    override val userProperties: UserProperties,
    override val sessionStoreProvider: () -> SessionStore,
) : MqttClientConfig
