package it.nicolasfarabegoli.mktt

import kotlinx.coroutines.CoroutineDispatcher

actual fun createMqttClient(
    dispatcher: CoroutineDispatcher,
    clientConfig: MqttClientConfiguration,
): MkttClient = MqttJsClient(dispatcher, clientConfig)
