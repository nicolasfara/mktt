package it.nicolasfarabegoli.mktt.message.publish

import it.nicolasfarabegoli.mktt.message.publish.puback.MqttPubAck
import it.nicolasfarabegoli.mktt.message.publish.pubrec.MqttPubRec

/**
 * Represents the result of a publish operation.
 */
sealed interface MqttPublishResult {
    /**
     * The [MqttPublish] that was published.
     */
    val publish: MqttPublish

    /**
     * The error that occurred during the publish operation, if any.
     */
    val error: Throwable?

    /**
     * Represents the result of a QoS 1 publish operation.
     */
    companion object {
        /**
         * Creates a new [MqttQoS1Result] with the given [publish], [error].
         */
        operator fun invoke(
            publish: MqttPublish,
            error: Throwable?,
        ): MqttPublishResult = MqttResultImpl(publish, error)

        private data class MqttResultImpl(
            override val publish: MqttPublish,
            override val error: Throwable?,
        ) : MqttPublishResult
    }

    /**
     * Represents the result of a QoS 1 publish operation with a [pubAck].
     */
    data class MqttQoS1Result(
        override val publish: MqttPublish,
        override val error: Throwable?,
        val pubAck: MqttPubAck,
    ) : MqttPublishResult

    /**
     * Represents the result of a QoS 2 publish operation with a [pubRec].
     */
    data class MqttQoS2hResult(
        override val publish: MqttPublish,
        override val error: Throwable?,
        val pubRec: MqttPubRec,
    ) : MqttPublishResult
}
