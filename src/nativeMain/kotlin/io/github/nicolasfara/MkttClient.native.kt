package io.github.nicolasfara

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Creates an MQTT client with the given [dispatcher] and [clientConfig].
 */
actual internal fun createMqttClient(dispatcher: CoroutineDispatcher, clientConfig: MqttClientConfiguration): MkttClient =
    NativeMkttClient(dispatcher, clientConfig)
