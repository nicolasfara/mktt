package io.github.nicolasfara.mktt.core

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ReceiveMaximumTest {

    @Test
    fun `zero not an allowed value`() {
        assertFailsWith<io.github.nicolasfara.mktt.core.MalformedPacketException> {
            _root_ide_package_.io.github.nicolasfara.mktt.core.ReceiveMaximum(
                0.toUShort(),
            )
        }
    }
}
