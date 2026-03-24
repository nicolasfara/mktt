package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.buildUserProperties
import io.github.nicolasfara.mktt.core.topics
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class UnsubscribeTest {

    @Test
    fun `encode and decode returns same packet`() = runTest {
        assertEncodeDecodeOf(Unsubscribe(6677u, topics("test/topic")))
        assertEncodeDecodeOf(
            Unsubscribe(
                6677u,
                topics(
                    "test/topic/1",
                    "test/topic/2",
                    "test/topic/3",
                ),
                buildUserProperties { "key" to "value" },
            ),
        )
    }
}
