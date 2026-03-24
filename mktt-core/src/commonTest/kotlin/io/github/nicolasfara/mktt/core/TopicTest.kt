package io.github.nicolasfara.mktt.core

import kotlin.test.*

class TopicTest {

    @Test
    fun `wildcards detected`() {
        listOf(
            "" to false,
            "sport/tennis" to false,
            "#" to true,
            "+" to true,
            "#+" to true,
            "sport/tennis/player1/#" to true,
            "sport/tennis/+" to true,
        ).forEach {
            _root_ide_package_.kotlin.test.assertEquals(
                it.second,
                _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(it.first).containsWildcard(),
                "'${it.first}' wildcard not matching",
            )
        }
    }

    @Test
    fun `shared filter detected`() {
        assertFalse(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic("abc/def").isShared()) // Not a share
        assertFalse(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic($$"$share").isShared()) // Missing share name
        assertFalse(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic($$"$share/").isShared()) // Missing share name
        assertFalse(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic($$"$share/name#").isShared()) // Invalid share name
        assertFalse(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic($$"$share/name+/filter").isShared()) // Invalid share name
        assertFalse(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic($$"$share/name/").isShared()) // Missing filter

        assertTrue(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic($$"$share/name/filter").isShared())
        assertTrue(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic($$"$share/name/filter#").isShared())
        assertTrue(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic($$"$share/name/filter/+").isShared())
    }

    @Test
    fun `return share name and filter`() {
        val (name, filter) = _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(
            $$"$share/consumer1/sport/tennis/+",
        )
            .shareNameAndFilter()
        assertEquals("consumer1", name)
        assertEquals(_root_ide_package_.io.github.nicolasfara.mktt.core.Topic("sport/tennis/+"), filter)

        assertFailsWith<IllegalStateException> {
            _root_ide_package_.io.github.nicolasfara.mktt.core.Topic("abc/def").shareNameAndFilter()
        }
    }

    @Test
    fun `toString returns topic name only`() {
        val name = "test/topic"
        assertEquals(name, _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(name).toString())
    }
}
