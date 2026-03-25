package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.util.MqttDslMarker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface MqttEngineFactory<out T : MqttEngineConfig> {

    fun create(block: T.() -> Unit): MqttEngine
}

@MqttDslMarker
open class MqttEngineConfig {

    var dispatcher: CoroutineDispatcher = Dispatchers.Default
}
