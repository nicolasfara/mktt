package io.github.nicolasfara.mktt.core.util

/**
 * Marks MQTT builder DSL scopes to prevent accidentally mixing receivers.
 */
@DslMarker
annotation class MqttDslMarker
