package io.github.nicolasfara

import io.github.nicolasfara.configuration.MqttTestConfiguration.connectionConfiguration
import io.github.nicolasfara.configuration.MqttTestConfiguration.invalidPortConnectionConfiguration
import io.github.nicolasfara.configuration.MqttTestConfiguration.wrongConnectionConfiguration
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

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
    fun `The connectionState should reflect Disconnected after a failed connection attempt`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, wrongConnectionConfiguration)
        assertFailsWith<Throwable> { mqttClient.connect() }
        assertIs<MqttConnectionState>(mqttClient.connectionState.value)
        // State must not be Connecting after a failed attempt
        check(mqttClient.connectionState.value !is MqttConnectionState.Connecting)
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
            brokerUrl = io.github.nicolasfara.configuration.MqttTestConfiguration.BROKER
            port = io.github.nicolasfara.configuration.MqttTestConfiguration.SSL_PORT
            ssl = true
        }
        mqttClient.connect()
        assertIs<MqttConnectionState.Connected>(mqttClient.connectionState.value)
        mqttClient.disconnect()
    }

    @Test
    fun `The connectionState should emit state changes as a Flow`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        assertIs<MqttConnectionState.Disconnected>(mqttClient.connectionState.first())
        mqttClient.connect()
        assertIs<MqttConnectionState.Connected>(mqttClient.connectionState.first())
        mqttClient.disconnect()
        assertIs<MqttConnectionState.Disconnected>(mqttClient.connectionState.first())
    }
}
