package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import kotlinx.coroutines.CoroutineDispatcher

/**
 * TODO.
 */
actual fun MqttClient(configuration: MqttConfiguration, dispatcher: CoroutineDispatcher): MkttClient =
    MqttJsClient(configuration, dispatcher)
