package io.github.nicolasfara.mktt

import io.github.nicolasfara.mktt.client.MqttClient
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers

open class DemoServer(
    val host: String,
    val port: Int,
    val tlsPort: Int,
    val websocketPort: Int,
    val tlsWebsocketPort: Int
) {
    open val clientId: String
        get() = "${this::class.simpleName}-${Random.nextInt(1_000, Int.MAX_VALUE)}"

    open val websocketPath = "/mqtt"
}

object Mosquitto : DemoServer("test.mosquitto.org", 1883, 8886, 8080, 8081)
object HiveMQ : DemoServer("broker.hivemq.com", 1883, 8883, 8000, 8884)
object Emqx : DemoServer("broker.emqx.io", 1883, 8883, 8083, 8084)

@Ignore
class PublicBrokerTest {

    private val server: DemoServer = Mosquitto

    //    @Test
    //    fun `test unencrypted connection`() = runTest {
    //        val dispatcher = Dispatchers.Unconfined
    //        MqttClient(server.host, server.port, dispatcher) {
    //            clientId = server.clientId
    //        }.testConnection()
    //    }
    //
    //    @Test
    //    fun `test encrypted connection`() = runTest {
    //        MqttClient(server.host, server.tlsPort) {
    //            clientId = server.clientId
    //            connection {
    //                tls { }
    //            }
    //        }.testConnection()
    //    }

    @Test
    fun `test unencrypted websocket connection`() = runTest {
        val dispatcher = Dispatchers.Unconfined
        MqttClient(Url("ws://${server.host}:${server.websocketPort}${server.websocketPath}"), dispatcher) {
            clientId = server.clientId
        }.testConnection()
    }

    @Test
    fun `test encrypted websocket connection`() = runTest {
        val dispatcher = Dispatchers.Unconfined
        MqttClient(Url("https://${server.host}:${server.tlsWebsocketPort}${server.websocketPath}"), dispatcher) {
            clientId = server.clientId
        }.testConnection()
    }

    private suspend fun MqttClient.testConnection() {
        val connack = connect()
        assertTrue { connack.isSuccess }
        disconnect()
    }
}
