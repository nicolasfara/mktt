package it.nicolasfarabegoli.mktt

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.testCoroutineScheduler
import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MqttClientTest : FreeSpec({
    coroutineTestScope = true
    "The client should be able to connect and disconnect from the broker" {
        val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "test.mosquitto.org"), dispatcher)
        shouldNotThrow<Exception> {
            mqttClient.connect()
            mqttClient.disconnect()
        }
    }
})
