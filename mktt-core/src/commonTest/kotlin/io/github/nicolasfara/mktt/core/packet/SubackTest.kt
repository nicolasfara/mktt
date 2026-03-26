package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.GrantedQoS0
import io.github.nicolasfara.mktt.core.GrantedQoS1
import io.github.nicolasfara.mktt.core.GrantedQoS2
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.buildUserProperties
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SubackTest {

    @Test
    fun `encode and decode returns same packet`() = runTest {
        assertEncodeDecodeOf(Suback(42u, listOf(GrantedQoS0)))
        assertEncodeDecodeOf(
            Suback(
                42u,
                listOf(GrantedQoS1, GrantedQoS2),
                ReasonString("reason"),
                buildUserProperties { "key" to "value" },
            ),
        )
    }
}
