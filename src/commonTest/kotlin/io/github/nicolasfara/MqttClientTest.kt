package io.github.nicolasfara

import io.github.nicolasfara.configuration.MqttTestConfiguration.connectionConfiguration
import io.github.nicolasfara.configuration.MqttTestConfiguration.invalidPortConnectionConfiguration
import io.github.nicolasfara.configuration.MqttTestConfiguration.wrongConnectionConfiguration
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
    @Ignore
    fun `The client should fail with an exception when disconnecting twice`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        mqttClient.disconnect()
        assertFailsWith<Throwable> { mqttClient.disconnect() }
    }

    @Test
    @Ignore
    fun `The client should fail with an exception when connecting twice`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val mqttClient = MkttClient(dispatcher, connectionConfiguration)
        mqttClient.connect()
        assertFailsWith<Throwable> { mqttClient.connect() }
        mqttClient.disconnect()
    }
}
