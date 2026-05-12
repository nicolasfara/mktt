package io.github.nicolasfara.mktt.core

import kotlin.test.Test
import kotlin.test.assertFailsWith

class MaximumQoSTest {

    @Test
    fun `qos 2 is not an allowed maximum qos property value`() {
        assertFailsWith<MalformedPacketException> {
            MaximumQoS(2)
        }
    }
}
