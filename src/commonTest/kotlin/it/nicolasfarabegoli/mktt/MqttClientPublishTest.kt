package it.nicolasfarabegoli.mktt

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.topic.MqttTopic.Companion.asTopic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MqttClientPublishTest : FreeSpec({
    coroutineTestScope = true
    "The client should publish successfully a message" {
        val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "test.mosquitto.org"), dispatcher)
        shouldNotThrow<Exception> {
            mqttClient.connect().reasonCode shouldBe MqttConnAckReasonCode.Success
            val messageResult = mqttClient.publish(
                byteArrayOf(0x00),
                "test/topic".asTopic(),
                qoS = MqttQoS.ExactlyOnce
            )
            messageResult.error shouldBe null
            messageResult.publish.topic shouldBe "test/topic".asTopic()
            messageResult.publish.qos shouldBe MqttQoS.ExactlyOnce
            messageResult.publish.payload shouldBe byteArrayOf(0x00)
            mqttClient.disconnect()
        }
    }
    "When multiple messages will be sent, a flow containing the successfully sent message should be returned" {
        val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "test.mosquitto.org"), dispatcher)
        val messages = (0..9).map {
            MqttPublish(topic = "test/topic".asTopic(), payload = byteArrayOf(it.toByte()), qos = MqttQoS.ExactlyOnce)
        }.asFlow()
        shouldNotThrow<Exception> {
            mqttClient.connect().reasonCode shouldBe MqttConnAckReasonCode.Success
            val responses = mqttClient.publish(messages)
            responses.toList().size shouldBe 10
            responses.map { it.publish.payload }.toList() shouldBe (0..9).map { byteArrayOf(it.toByte()) }
            mqttClient.disconnect()
        }
    }
    "The client should fail with an exception when publishing and the client is not connected" {
        val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "test.mosquitto.org"), dispatcher)
        shouldThrow<Exception> {
            mqttClient.publish(
                byteArrayOf(0x00),
                "test/topic".asTopic(),
                qoS = MqttQoS.ExactlyOnce
            )
        }
    }
})
