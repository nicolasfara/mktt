package io.github.nicolasfara

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Creates an MQTT client with the given [dispatcher] and [clientConfig].
 */
actual fun createMqttClient(dispatcher: CoroutineDispatcher, clientConfig: MqttClientConfiguration): MkttClient =
    MqttJsClient(dispatcher, clientConfig)
