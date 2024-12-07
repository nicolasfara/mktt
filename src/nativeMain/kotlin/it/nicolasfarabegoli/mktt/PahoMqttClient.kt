package it.nicolasfarabegoli.mktt

import it.nicolasfarabegoli.mktt.configuration.MqttConfiguration
import it.nicolasfarabegoli.mktt.message.connect.connack.MqttConnAck
import it.nicolasfarabegoli.mktt.message.publish.MqttPublish
import it.nicolasfarabegoli.mktt.message.publish.MqttPublishResult
import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

class PahoMqttClient(
    private val configuration: MqttConfiguration,
    override val defaultDispatcher: CoroutineDispatcher,
) : MqttClient {

//    private val client = nativeHeap.alloc<MQTTClientVar>()
//    private val mqttConnectionOptions = nativeHeap.alloc<MQTTClient_connectOptions> {
//        struct_id[0] = 'M'.code.toByte()
//        struct_id[1] = 'Q'.code.toByte()
//        struct_id[2] = 'T'.code.toByte()
//        struct_id[3] = 'C'.code.toByte()
//        struct_version = 8
//        keepAliveInterval = configuration.keepAliveInterval
//        cleansession = 0
//        reliable = 1
//        will = null
//        configuration.username.encodeToByteArray().usePinned {
//            username = it.addressOf(0)
//        }
//        configuration.password.encodeToByteArray().usePinned {
//            password = it.addressOf(0)
//        }
//        connectTimeout = 30
//        retryInterval = 0
//        ssl = null
//        serverURIcount = 0
//        serverURIs = null
//        MQTTVersion = MQTTVERSION_5
//        cValue<anonymousStruct3> {
//            serverURI = null
//            MQTTVersion = 0
//            sessionPresent = 0
//        }
//        cValue<anonymousStruct4> {
//            "usr".encodeToByteArray().usePinned { username = it.addressOf(0) }
//            password = null
//        }
//        maxInflightMessages = -1
//        cleanstart = 1
//        httpHeaders = null
//        httpProxy = null
//        httpsProxy = null
//    }

    override suspend fun connect(): MqttConnAck = withContext(Dispatchers.IO) {
//        if (client.value != null) {
//            error("The client is already connected")
//        }
//        val connectionString = "tcp://${configuration.hostname}:${configuration.port}"
//        val result = MQTTClient_create(
//            client.ptr,
//            connectionString,
//            "CHANGE_ME",
//            MQTTCLIENT_PERSISTENCE_NONE,
//            null,
//        )
//        require(result == MQTTCLIENT_SUCCESS) {
//            "Failed to create the MQTT client"
//        }
//        val res = MQTTClient_connect(client.value, mqttConnectionOptions.ptr)
//        MqttConnAck(
//            MqttConnAckReasonCode.fromCode(res.toByte()),
//            false,
//        )
        TODO()
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
//        requireNotNull(client.value) {
//            "The client is not connected"
//        }
//        MQTTClient_disconnect(client.value, 0)
//        MQTTClient_destroy(client.ptr)
    }

    override fun subscribe(subscription: MqttSubscription): Flow<MqttPublish> {
        TODO("Not yet implemented")
    }

    override fun publish(messages: Flow<MqttPublish>): Flow<MqttPublishResult> {
        TODO("Not yet implemented")
    }
}
