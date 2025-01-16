package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.topic.MqttTopicFilter
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class MqttClientSubscribeTest {
    @Test
    @Ignore
    fun `The client should subscribe successfully to a topic`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
        assertEquals(MqttConnAckReasonCode.Success, mqttClient.connect().reasonCode)
        val filterTopic = MqttTopicFilter.of("test/topic")
        mqttClient.subscribe(filterTopic)
    }
    //    "The client should subscribe to a topic and start collecting the messages".config(enabled = false) {
    //        val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
    //        val client = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
    //        shouldNotThrow<Exception> {
    //            client.connect().reasonCode shouldBe MqttConnAckReasonCode.Success
    //            val filterTopic = MqttTopicFilter.of("test/topic")
    //            val job = launch(UnconfinedTestDispatcher(testCoroutineScheduler)) {
    //                client.subscribe(filterTopic, qoS = MqttQoS.ExactlyOnce).take(1).collect {
    //                    it.payload?.decodeToString() shouldBe "test message"
    //                }
    //            }
    //            val message = MqttPublish(
    //                topic = "test/topic".asTopic(),
    //                payload = "test message".encodeToByteArray(),
    //                qos = MqttQoS.ExactlyOnce,
    //            )
    //            val sendJob = launch(UnconfinedTestDispatcher(testCoroutineScheduler)) {
    //                client.publish(message).error shouldBe null
    //            }
    //            sendJob.join()
    //            job.join()
    //        }
    //    }
}
