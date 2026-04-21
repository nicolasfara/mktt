package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.TopicAliasMaximum
import io.github.nicolasfara.mktt.core.isAvailable
import io.github.nicolasfara.mktt.core.packet.Connack

internal class DefaultMqttClientCapabilities(initialClientId: String) {
    var maxQos: QoS = QoS.EXACTLY_ONE
        private set

    var clientId: String = initialClientId
        private set

    var serverTopicAliasMaximum: TopicAliasMaximum = TopicAliasMaximum(0u)
        private set

    var subscriptionIdentifierAvailable: Boolean = true
        private set

    var receiveMaximum: UShort = UShort.MAX_VALUE
        private set

    var retainAvailable: Boolean = true
        private set

    var wildcardSubscriptionAvailable: Boolean = true
        private set

    var sharedSubscriptionAvailable: Boolean = true
        private set

    var maxPacketSize: UInt = UInt.MAX_VALUE
        private set

    suspend fun updateFrom(
        connack: Connack,
        configuredClientId: String,
        updateReceiveMaximum: suspend (UShort) -> Unit,
    ) {
        connack.maximumQoS?.let {
            maxQos = it.qoS
        }
        serverTopicAliasMaximum = connack.topicAliasMaximum ?: TopicAliasMaximum(0u)

        if (configuredClientId.isEmpty()) {
            connack.assignedClientIdentifier?.let { clientId = it.value }
        }

        subscriptionIdentifierAvailable = connack.subscriptionIdentifierAvailable.isAvailable()
        val nextReceiveMaximum = connack.receiveMaximum?.value ?: UShort.MAX_VALUE
        updateReceiveMaximum(nextReceiveMaximum)
        receiveMaximum = nextReceiveMaximum
        retainAvailable = connack.retainAvailable?.value ?: true
        wildcardSubscriptionAvailable = connack.wildcardSubscriptionAvailable?.value ?: true
        sharedSubscriptionAvailable = connack.sharedSubscriptionAvailable?.value ?: true
        maxPacketSize = connack.maximumPacketSize?.value ?: UInt.MAX_VALUE
    }
}
