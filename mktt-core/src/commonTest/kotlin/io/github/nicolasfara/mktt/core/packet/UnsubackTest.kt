package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.NotAuthorized
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.buildUserProperties
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UnsubackTest {

    @Test
    fun `encode and decode returns same packet`() = runTest {
        assertEncodeDecodeOf(Unsuback(UShort.MAX_VALUE, listOf(Success)))
        assertEncodeDecodeOf(
            Unsuback(
                1u,
                listOf(Success, NotAuthorized),
                ReasonString("reason"),
                buildUserProperties { "key" to "value" },
            ),
        )
    }
}
