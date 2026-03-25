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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.io.bytestring.ByteString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface MqttClientConfig {
    val engine: MqttEngine
    val dispatcher: CoroutineDispatcher
    val clientId: String
    val ackMessageTimeout: Duration
    val willMessage: WillMessage?
    val willQqS: QoS
    val retainWillMessage: Boolean
    val keepAliveSeconds: UShort
    val username: String?
    val password: String?
    val sessionExpiryInterval: SessionExpiryInterval?
    val receiveMaximum: ReceiveMaximum?
    val maximumPacketSize: MaximumPacketSize?
    val topicAliasMaximum: TopicAliasMaximum
    val requestResponseInformation: RequestResponseInformation
    val requestProblemInformation: RequestProblemInformation
    val authenticationMethod: AuthenticationMethod?
    val authenticationData: AuthenticationData?
    val userProperties: UserProperties
    val sessionStoreProvider: () -> SessionStore
}

fun <T : MqttEngineConfig> buildConfig(
    connectionFactory: MqttEngineFactory<T>,
    init: MqttClientConfigBuilder<T>.() -> Unit,
): MqttClientConfig = MqttClientConfigBuilder(
    connectionFactory,
).apply(init).build()

@MqttDslMarker
class MqttClientConfigBuilder<out T : MqttEngineConfig>(private val engineFactory: MqttEngineFactory<T>) {
    private var userPropertiesBuilder: UserPropertiesBuilder? = null
    private var willMessageBuilder: WillMessageBuilder? = null
    private var engine: MqttEngine? = null

    var dispatcher: CoroutineDispatcher = Dispatchers.Default
    var ackMessageTimeout: Duration = 7.seconds
    var clientId: String = ""
    var keepAliveSeconds: UShort = 0u
    var username: String? = null
    var password: String? = null
    var sessionExpiryInterval: Duration? = null
    var receiveMaximum: UShort? = null
    var maximumPacketSize: UInt? = null
    var topicAliasMaximum: UShort = 0u
    var requestResponseInformation: Boolean = false
    var requestProblemInformation: Boolean = true
    var authenticationMethod: String? = null
    var authenticationData: ByteString? = null
    var sessionStoreProvider: () -> SessionStore = {
        InMemorySessionStore()
    }

    fun connection(init: T.() -> Unit) {
        engine = engineFactory.create(init)
    }

    fun userProperties(init: UserPropertiesBuilder.() -> Unit) {
        userPropertiesBuilder = UserPropertiesBuilder().apply(init)
    }

    fun willMessage(topic: String, init: WillMessageBuilder.() -> Unit) {
        willMessageBuilder = WillMessageBuilder(topic).apply(init)
    }

    fun build(): MqttClientConfig {
        val resolvedEngine = engine ?: engineFactory.create { }
        return MqttClientConfigImpl(
            engine = resolvedEngine,
            dispatcher = dispatcher,
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
    override val dispatcher: CoroutineDispatcher,
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
