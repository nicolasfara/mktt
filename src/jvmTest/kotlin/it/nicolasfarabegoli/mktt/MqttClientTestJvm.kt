package it.nicolasfarabegoli.mktt

import com.hivemq.client.mqtt.exceptions.ConnectionFailedException
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.test.testCoroutineScheduler
import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class)
class MqttClientTestJvm : FreeSpec({
    coroutineTestScope = true
    "The client should raise an `ConnectionFailedException` exception when connecting to an invalid broker in JVM" {
        val dispatcher = StandardTestDispatcher(testCoroutineScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "invalid.broker"), dispatcher)
        shouldThrowUnit<ConnectionFailedException> {
            mqttClient.connect()
        }
    }
})
