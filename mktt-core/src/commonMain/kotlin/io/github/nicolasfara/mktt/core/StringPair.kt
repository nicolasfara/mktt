package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.util.readMqttString
import io.github.nicolasfara.mktt.core.util.writeMqttString
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * Represents a name/value pair as specified in the [MQTT specification](https://docs.oasis-open.org/mqtt/mqtt/v5.0/os/mqtt-v5.0-os.html#_Toc3901013).
 */
public data class StringPair(val name: String, val value: String) {

    override fun toString(): String = "$name=$value"
}

/**
 * Infix function to create a [io.github.nicolasfara.mktt.core.StringPair], hence:
 * ```
 * val stringPair = "name" to "value"
 * ```
 */
public infix fun String.to(that: String): io.github.nicolasfara.mktt.core.StringPair =
    _root_ide_package_.io.github.nicolasfara.mktt.core.StringPair(this, that)

internal fun Sink.write(pair: io.github.nicolasfara.mktt.core.StringPair) {
    writeMqttString(pair.name)
    writeMqttString(pair.value)
}

internal fun Source.readStringPair() =
    _root_ide_package_.io.github.nicolasfara.mktt.core.StringPair(readMqttString(), readMqttString())
