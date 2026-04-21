package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.SubscriptionIdentifier
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.TopicAliasException
import io.github.nicolasfara.mktt.core.TopicFilter
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.packet.Connect
import io.github.nicolasfara.mktt.core.packet.Disconnect
import io.github.nicolasfara.mktt.core.packet.Publish
import io.github.nicolasfara.mktt.core.packet.Subscribe
import io.github.nicolasfara.mktt.core.packet.Unsubscribe
import io.github.nicolasfara.mktt.core.toReasonString

internal object DefaultMqttClientPackets {
    fun createConnect(config: MqttClientConfig, cleanStart: Boolean): Connect = Connect(
        isCleanStart = cleanStart,
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

    fun createSubscribe(
        filters: List<TopicFilter>,
        subscriptionIdentifier: SubscriptionIdentifier?,
        userProperties: UserProperties,
        nextPacketIdentifier: () -> UShort,
    ): Subscribe = Subscribe(
        packetIdentifier = nextPacketIdentifier(),
        filters = filters,
        subscriptionIdentifier = subscriptionIdentifier,
        userProperties = userProperties,
    )

    fun createUnsubscribe(
        topics: List<Topic>,
        userProperties: UserProperties,
        nextPacketIdentifier: () -> UShort,
    ): Unsubscribe = Unsubscribe(
        packetIdentifier = nextPacketIdentifier(),
        topics = topics,
        userProperties = userProperties,
    )

    fun createPublish(
        request: PublishRequest,
        capabilities: DefaultMqttClientCapabilities,
        nextPacketIdentifier: () -> UShort,
        isDupMessage: Boolean = false,
    ): Result<Publish> {
        if (request.topicAlias != null && request.topicAlias.value > capabilities.serverTopicAliasMaximum.value) {
            return Result.failure(
                TopicAliasException(
                    "Server maximum topic alias is: ${capabilities.serverTopicAliasMaximum}, " +
                        "but you requested: ${request.topicAlias}",
                ),
            )
        }

        if (request.isRetainMessage && !capabilities.retainAvailable) {
            return Result.failure(IllegalArgumentException("Server does not support retained messages"))
        }

        val actualQoS = request.desiredQoS.coerceAtMost(capabilities.maxQos)
        val isAtMostOnce = actualQoS == QoS.AT_MOST_ONCE

        return Result.success(
            Publish(
                isDupMessage = if (isAtMostOnce) false else isDupMessage,
                qoS = actualQoS,
                isRetainMessage = request.isRetainMessage,
                packetIdentifier = if (isAtMostOnce) null else nextPacketIdentifier(),
                topic = request.topic,
                payloadFormatIndicator = request.payloadFormatIndicator,
                messageExpiryInterval = request.messageExpiryInterval,
                topicAlias = request.topicAlias,
                responseTopic = request.responseTopic,
                correlationData = request.correlationData,
                userProperties = request.userProperties,
                subscriptionIdentifier = null,
                contentType = request.contentType,
                payload = request.payloadAsByteString(),
            ),
        )
    }

    fun createDisconnect(
        reasonCode: ReasonCode,
        reason: String?,
        sessionExpiryInterval: SessionExpiryInterval?,
    ): Disconnect = Disconnect(
        reason = reasonCode,
        sessionExpiryInterval = sessionExpiryInterval,
        reasonString = reason.toReasonString(),
    )
}
