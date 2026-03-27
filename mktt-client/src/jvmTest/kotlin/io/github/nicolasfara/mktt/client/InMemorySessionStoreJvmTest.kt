package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.InFlightPublish
import io.github.nicolasfara.mktt.core.QoS
import io.github.nicolasfara.mktt.core.Topic
import io.github.nicolasfara.mktt.core.packet.Publish
import io.github.nicolasfara.mktt.core.packet.Pubrel
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.io.bytestring.ByteString

class InMemorySessionStoreJvmTest {
    @Test
    fun `rememberIncomingPacketId follows SessionStore contract`() {
        val store = InMemorySessionStore()
        val publish = qos1Publish(7u)

        assertTrue(store.rememberIncomingPacketId(publish))
        assertFalse(store.rememberIncomingPacketId(publish))
        assertTrue(store.hasIncomingPacketId(publish))

        store.releaseIncomingPacketId(Pubrel.from(publish))

        assertFalse(store.hasIncomingPacketId(publish))
    }

    @Test
    fun `replace fails when no matching in flight publish exists`() {
        val store = InMemorySessionStore()
        val inFlightPublish = InFlightPublish(
            source = qos1Publish(42u),
            timestamp = kotlin.time.Clock.System.now(),
            id = 1,
        )

        assertFailsWith<NoSuchElementException> {
            store.replace(inFlightPublish)
        }
    }

    @Test
    fun `concurrent rememberIncomingPacketId only adds once`() {
        val store = InMemorySessionStore()
        val publish = qos1Publish(3u)
        val executor = Executors.newFixedThreadPool(8)
        val dispatcher = executor.asCoroutineDispatcher()

        try {
            val results = runBlocking {
                (1..256).map {
                    async(dispatcher) {
                        store.rememberIncomingPacketId(publish)
                    }
                }.awaitAll()
            }

            assertEquals(1, results.count { it })
            assertTrue(store.hasIncomingPacketId(publish))
        } finally {
            dispatcher.close()
            executor.shutdown()
        }
    }

    private fun qos1Publish(packetIdentifier: UInt): Publish = Publish(
        qoS = QoS.AT_LEAST_ONCE,
        packetIdentifier = packetIdentifier.toUShort(),
        topic = Topic("test/topic"),
        payload = ByteString("payload".encodeToByteArray()),
    )
}
