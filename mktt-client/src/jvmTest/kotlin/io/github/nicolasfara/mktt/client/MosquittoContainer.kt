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
    ).withExposedPorts(1883, 1884, 8883)

    val host: String
        get() = container.host

    val defaultPort: Int
        get() = container.getMappedPort(1884)

    val tlsPort: Int
        get() = container.getMappedPort(8883)

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
        assertEquals(0, result.exitCode, result.stderr)
    }

    companion object {
        const val USER = "mqtt-test-user"
        const val PASSWORD = "3n63hLKRV31fHf41NF95"
    }
}
