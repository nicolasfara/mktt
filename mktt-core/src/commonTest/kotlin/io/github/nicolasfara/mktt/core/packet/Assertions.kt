package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.packet.readPacket
import io.github.nicolasfara.mktt.core.packet.write
import io.ktor.utils.io.*
import kotlinx.io.Buffer
import kotlin.test.assertEquals

/**
 * Writes the specified packet and re-reads it, asserts that the decoded packet is equal to the original packet.
 */
suspend fun assertEncodeDecodeOf(packet: io.github.nicolasfara.mktt.core.packet.Packet) {
    with(Buffer()) {
        write(packet)
        assertEquals(packet, ByteReadChannel(this).readPacket())
    }
}
