package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.errors.ClientAlreadyConnectedError
import it.nicolasfarabegoli.mktt.errors.ClientNotConnectedError
import it.nicolasfarabegoli.mktt.errors.ConnectionError
import it.nicolasfarabegoli.mktt.errors.GenericClientError
import it.nicolasfarabegoli.mktt.errors.InvalidBrokerError
import it.nicolasfarabegoli.mktt.facade.IClientPublishOptions
import it.nicolasfarabegoli.mktt.facade.MqttClient
import it.nicolasfarabegoli.mktt.facade.connectAsync
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAck
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

internal class MqttJsClient(
    private val configuration: MqttConfiguration,
    override val defaultDispatcher: CoroutineDispatcher,
) : MkttClient {
    private var connected = false
    private lateinit var mqttClient: MqttClient

    override suspend fun connect(): MqttConnAck = withContext(defaultDispatcher) {
        if (connected) {
            throw ClientAlreadyConnectedError()
        }
        try {
            mqttClient = connectAsync("mqtt://${configuration.hostname}:${configuration.port}").await()
            connected = true
        } catch (error: Throwable) {
            when {
                error.message?.contains("ENOTFOUND") == true -> throw InvalidBrokerError(error.message!!)
                else -> {
                    val errors = error.asDynamic().errors.unsafeCast<Array<Error>>()
                    when {
                        errors.any { it.message?.contains("ECONNREFUSED") == true } ->
                            throw ConnectionError(errors.joinToString { it.message.toString() })
                        else -> throw GenericClientError(errors.joinToString { it.message.toString() })
                    }
                }
            }
        }
        // TODO(this code must be fixed since no connack in async api from mqtt.js)
        MqttConnAck(MqttConnAckReasonCode.Success, false)
    }

    override suspend fun disconnect() {
        if (!connected) {
            throw ClientNotConnectedError()
        }
        connected = false
        mqttClient.endAsync().await()
    }

    override fun subscribe(subscription: MqttSubscription): Flow<MqttPublish> {
        TODO()
//        require(connected) { "The client is not connected" }
//        return callbackFlow<MqttPublish> {
//            val subscriptionResult = mqttClient.subscribeAsync(subscription.toISubscriptionMap()).await()
//            require(subscriptionResult.isNotEmpty()) { "The subscription failed, empty answer" }
//            mqttClient.on("message") { topic, message ->
//                println("topic: $topic")
//                println("message: $message")
//                val mqttPublish = MqttPublish(
//                    topic = MqttTopic.of(topic),
//                )
//                trySend(mqttPublish)
//                    .onFailure { error ->
//                        close(error)
//                    }
//            }
//        }
    }

    override suspend fun publish(messages: Flow<MqttPublish>) {
        messages.collect { message ->
            val options = object : IClientPublishOptions {
                override var qos: Number? = message.qos.code
                override var retain: Boolean? = message.isRetain
                override var dup: Boolean? = null
                override var properties: Any? = null
                override var cbStorePut: ((it.nicolasfarabegoli.mktt.facade.Error?) -> Unit)? = null

            }
            mqttClient.publishAsync(message.topic.topicName, message.payload?.decodeToString() ?: "", options)
                    .await()
        }
    }
}
