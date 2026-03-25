package io.github.nicolasfara.mktt.core

import kotlin.jvm.JvmInline

/**
 * May represent a topic or a topic filter expression. Hence, valid topics are:
 *
 * - `sport/tennis/player1`
 * - `sport/tennis/player1/#`
 * - `sport/tennis/+` and also
 * - `/`
 */
@JvmInline
value class Topic(val name: String) {

    fun containsWildcard(): Boolean = name.indexOfAny(charArrayOf('#', '+')) != -1

    /**
     * Determines whether this topic is a valid 'shared subscription name', hence has the form:
     * `$share/{ShareName}/{filter}`
     */
    fun isShared(): Boolean =
        shareRegex.matches(name)

    /**
     * When this is a shared subscription (see [isShared]), returns the name of the share and the remaining filter,
     * otherwise throws [IllegalStateException]. Hence, for a topic `$share/consumer1/sport/tennis/+` this method
     * returns `consumer1` as the share name and `sport/tennis/+` as the filter.
     */
    fun shareNameAndFilter(): Pair<String, Topic> {
        shareRegex.find(name)?.let { m ->
            return m.groupValues[1] to Topic(m.groupValues[2])
        }
        throw IllegalStateException("'$name' is not a valid shared subscription")
    }

    fun isNotBlank(): Boolean = name.isNotBlank()

    override fun toString(): String = name

    internal companion object {
        val shareRegex = Regex($$"""\$share/([^+#/]+)/(.+)""")
    }
}

/**
 * Converts a list of strings into a list of Topic items.
 */
fun topics(vararg topic: String): List<Topic> = topic.map {
    Topic(it)
}
