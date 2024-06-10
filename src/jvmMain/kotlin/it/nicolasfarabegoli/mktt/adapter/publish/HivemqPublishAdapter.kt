package it.nicolasfarabegoli.mktt.adapter.publish

import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult
import it.nicolasfarabegoli.mktt.adapter.HivemqAdapter.toHiveMqttQos
import it.nicolasfarabegoli.mktt.adapter.publish.puback.HivemqPubAckAdapter.toMqtt
import it.nicolasfarabegoli.mktt.adapter.publish.pubrec.HivemqPubRecAdapter.toMqtt
import it.nicolasfarabegoli.mktt.adapter.topic.HivemqTopicAdapter.toMqtt
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.message.publish.MqttPublishResult
import it.nicolasfarabegoli.mktt.utils.JavaKotlinUtils.toByteArray
import it.nicolasfarabegoli.mktt.utils.JavaKotlinUtils.toLongOrNull
import kotlin.jvm.optionals.getOrNull

object HivemqPublishAdapter {
    fun Mqtt5Publish.toMqtt(): MqttPublish {
        return MqttPublish(
            qos = MqttQoS.fromCode(qos.code),
            topic = topic.toMqtt(),
            payload = payload.getOrNull()?.toByteArray(),
            isRetain = isRetain,
            expiryInterval = messageExpiryInterval.toLongOrNull(),
            contentType = contentType.getOrNull()?.toString(),
            responseTopic = responseTopic.getOrNull()?.let {
                val topic = it.toByteBuffer().toByteArray().decodeToString()
                if (topic != "null") it.toMqtt() else null // Workaround for: https://github.com/hivemq/hivemq-mqtt-client/issues/632
            },
            correlationData = correlationData.getOrNull()?.array(),
        )
    }

    fun MqttPublish.toHivemqMqtt(): Mqtt5Publish {
        return Mqtt5Publish.builder()
            .topic(topic.toString())
            .qos(qos.toHiveMqttQos())
            .payload(payload)
            .retain(isRetain)
            .contentType(contentType)
            .responseTopic(responseTopic.toString())
            .correlationData(correlationData)
            .build()
    }

    fun Mqtt5PublishResult.toMqtt(): MqttPublishResult {
        return when (this) {
            is Mqtt5PublishResult.Mqtt5Qos1Result -> MqttPublishResult.MqttQoS1Result(
                publish = this.publish.toMqtt(),
                error = this.error.getOrNull(),
                pubAck = this.pubAck.toMqtt(),
            )
            is Mqtt5PublishResult.Mqtt5Qos2Result -> MqttPublishResult.MqttQoS2hResult(
                publish = this.publish.toMqtt(),
                error = this.error.getOrNull(),
                pubRec = this.pubRec.toMqtt(),
            )
            else -> error("Unknown ${this::class} type")
        }
    }
}
