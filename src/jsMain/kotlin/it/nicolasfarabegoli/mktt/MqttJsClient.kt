package it.nicolasfarabegoli.mktt

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

// internal class MqttJsClient(
//    private val configuration: MqttConfiguration,
//    override val defaultDispatcher: CoroutineDispatcher,
// ) : MkttClient {
//    private var connected = false
//    private lateinit var mqttClient: MqttClient
//
//    override suspend fun connect(): MqttConnAck = withContext(defaultDispatcher) {
//        if (connected) {
//            throw ClientAlreadyConnectedError()
//        }
//        try {
//            mqttClient = connectAsync("mqtt://${configuration.hostname}:${configuration.port}").await()
//            connected = true
//        } catch (error: Throwable) {
//            when {
//                error.message?.contains("ENOTFOUND") == true -> throw InvalidBrokerError(error.message!!)
//                else -> {
//                    val errors = error.asDynamic().errors.unsafeCast<Array<Error>>()
//                    when {
//                        errors.any { it.message?.contains("ECONNREFUSED") == true } ->
//                            throw ConnectionError(errors.joinToString { it.message.toString() })
//                        else -> throw GenericClientError(errors.joinToString { it.message.toString() })
//                    }
//                }
//            }
//        }
//        // TODO(this code must be fixed since no connack in async api from mqtt.js)
//        MqttConnAck(MqttConnAckReasonCode.Success, false)
//    }
//
//    override suspend fun disconnect() {
//        if (!connected) {
//            throw ClientNotConnectedError()
//        }
//        connected = false
//        mqttClient.endAsync().await()
//    }
//
//    override fun subscribe(subscription: MqttSubscription): Flow<MqttPublish> {
//        TODO()
// //        require(connected) { "The client is not connected" }
// //        return callbackFlow<MqttPublish> {
// //            val subscriptionResult = mqttClient.subscribeAsync(subscription.toISubscriptionMap()).await()
// //            require(subscriptionResult.isNotEmpty()) { "The subscription failed, empty answer" }
// //            mqttClient.on("message") { topic, message ->
// //                println("topic: $topic")
// //                println("message: $message")
// //                val mqttPublish = MqttPublish(
// //                    topic = MqttTopic.of(topic),
// //                )
// //                trySend(mqttPublish)
// //                    .onFailure { error ->
// //                        close(error)
// //                    }
// //            }
// //        }
//    }
//
//    override suspend fun publish(messages: Flow<MqttPublish>) {
//        messages.collect { message ->
//            val options = object : IClientPublishOptions {
//                override var qos: Number? = message.qos.code
//                override var retain: Boolean? = message.isRetain
//                override var dup: Boolean? = null
//                override var properties: Any? = null
//                override var cbStorePut: ((it.nicolasfarabegoli.mktt.facade.Error?) -> Unit)? = null
//            }
//            mqttClient.publishAsync(message.topic.topicName, message.payload?.decodeToString() ?: "", options)
//                .await()
//        }
//    }
// }

internal class MqttJsClient(
    override val dispatcher: CoroutineDispatcher,
    private val configuration: MqttClientConfiguration,
) : MkttClient {
    override val connectionState: Flow<MqttConnectionState>
        get() = TODO("Not yet implemented")

    override suspend fun connect() {
        TODO("Not yet implemented")
    }

    override suspend fun disconnect() {
        TODO("Not yet implemented")
    }

    override suspend fun publish(
        topic: String,
        message: ByteArray,
        qos: MqttQoS,
    ) {
        TODO("Not yet implemented")
    }

    override fun subscribe(
        topic: String,
        qos: MqttQoS,
    ): Flow<MqttMessage> {
        TODO("Not yet implemented")
    }

    override suspend fun unsubscribe(topic: String) {
        TODO("Not yet implemented")
    }
}
