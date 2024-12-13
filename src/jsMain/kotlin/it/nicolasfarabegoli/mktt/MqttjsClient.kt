package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.facade.MqttClient
import it.nicolasfarabegoli.mktt.facade.connectAsync
import it.nicolasfarabegoli.mktt.facade.toClientOptions
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAck
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.message.publish.MqttPublishResult
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class MqttjsClient(
    private val configuration: MqttConfiguration,
    override val defaultDispatcher: CoroutineDispatcher
) : MkttClient {
    private lateinit var mqttClient: MqttClient
    override suspend fun connect(): MqttConnAck = withContext(defaultDispatcher) {
        mqttClient = connectAsync(configuration.hostname, configuration.toClientOptions(), true).await()
        TODO("Not yet implemented")
    }

    override suspend fun disconnect() {
        mqttClient.endAsync().await()
    }

    override fun subscribe(subscription: MqttSubscription): Flow<MqttPublish> {
        TODO("Not yet implemented")
    }

    override fun publish(messages: Flow<MqttPublish>): Flow<MqttPublishResult> {
        TODO("Not yet implemented")
    }
}