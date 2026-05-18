package io.github.nicolasfara.mktt.engine

import io.github.nicolasfara.mktt.core.util.MqttDslMarker

/**
 * Factory used to create transport engines for the MQTT client.
 */
interface MqttEngineFactory<out T : MqttEngineConfig> {

    /**
     * Creates a new engine by applying [block] to a fresh engine configuration instance.
     *
     * @param block configuration block applied to the engine-specific configuration.
     * @return a configured MQTT transport engine.
     */
    fun create(block: T.() -> Unit): MqttEngine
}

/**
 * Base DSL configuration used by [MqttEngineFactory] implementations.
 */
@MqttDslMarker
open class MqttEngineConfig
