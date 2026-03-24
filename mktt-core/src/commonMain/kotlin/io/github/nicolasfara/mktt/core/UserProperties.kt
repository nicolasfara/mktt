package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.util.MqttDslMarker
import io.github.nicolasfara.mktt.core.write
import kotlinx.io.Sink

/**
 * Represents the user properties of the MQTT specification. Note that unlike in `Map<String, String>` the names of the
 * user properties may occur more than once.
 */
public data class UserProperties(public val values: List<io.github.nicolasfara.mktt.core.StringPair>) {

    // Note: not using a map for storing key/value pairs, as the key might appear more than once in a user property!

    /**
     * Returns the first occurrence of the user property with the specified name or `null` if this user property doesn't
     * contain the specified name
     *
     * @see getAll
     */
    public operator fun get(name: String): String? = values.firstOrNull { it.name == name }?.value

    /**
     * Returns all values of the properties with the specified name.
     */
    public fun getAll(name: String): List<String> = values.filter { it.name == name }.map { it.value }

    public fun containsKey(name: String): Boolean = values.find { it.name == name } != null

    public fun containsValue(value: String): Boolean = values.find { it.value == value } != null

    public fun isNotEmpty(): Boolean = values.isNotEmpty()

    public companion object {

        /**
         * An empty list of user properties.
         */
        public val EMPTY: io.github.nicolasfara.mktt.core.UserProperties =
            _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties(values = emptyList())

        internal fun from(
            properties: List<io.github.nicolasfara.mktt.core.Property<*>>,
        ): io.github.nicolasfara.mktt.core.UserProperties =
            with(properties.filterIsInstance<io.github.nicolasfara.mktt.core.UserProperty>()) {
                if (isEmpty()) {
                    EMPTY
                } else {
                    _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties(map { it.value })
                }
            }
    }
}

/**
 * DSL for building a [io.github.nicolasfara.mktt.core.UserProperties] instance.
 *
 * For example:
 * ```kotlin
 * buildUserProperties {
 *     "filename" to "test.txt"
 * }
 * ```
 */
public fun buildUserProperties(
    init: io.github.nicolasfara.mktt.core.UserPropertiesBuilder.() -> Unit,
): io.github.nicolasfara.mktt.core.UserProperties {
    val builder = _root_ide_package_.io.github.nicolasfara.mktt.core.UserPropertiesBuilder()
    builder.init()
    return builder.build()
}

/**
 * DSL for creating MQTT user properties. Note that the same name is allowed to appear more than once in user properties.
 */
@io.github.nicolasfara.mktt.core.util.MqttDslMarker
public class UserPropertiesBuilder {

    private val userProperties = mutableListOf<io.github.nicolasfara.mktt.core.StringPair>()

    /**
     * Shortcut for adding key and value to this builder. Hence
     * ```
     * userProperties {
     *     "key-1" to "value-1"
     *     "key-2" to "value-2"
     * }
     * ```
     * adds 2 properties.
     */
    public infix fun String.to(value: String) {
        userProperties.add(_root_ide_package_.io.github.nicolasfara.mktt.core.StringPair(this, value))
    }

    /**
     * Adds a property with the specified key and value to this builder.
     */
    public fun add(key: String, value: String) {
        userProperties.add(_root_ide_package_.io.github.nicolasfara.mktt.core.StringPair(key, value))
    }

    /**
     * Adds all key/value pairs from the list to this builder.
     */
    public fun addAll(properties: List<Pair<String, String>>) {
        userProperties.addAll(
            properties.map {
                _root_ide_package_.io.github.nicolasfara.mktt.core.StringPair(
                    it.first,
                    it.second,
                )
            },
        )
    }

    public fun build(): io.github.nicolasfara.mktt.core.UserProperties = if (userProperties.isEmpty()) {
        _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY
    } else {
        _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties(userProperties)
    }
}

internal val io.github.nicolasfara.mktt.core.UserProperties.asArray: Array<io.github.nicolasfara.mktt.core.UserProperty>
    get() = values.map { _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperty(it) }.toTypedArray()

internal fun Sink.write(userProperties: io.github.nicolasfara.mktt.core.UserProperties) {
    if (userProperties.values.isNotEmpty()) {
        userProperties.values.forEach { this.write(it) }
    }
}
