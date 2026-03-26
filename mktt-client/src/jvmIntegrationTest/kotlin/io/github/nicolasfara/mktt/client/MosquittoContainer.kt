package io.github.nicolasfara.mktt.client

import kotlin.test.assertEquals
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.ImageFromDockerfile

internal class MosquittoContainer {
    private val container = GenericContainer(
        ImageFromDockerfile()
            .withFileFromClasspath("mosquitto.conf", "mosquitto.conf")
            .withFileFromClasspath("passwd", "passwd")
            .withFileFromClasspath("Dockerfile", "Dockerfile")
            .withFileFromClasspath("ca.crt", "ca.crt")
            .withFileFromClasspath("server.key", "server.key")
            .withFileFromClasspath("server.crt", "server.crt"),
    ).withExposedPorts(BROKER_PORT, AUTHENTICATED_BROKER_PORT, TLS_BROKER_PORT)

    val host: String
        get() = container.host

    val defaultPort: Int
        get() = container.getMappedPort(AUTHENTICATED_BROKER_PORT)

    val tlsPort: Int
        get() = container.getMappedPort(TLS_BROKER_PORT)

    fun start() {
        container.start()
    }

    fun stop() {
        container.stop()
    }

    fun publish(topic: String, qos: String, payload: String) {
        val result = container.execInContainer(
            "mosquitto_pub",
            "-h",
            "localhost",
            "-u",
            USER,
            "-P",
            PASSWORD,
            "-t",
            topic,
            "-q",
            qos,
            "-i",
            "test-publisher",
            "-m",
            payload,
        )
        assertEquals(COMMAND_SUCCESS_EXIT_CODE, result.exitCode, result.stderr)
    }

    companion object {
        private const val BROKER_PORT = 1883
        private const val AUTHENTICATED_BROKER_PORT = 1884
        private const val TLS_BROKER_PORT = 8883
        private const val COMMAND_SUCCESS_EXIT_CODE = 0

        const val USER = "mqtt-test-user"
        const val PASSWORD = "3n63hLKRV31fHf41NF95"
    }
}
