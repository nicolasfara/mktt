package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.ConnectionException
import io.github.nicolasfara.mktt.core.packet.Packet
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class PendingResponseRegistryTest {
    @Test
    fun `reset completes pending waiters with failure`() = runTest {
        val registry = PendingResponseRegistry(1.seconds)
        val pending = async {
            registry.awaitResponseOf<Packet>({ true }) {
                Result.success(Unit)
            }
        }
        runCurrent()

        registry.reset(ConnectionException("closed"))
        val result = pending.await()

        assertTrue(result.isFailure)
        assertIs<ConnectionException>(result.exceptionOrNull())
    }
}
