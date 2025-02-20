package it.nicolasfarabegoli.mktt

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.shouldBe
import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MqttClientTest :
    FreeSpec({
        coroutineTestScope = true
        "The client should be able to connect and disconnect from the broker" {
            val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
            val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
            shouldNotThrow<Exception> {
                mqttClient.connect().reasonCode shouldBe MqttConnAckReasonCode.Success
                mqttClient.disconnect()
            }
        }
        "The client should fail with an exception when connecting to an invalid broker" {
            val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
            val mqttClient = MqttClient(MqttConfiguration(hostname = "invalid.broker"), dispatcher)
            shouldThrowUnit<Exception> {
                mqttClient.connect()
            }
        }
//    "The client should fail with an exception when connecting to a valid broker using an invalid port" {
//        val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
//        val mqttClient = MqttClient(MqttConfiguration(hostname = "test.mosquitto.org", port = 1234), dispatcher)
//        shouldThrowUnit<Exception> {
//            mqttClient.connect()
//        }
//    }
        "The client should fail with an exception when disconnecting without connecting" {
            val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
            val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
            shouldThrowUnit<Exception> {
                mqttClient.disconnect()
            }
        }
        "The client should fail with an exception when disconnecting twice" {
            val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
            val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
            shouldNotThrow<Exception> {
                mqttClient.connect().reasonCode shouldBe MqttConnAckReasonCode.Success
                mqttClient.disconnect()
            }
            shouldThrowUnit<Exception> {
                mqttClient.disconnect()
            }
        }
        "The client should fail with an exception when connecting twice" {
            val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
            val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), dispatcher)
            shouldNotThrow<Exception> {
                mqttClient.connect().reasonCode shouldBe MqttConnAckReasonCode.Success
            }
            shouldThrowUnit<Exception> {
                mqttClient.connect()
            }
        }
    })
