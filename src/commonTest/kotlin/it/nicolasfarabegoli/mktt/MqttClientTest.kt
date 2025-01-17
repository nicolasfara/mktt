package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MqttClientTest {
    @Test
    fun `The client should be able to connect and disconnect from the broker`() = runTest {
        val testCoroutineScheduler = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), testCoroutineScheduler)
        assertEquals(MqttConnAckReasonCode.Success, mqttClient.connect().reasonCode)
        mqttClient.disconnect()
    }

    @Test
    fun `The client should fail with an exception when connecting to an invalid broker`() = runTest {
        val testCoroutineScheduler = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "invalid.broker"), testCoroutineScheduler)
        assertFailsWith<Exception> { mqttClient.connect() }
    }

    @Test
    fun `The client should fail with an exception when connecting to a valid broker using an invalid port`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "test.mosquitto.org", port = 1234), dispatcher)
        assertFailsWith<Exception> { mqttClient.connect() }
    }

    @Test
    fun `The client should fail with an exception when disconnecting without connecting`() = runTest {
        val testCoroutineScheduler = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), testCoroutineScheduler)
        assertFailsWith<Exception> { mqttClient.disconnect() }
    }

    @Test
    fun `The client should fail with an exception when disconnecting twice`() = runTest {
        val testCoroutineScheduler = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), testCoroutineScheduler)
        assertEquals(MqttConnAckReasonCode.Success, mqttClient.connect().reasonCode)
        mqttClient.disconnect()
        assertFailsWith<Exception> { mqttClient.disconnect() }
    }

    @Test
    fun `The client should fail with an exception when connecting twice`() = runTest {
        val testCoroutineScheduler = StandardTestDispatcher(testScheduler)
        val mqttClient = MqttClient(MqttConfiguration(hostname = "mqtt.eclipseprojects.io"), testCoroutineScheduler)
        assertEquals(MqttConnAckReasonCode.Success, mqttClient.connect().reasonCode)
        assertFailsWith<Exception> { mqttClient.connect() }
    }
}
