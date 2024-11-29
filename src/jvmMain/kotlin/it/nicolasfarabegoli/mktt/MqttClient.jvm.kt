package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Creates a new [MqttClient] based on the given [configuration] and [dispatcher].
 */
actual fun MqttClient(configuration: MqttConfiguration, dispatcher: CoroutineDispatcher): MqttClient =
    HivemqMqttClient(configuration, dispatcher)
