package it.nicolasfarabegoli.mktt.message.publish

import it.nicolasfarabegoli.mktt.message.publish.puback.MqttPubAck
import it.nicolasfarabegoli.mktt.message.publish.pubrec.MqttPubRec

sealed interface MqttPublishResult {
    val publish: MqttPublish
    val error: Throwable?

    companion object {
        operator fun invoke(
            publish: MqttPublish,
            error: Throwable?,
        ): MqttPublishResult = MqttResultImpl(publish, error)

        private data class MqttResultImpl(
            override val publish: MqttPublish,
            override val error: Throwable?,
        ) : MqttPublishResult
    }

    data class MqttQoS1Result(
        override val publish: MqttPublish,
        override val error: Throwable?,
        val pubAck: MqttPubAck,
    ) : MqttPublishResult

    data class MqttQoS2hResult(
        override val publish: MqttPublish,
        override val error: Throwable?,
        val pubRec: MqttPubRec,
    ) : MqttPublishResult
}
