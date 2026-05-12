package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest

class PacketTest {
    @Test
    fun `readPacket rejects invalid fixed header flags`() = runTest {
        val pingreqWithRetainFlag = byteArrayOf(0xC1.toByte(), 0)

        assertFailsWith<MalformedPacketException> {
            ByteReadChannel(pingreqWithRetainFlag).readPacket()
        }
    }

    @Test
    fun `readPacket rejects unread remaining body bytes`() = runTest {
        val pingreqWithBody = byteArrayOf(0xC0.toByte(), 1, 0)

        assertFailsWith<MalformedPacketException> {
            ByteReadChannel(pingreqWithBody).readPacket()
        }
    }
}
