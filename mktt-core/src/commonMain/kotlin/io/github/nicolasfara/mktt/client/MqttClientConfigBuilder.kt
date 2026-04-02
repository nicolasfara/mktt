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
     * Timeout for handshake acknowledgments.
     */
    var ackMessageTimeout: Duration = 7.seconds

    /**
     * MQTT client identifier. Empty means broker-assigned when supported.
     */
    var clientId: String = ""

    /**
     * Keep-alive interval in seconds.
     */
    var keepAliveSeconds: UShort = 0u

    /**
     * Optional user name for authentication.
     */
    var username: String? = null

    /**
     * Optional password for authentication.
     */
    var password: String? = null

    /**
     * Optional session expiry interval.
     */
    var sessionExpiryInterval: Duration? = null

    /**
     * Optional receive maximum value.
     */
    var receiveMaximum: UShort? = null

    /**
     * Optional maximum packet size value.
     */
    var maximumPacketSize: UInt? = null

    /**
     * Topic alias maximum value.
     */
    var topicAliasMaximum: UShort = 0u

    /**
     * Whether to request response information from the server.
     */
    var requestResponseInformation: Boolean = false

    /**
     * Whether to request problem information from the server.
     */
    var requestProblemInformation: Boolean = true

    /**
     * Optional enhanced authentication method.
     */
    var authenticationMethod: String? = null

    /**
     * Optional enhanced authentication data.
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
     */
    fun connection(init: T.() -> Unit) {
        connectionBlock = init
    }

    /**
     * Adds CONNECT user properties.
     */
    fun userProperties(init: UserPropertiesBuilder.() -> Unit) {
        userPropertiesBuilder = UserPropertiesBuilder().apply(init)
    }

    /**
     * Configures the Last Will and Testament message for the given [topic].
     */
    fun willMessage(topic: String, init: WillMessageBuilder.() -> Unit) {
        willMessageBuilder = WillMessageBuilder(topic).apply(init)
    }

    /**
     * Builds an immutable [MqttClientConfig].
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
