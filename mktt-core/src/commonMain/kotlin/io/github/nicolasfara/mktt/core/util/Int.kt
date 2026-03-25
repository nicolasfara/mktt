// The ByteWriteChannel and ByteReadChannel versions are only required in `Packet` once,
// while the Source and Sink versions are used throughout packet parsing and writing.
// The duplicated code isn't nice, but due to the nature of parsing and writing cannot be
// avoided.
package io.github.nicolasfara.mktt.core.util

import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.readByte
import io.ktor.utils.io.writeByte
import kotlinx.io.Sink
import kotlinx.io.Source

private const val VARIABLE_BYTE_INT_BASE = 128
private const val VARIABLE_BYTE_INT_DATA_MASK = 127
private const val VARIABLE_BYTE_INT_CONTINUATION_BIT = 128
private const val VARIABLE_BYTE_INT_MAX_MULTIPLIER =
    VARIABLE_BYTE_INT_BASE * VARIABLE_BYTE_INT_BASE * VARIABLE_BYTE_INT_BASE

internal fun Sink.writeVariableByteInt(value: Int) {
    var x = value
    do {
        var encodedByte = x.rem(VARIABLE_BYTE_INT_BASE)
        x /= VARIABLE_BYTE_INT_BASE
        if (x > 0) {
            encodedByte = encodedByte or VARIABLE_BYTE_INT_CONTINUATION_BIT
        }
        writeByte(encodedByte.toByte())
    } while (x > 0)
}

internal suspend fun ByteWriteChannel.writeVariableByteInt(value: Int) {
    var x = value
    do {
        var encodedByte = x.rem(VARIABLE_BYTE_INT_BASE)
        x /= VARIABLE_BYTE_INT_BASE
        if (x > 0) {
            encodedByte = encodedByte or VARIABLE_BYTE_INT_CONTINUATION_BIT
        }
        writeByte(encodedByte.toByte())
    } while (x > 0)
}

internal fun Source.readVariableByteInt(): Int {
    val decoder = VariableByteIntDecoder()
    do {
        decoder.consume(readByte().toInt())
    } while (decoder.hasContinuation)

    return decoder.value
}

internal suspend fun ByteReadChannel.readVariableByteInt(): Int {
    val decoder = VariableByteIntDecoder()
    do {
        decoder.consume(readByte().toInt())
    } while (decoder.hasContinuation)

    return decoder.value
}

internal fun Int.variableByteIntSize(): Int {
    var x = this
    var count = 0
    do {
        x /= VARIABLE_BYTE_INT_BASE
        count++
    } while (x > 0)

    return count
}

private class VariableByteIntDecoder {
    var multiplier: Int = 1
        private set

    var value: Int = 0
        private set

    var hasContinuation: Boolean = false
        private set

    fun consume(encodedByte: Int) {
        value += (encodedByte and VARIABLE_BYTE_INT_DATA_MASK) * multiplier
        if (multiplier > VARIABLE_BYTE_INT_MAX_MULTIPLIER) {
            throw MalformedPacketException(
                "malformed variable byte integer",
            )
        }
        multiplier *= VARIABLE_BYTE_INT_BASE
        hasContinuation = (encodedByte and VARIABLE_BYTE_INT_CONTINUATION_BIT) != 0
    }
}
