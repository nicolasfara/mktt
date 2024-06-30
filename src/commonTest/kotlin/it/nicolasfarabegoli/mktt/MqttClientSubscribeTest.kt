package it.nicolasfarabegoli.mktt

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.MqttQoS
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.topic.MqttTopic.Companion.asTopic
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MqttClientSubscribeTest : FreeSpec({
    coroutineTestScope = true
    "The client should subscribe successfully to a topic" {
        val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "test.mosquitto.org"), dispatcher)
        shouldNotThrow<Exception> {
            mqttClient.connect().reasonCode shouldBe MqttConnAckReasonCode.Success
            val filterTopic = MqttTopicFilter.of("test/topic")
            mqttClient.subscribe(filterTopic)
        }
    }
    "The client should subscribe to a topic and start collecting the messages" {
        val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
        val client = MqttClient(MqttConfiguration(hostname = "test.mosquitto.org"), dispatcher)
        shouldNotThrow<Exception> {
            client.connect().reasonCode shouldBe MqttConnAckReasonCode.Success
            val filterTopic = MqttTopicFilter.of("test/topic")
            val job = launch(UnconfinedTestDispatcher(testCoroutineScheduler)) {
                client.subscribe(filterTopic, qoS = MqttQoS.ExactlyOnce).take(1).collect {
                    println("Received message: $it")
                    it.payload shouldBe "hello".encodeToByteArray()
                }
            }
            val message = MqttPublish(
                topic = "test/topic".asTopic(),
                payload = "test message".encodeToByteArray(),
                qos = MqttQoS.ExactlyOnce,
            )
            client.publish(message).error shouldBe null
            job.join()
        }
    }
})
