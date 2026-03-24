package io.github.nicolasfara.mktt.core

import kotlin.test.Test
import kotlin.test.assertFailsWith

class MaximumPacketSizeTest {

    @Test
    fun `zero not an allowed value`() {
        assertFailsWith<io.github.nicolasfara.mktt.core.MalformedPacketException> {
            _root_ide_package_.io.github.nicolasfara.mktt.core.MaximumPacketSize(
                0.toUInt(),
            )
        }
    }
}
