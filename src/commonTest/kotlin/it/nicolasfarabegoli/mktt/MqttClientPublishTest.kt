package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttTestConfiguration.connectionConfiguration
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MqttClientPublishTest {
    @Test
    fun `The client should publish successfully a message`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        mqttClient.publish(
            "test/topic",
            "hello world".encodeToByteArray(),
            MqttQoS.ExactlyOnce,
        )
        mqttClient.disconnect()
    }

//    @Test
//    fun `When multiple messages will be sent, a flow containing the successfully sent message should be returned`() =
//        runTest {
//            val dispatcher = StandardTestDispatcher(testScheduler)
//            val mqttClient = MqttClient(dispatcher, connectionConfiguration)
//            val messages = (0..9).map {
//                MqttPublish(
//                    topic = "test/topic".asTopic(),
//                    payload = it.toString().encodeToByteArray(),
//                    qos = MqttQoS.ExactlyOnce,
//                )
//            }.asFlow()
//            assertEquals(mqttClient.connect().reasonCode, MqttConnAckReasonCode.Success)
//            mqttClient.publish(messages)
//            mqttClient.disconnect()
//        }

    @Test
    fun `The client should fail with an exception when publishing and the client is not connected`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        assertFailsWith<Throwable> {
            mqttClient.publish("test/topic", "hello world".encodeToByteArray())
        }
    }
}
