package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.facade.MqttClient
import it.nicolasfarabegoli.mktt.facade.toClientOptions
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAck
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAckReasonCode
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.message.publish.MqttPublishResult
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class MqttJsClient(
    private val configuration: MqttConfiguration,
    override val defaultDispatcher: CoroutineDispatcher,
) : MkttClient {
    private var connected = false
    private lateinit var mqttClient: MqttClient

    override suspend fun connect(): MqttConnAck = withContext(defaultDispatcher) {
        require(!connected) { "The client is already connected" }
        mqttClient = it.nicolasfarabegoli.mktt.facade.connect(
            configuration.toClientOptions(),
        )
        suspendCoroutine<MqttConnAck> { continuation ->
            mqttClient.on("connect") {
                val connAck = MqttConnAck(
                    reasonCode = MqttConnAckReasonCode.from(it.returnCode),
                    isSessionPresent = it.sessionPresent,
                )
                connected = true
                continuation.resume(connAck)
            }
            mqttClient.on("error") {
                continuation.resumeWithException(Exception(JSON.stringify(it)))
            }
        }
    }

    override suspend fun disconnect() {
        require(connected) { "The client is not connected" }
        connected = false
        mqttClient.endAsync().await()
    }

    override fun subscribe(subscription: MqttSubscription): Flow<MqttPublish> {
        TODO("Not yet implemented")
    }

    override fun publish(messages: Flow<MqttPublish>): Flow<MqttPublishResult> {
        TODO("Not yet implemented")
    }
}
