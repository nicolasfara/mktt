package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.util.MqttDslMarker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public interface MqttEngineFactory<out T : io.github.nicolasfara.mktt.client.MqttEngineConfig> {

    public fun create(block: T.() -> Unit): io.github.nicolasfara.mktt.client.MqttEngine
}

@io.github.nicolasfara.mktt.core.util.MqttDslMarker
public open class MqttEngineConfig {

    public var dispatcher: CoroutineDispatcher = Dispatchers.Default
}
