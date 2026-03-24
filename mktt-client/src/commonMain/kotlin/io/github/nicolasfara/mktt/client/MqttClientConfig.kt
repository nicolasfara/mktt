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

public interface MqttClientConfig {
    public val engine: io.github.nicolasfara.mktt.client.MqttEngine
    public val dispatcher: CoroutineDispatcher
    public val clientId: String
    public val ackMessageTimeout: Duration
    public val willMessage: io.github.nicolasfara.mktt.core.WillMessage?
    public val willQqS: io.github.nicolasfara.mktt.core.QoS
    public val retainWillMessage: Boolean
    public val keepAliveSeconds: UShort
    public val username: String?
    public val password: String?
    public val sessionExpiryInterval: io.github.nicolasfara.mktt.core.SessionExpiryInterval?
    public val receiveMaximum: io.github.nicolasfara.mktt.core.ReceiveMaximum?
    public val maximumPacketSize: io.github.nicolasfara.mktt.core.MaximumPacketSize?
    public val topicAliasMaximum: io.github.nicolasfara.mktt.core.TopicAliasMaximum
    public val requestResponseInformation: io.github.nicolasfara.mktt.core.RequestResponseInformation
    public val requestProblemInformation: io.github.nicolasfara.mktt.core.RequestProblemInformation
    public val authenticationMethod: io.github.nicolasfara.mktt.core.AuthenticationMethod?
    public val authenticationData: io.github.nicolasfara.mktt.core.AuthenticationData?
    public val userProperties: io.github.nicolasfara.mktt.core.UserProperties
    public val sessionStoreProvider: () -> io.github.nicolasfara.mktt.core.SessionStore
}

public fun <T : io.github.nicolasfara.mktt.client.MqttEngineConfig> buildConfig(
    connectionFactory: io.github.nicolasfara.mktt.client.MqttEngineFactory<T>,
    init: io.github.nicolasfara.mktt.client.MqttClientConfigBuilder<T>.() -> Unit,
): io.github.nicolasfara.mktt.client.MqttClientConfig =
    _root_ide_package_.io.github.nicolasfara.mktt.client.MqttClientConfigBuilder(
        connectionFactory,
    ).apply(init).build()

@io.github.nicolasfara.mktt.core.util.MqttDslMarker
public class MqttClientConfigBuilder<out T : io.github.nicolasfara.mktt.client.MqttEngineConfig>(
    private val engineFactory: io.github.nicolasfara.mktt.client.MqttEngineFactory<T>,
) {
    private var userPropertiesBuilder: io.github.nicolasfara.mktt.core.UserPropertiesBuilder? = null
    private var willMessageBuilder: io.github.nicolasfara.mktt.core.WillMessageBuilder? = null
    private var engine: io.github.nicolasfara.mktt.client.MqttEngine? = null

    public var dispatcher: CoroutineDispatcher = Dispatchers.Default
    public var ackMessageTimeout: Duration = 7.seconds
    public var clientId: String = ""
    public var keepAliveSeconds: UShort = 0u
    public var username: String? = null
    public var password: String? = null
    public var sessionExpiryInterval: Duration? = null
    public var receiveMaximum: UShort? = null
    public var maximumPacketSize: UInt? = null
    public var topicAliasMaximum: UShort = 0u
    public var requestResponseInformation: Boolean = false
    public var requestProblemInformation: Boolean = true
    public var authenticationMethod: String? = null
    public var authenticationData: ByteString? = null
    public var sessionStoreProvider: () -> io.github.nicolasfara.mktt.core.SessionStore = {
        _root_ide_package_.io.github.nicolasfara.mktt.client.InMemorySessionStore()
    }

    public fun connection(init: T.() -> Unit) {
        engine = engineFactory.create(init)
    }

    public fun userProperties(init: io.github.nicolasfara.mktt.core.UserPropertiesBuilder.() -> Unit) {
        userPropertiesBuilder = _root_ide_package_.io.github.nicolasfara.mktt.core.UserPropertiesBuilder().apply(init)
    }

    public fun willMessage(topic: String, init: io.github.nicolasfara.mktt.core.WillMessageBuilder.() -> Unit) {
        willMessageBuilder = _root_ide_package_.io.github.nicolasfara.mktt.core.WillMessageBuilder(topic).apply(init)
    }

    public fun build(): io.github.nicolasfara.mktt.client.MqttClientConfig {
        val resolvedEngine = engine ?: engineFactory.create { }
        return _root_ide_package_.io.github.nicolasfara.mktt.client.MqttClientConfigImpl(
            engine = resolvedEngine,
            dispatcher = dispatcher,
            clientId = clientId,
            ackMessageTimeout = ackMessageTimeout,
            willMessage = willMessageBuilder?.build(),
            willQqS = willMessageBuilder?.willOqS
                ?: _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE,
            retainWillMessage = willMessageBuilder?.retainWillMessage ?: false,
            keepAliveSeconds = keepAliveSeconds,
            username = username,
            password = password,
            sessionExpiryInterval = sessionExpiryInterval?.toSessionExpiryInterval(),
            receiveMaximum = receiveMaximum?.let(::ReceiveMaximum),
            maximumPacketSize = maximumPacketSize?.let(::MaximumPacketSize),
            topicAliasMaximum = _root_ide_package_.io.github.nicolasfara.mktt.core.TopicAliasMaximum(topicAliasMaximum),
            requestResponseInformation = _root_ide_package_.io.github.nicolasfara.mktt.core.RequestResponseInformation(
                requestResponseInformation,
            ),
            requestProblemInformation = _root_ide_package_.io.github.nicolasfara.mktt.core.RequestProblemInformation(
                requestProblemInformation,
            ),
            authenticationMethod = authenticationMethod?.let(::AuthenticationMethod),
            authenticationData = authenticationData?.let(::AuthenticationData),
            userProperties = userPropertiesBuilder?.build()
                ?: _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
            sessionStoreProvider = sessionStoreProvider,
        )
    }
}

private data class MqttClientConfigImpl(
    override val engine: io.github.nicolasfara.mktt.client.MqttEngine,
    override val dispatcher: CoroutineDispatcher,
    override val clientId: String,
    override val ackMessageTimeout: Duration,
    override val willMessage: io.github.nicolasfara.mktt.core.WillMessage?,
    override val willQqS: io.github.nicolasfara.mktt.core.QoS,
    override val retainWillMessage: Boolean,
    override val keepAliveSeconds: UShort,
    override val username: String?,
    override val password: String?,
    override val sessionExpiryInterval: io.github.nicolasfara.mktt.core.SessionExpiryInterval?,
    override val receiveMaximum: io.github.nicolasfara.mktt.core.ReceiveMaximum?,
    override val maximumPacketSize: io.github.nicolasfara.mktt.core.MaximumPacketSize?,
    override val topicAliasMaximum: io.github.nicolasfara.mktt.core.TopicAliasMaximum,
    override val requestResponseInformation: io.github.nicolasfara.mktt.core.RequestResponseInformation,
    override val requestProblemInformation: io.github.nicolasfara.mktt.core.RequestProblemInformation,
    override val authenticationMethod: io.github.nicolasfara.mktt.core.AuthenticationMethod?,
    override val authenticationData: io.github.nicolasfara.mktt.core.AuthenticationData?,
    override val userProperties: io.github.nicolasfara.mktt.core.UserProperties,
    override val sessionStoreProvider: () -> io.github.nicolasfara.mktt.core.SessionStore,
) : io.github.nicolasfara.mktt.client.MqttClientConfig
