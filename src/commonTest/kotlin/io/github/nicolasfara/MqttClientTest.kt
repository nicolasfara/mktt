package io.github.nicolasfara

import io.github.nicolasfara.configuration.MqttTestConfiguration.BROKER
import io.github.nicolasfara.configuration.MqttTestConfiguration.SSL_PORT
import io.github.nicolasfara.configuration.MqttTestConfiguration.connectionConfiguration
import io.github.nicolasfara.configuration.MqttTestConfiguration.invalidPortConnectionConfiguration
import io.github.nicolasfara.configuration.MqttTestConfiguration.wrongConnectionConfiguration
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MqttClientTest {
    @Test
    fun `The client should be able to connect and disconnect from the broker`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        mqttClient.disconnect()
    }

    @Test
    fun `The client should fail with an exception when connecting to an invalid broker`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, wrongConnectionConfiguration)
        assertFailsWith<Throwable> { mqttClient.connect() }
    }

    @Test
    fun `The client should fail with an exception when connecting to a valid broker using an invalid port`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, invalidPortConnectionConfiguration)
        assertFailsWith<Throwable> { mqttClient.connect() }
    }

    @Test
    fun `The client should fail with an exception when disconnecting without connecting`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        assertFailsWith<Throwable> { mqttClient.disconnect() }
    }

    @Test
    fun `The client should fail with an exception when disconnecting twice`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        mqttClient.disconnect()
        assertFailsWith<Throwable> { mqttClient.disconnect() }
    }

    @Test
    fun `The client should fail with an exception when connecting twice`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        assertFailsWith<Throwable> { mqttClient.connect() }
        mqttClient.disconnect()
    }

    @Test
    fun `The connectionState should reflect Connected after a successful connect`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        assertIs<MqttConnectionState.Connected>(mqttClient.connectionState.value)
        mqttClient.disconnect()
    }

    @Test
    fun `The connectionState should reflect Disconnected after disconnect`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        mqttClient.disconnect()
        assertIs<MqttConnectionState.Disconnected>(mqttClient.connectionState.value)
    }

    @Test
    fun `The connectionState should not remain Connecting after a failed connection attempt`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, wrongConnectionConfiguration)
        assertFailsWith<Throwable> { mqttClient.connect() }
        val state = mqttClient.connectionState.value
        assertTrue(
            state is MqttConnectionState.Disconnected || state is MqttConnectionState.ConnectionError,
            "Expected Disconnected or ConnectionError but got $state",
        )
    }

    @Test
    fun `The connectionState initial value should be Disconnected`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        assertIs<MqttConnectionState.Disconnected>(mqttClient.connectionState.value)
    }

    @Test
    fun `The client should connect over SSL`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher) {
            brokerUrl = BROKER
            port = SSL_PORT
            ssl = true
        }
        try {
            mqttClient.connect()
        } catch (error: IllegalStateException) {
            // Ktor TLS on Kotlin/Native currently reports this unsupported mode explicitly.
            if (error.message?.contains("TLS sessions are not supported") == true) {
                return@runTest
            }
            throw error
        }
        assertIs<MqttConnectionState.Connected>(mqttClient.connectionState.value)
        mqttClient.disconnect()
    }

    @Test
    fun `The connectionState value should reflect each stage of the client lifecycle`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        assertIs<MqttConnectionState.Disconnected>(mqttClient.connectionState.value)
        mqttClient.connect()
        assertIs<MqttConnectionState.Connected>(mqttClient.connectionState.value)
        mqttClient.disconnect()
        assertIs<MqttConnectionState.Disconnected>(mqttClient.connectionState.value)
    }
}
