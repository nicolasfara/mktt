package io.github.nicolasfara.mktt.engine

import io.github.nicolasfara.mktt.core.util.MqttDslMarker
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Factory used to create transport engines for the MQTT client.
 */
interface MqttEngineFactory<out T : MqttEngineConfig> {

    /**
     * Creates a new engine by applying [block] to a fresh engine configuration instance.
     */
    fun create(block: T.() -> Unit): MqttEngine
}

/**
 * Base DSL configuration used by [MqttEngineFactory] implementations.
 */
@MqttDslMarker
open class MqttEngineConfig
