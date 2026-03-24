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
public value class Topic(public val name: String) {

    public fun containsWildcard(): Boolean = name.indexOfAny(charArrayOf('#', '+')) != -1

    /**
     * Determines whether this topic is a valid 'shared subscription name', hence has the form:
     * `$share/{ShareName}/{filter}`
     */
    public fun isShared(): Boolean =
        _root_ide_package_.io.github.nicolasfara.mktt.core.Topic.Companion.shareRegex.matches(name)

    /**
     * When this is a shared subscription (see [isShared]), returns the name of the share and the remaining filter,
     * otherwise throws [IllegalStateException]. Hence, for a topic `$share/consumer1/sport/tennis/+` this method
     * returns `consumer1` as the share name and `sport/tennis/+` as the filter.
     */
    public fun shareNameAndFilter(): Pair<String, io.github.nicolasfara.mktt.core.Topic> {
        _root_ide_package_.io.github.nicolasfara.mktt.core.Topic.Companion.shareRegex.find(name)?.let { m ->
            return m.groupValues[1] to _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(m.groupValues[2])
        }

        throw IllegalStateException("'$name' is not a valid shared subscription")
    }

    public fun isNotBlank(): Boolean = name.isNotBlank()

    override fun toString(): String = name

    internal companion object {

        val shareRegex = Regex($$"""\$share/([^+#/]+)/(.+)""")
    }
}

/**
 * Converts a list of strings into a list of Topic items.
 */
public fun topics(vararg topic: String): List<io.github.nicolasfara.mktt.core.Topic> = topic.map {
    _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(
        it,
    )
}
