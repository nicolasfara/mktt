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
value class Topic(
    /** Topic name or topic-filter expression. */
    val name: String,
) {

    /** Returns `true` when this topic contains MQTT wildcard characters. */
    fun containsWildcard(): Boolean = name.indexOfAny(charArrayOf('#', '+')) != -1

    /**
     * Determines whether this topic is a valid 'shared subscription name', hence has the form:
     * `$share/{ShareName}/{filter}`.
     */
    fun isShared(): Boolean = shareRegex.matches(name)

    /**
     * When this is a shared subscription (see [isShared]), returns the name of the share and the remaining filter,
     * otherwise throws [IllegalStateException]. Hence, for a topic `$share/consumer1/sport/tennis/+` this method
     * returns `consumer1` as the share name and `sport/tennis/+` as the filter.
     */
    fun shareNameAndFilter(): Pair<String, Topic> {
        val match = checkNotNull(shareRegex.find(name)) {
            "'$name' is not a valid shared subscription"
        }
        return match.groupValues[1] to Topic(match.groupValues[2])
    }

    /** Returns `true` when [name] is not blank. */
    fun isNotBlank(): Boolean = name.isNotBlank()

    override fun toString(): String = name

    internal companion object {
        val shareRegex = Regex("""${'$'}share/([^+#/]+)/(.+)""")
    }
}

/**
 * Converts a list of strings into a list of Topic items.
 */
fun topics(vararg topic: String): List<Topic> = topic.map {
    Topic(it)
}
