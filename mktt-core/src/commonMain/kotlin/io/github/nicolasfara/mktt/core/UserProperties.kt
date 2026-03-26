package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.util.MqttDslMarker
import kotlinx.io.Sink

/**
 * Represents the user properties of the MQTT specification.
 *
 * Note that unlike in `Map<String, String>`, the names of the
 * user properties may occur more than once.
 */
data class UserProperties(
    /** Underlying list of user-property key/value entries. */
    val values: List<StringPair>,
) {

    // Note: not using a map for storing key/value pairs, as the key might appear more than once in a user property!

    /**
     * Returns the first occurrence of the user property with the specified name,
     * or `null` if this user property doesn't
     * contain the specified name.
     *
     * @see getAll
     */
    operator fun get(name: String): String? = values.firstOrNull { it.name == name }?.value

    /**
     * Returns all values of the properties with the specified name.
     */
    fun getAll(name: String): List<String> = values.filter { it.name == name }.map { it.value }

    /** Returns `true` if at least one property exists with the given name. */
    fun containsKey(name: String): Boolean = values.any { it.name == name }

    /** Returns `true` if at least one property exists with the given value. */
    fun containsValue(value: String): Boolean = values.any { it.value == value }

    /** Returns `true` when this set of user properties is not empty. */
    fun isNotEmpty(): Boolean = values.isNotEmpty()

    /** Holds shared factory helpers and constants for [UserProperties]. */
    companion object {

        /**
         * An empty list of user properties.
         */
        val EMPTY: UserProperties =
            UserProperties(values = emptyList())

        internal fun from(properties: List<Property<*>>): UserProperties =
            with(properties.filterIsInstance<UserProperty>()) {
                if (isEmpty()) {
                    EMPTY
                } else {
                    UserProperties(map { it.value })
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
fun buildUserProperties(init: UserPropertiesBuilder.() -> Unit): UserProperties {
    val builder = UserPropertiesBuilder()
    builder.init()
    return builder.build()
}

/**
 * DSL for creating MQTT user properties.
 *
 * Note that the same name is allowed to appear more than once in
 * user properties.
 */
@MqttDslMarker
class UserPropertiesBuilder {

    private val userProperties = mutableListOf<StringPair>()

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
    infix fun String.to(value: String) {
        userProperties.add(StringPair(this, value))
    }

    /**
     * Adds a property with the specified key and value to this builder.
     */
    fun add(key: String, value: String) {
        userProperties.add(StringPair(key, value))
    }

    /**
     * Adds all key/value pairs from the list to this builder.
     */
    fun addAll(properties: List<Pair<String, String>>) {
        userProperties.addAll(
            properties.map {
                StringPair(
                    it.first,
                    it.second,
                )
            },
        )
    }

    /** Builds a [UserProperties] instance from the values added to this builder. */
    fun build(): UserProperties = if (userProperties.isEmpty()) {
        UserProperties.EMPTY
    } else {
        UserProperties(userProperties)
    }
}

internal val UserProperties.asArray: Array<UserProperty>
    get() = values.map { UserProperty(it) }.toTypedArray()

internal fun Sink.write(userProperties: UserProperties) {
    if (userProperties.values.isNotEmpty()) {
        userProperties.values.forEach { this.write(it) }
    }
}
