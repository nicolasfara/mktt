package io.github.nicolasfara.mktt.engine

import kotlinx.coroutines.CoroutineDispatcher

internal class DefaultEngineFactory(
    private val host: String,
    private val port: Int,
    private val dispatcher: CoroutineDispatcher,
) : MqttEngineFactory<DefaultEngineConfig> {

    override fun create(block: DefaultEngineConfig.() -> Unit): MqttEngine = DefaultEngine(
        DefaultEngineConfig(
            host,
            port,
        ).apply(block),
        dispatcher,
    )
}
