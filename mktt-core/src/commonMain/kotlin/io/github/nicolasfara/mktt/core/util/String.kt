package io.github.nicolasfara.mktt.core.util

import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.ResponseInformation
import io.github.nicolasfara.mktt.core.ResponseTopic
import io.github.nicolasfara.mktt.core.Topic
import kotlin.toUShort
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.bytestring.decodeToString
import kotlinx.io.bytestring.encodeToByteString
import kotlinx.io.readByteString
import kotlinx.io.readUShort
import kotlinx.io.write
import kotlinx.io.writeUShort

private const val MAX_TEXT_SIZE = 65_535
private const val UTF8_ONE_BYTE_LIMIT = 0x80
private const val UTF8_TWO_BYTE_LIMIT = 0x800
private const val HIGH_SURROGATE_START = 0xD800
private const val HIGH_SURROGATE_END = 0xDBFF
private const val LOW_SURROGATE_START = 0xDC00
private const val LOW_SURROGATE_END = 0xDFFF

/** Converts this [String] to a [Topic]. */
fun String.toTopic(): Topic = Topic(this)

/** Converts this [String] to a [ResponseTopic]. */
fun String.toResponseTopic(): ResponseTopic = ResponseTopic(this)

/** Converts this [String] to [ResponseInformation]. */
fun String.toResponseInformation(): ResponseInformation = ResponseInformation(this)

/** Converts this [String] to a [ReasonString]. */
fun String.toReasonString(): ReasonString = ReasonString(this)

/**
 * Writes the specified string as an MQTT string, hence writing first the size of the string, then the ZTF-8 encoded
 * string.
 *
 * @throws io.github.nicolasfara.mktt.core.MalformedPacketException
 *   when the byte size of the string is larger than 65,535.
 */
internal fun Sink.writeMqttString(text: String) {
    val size = text.utf8Size()
    if (size > MAX_TEXT_SIZE) {
        throw MalformedPacketException(
            "Text '${
                text.substring(
                    0..100,
                )
            }...' is too large: $size (max allowed size: ${MAX_TEXT_SIZE})",
        )
    }

    writeUShort(size.toUShort())
    write(text.encodeToByteString())
}

internal fun Source.readMqttString(): String {
    val bytes = readByteString(readUShort().toInt())

    return bytes.decodeToString()
}

internal fun String.utf8Size(beginIndex: Int = 0, endIndex: Int = length): Int {
    var count = 0
    var i = beginIndex

    while (i < endIndex) {
        val c = this[i].code

        if (c < UTF8_ONE_BYTE_LIMIT) {
            // 7-bit character with 1 byte
            count++
            i++
        } else if (c < UTF8_TWO_BYTE_LIMIT) {
            // 11-bit character with 2 bytes
            count += 2
            i++
        } else if (c !in HIGH_SURROGATE_START..LOW_SURROGATE_END) {
            // 16-bit character with 3 bytes
            count += 3
            i++
        } else {
            val low = if (i + 1 < endIndex) this[i + 1].code else 0
            if (c > HIGH_SURROGATE_END || low < LOW_SURROGATE_START || low > LOW_SURROGATE_END) {
                // Malformed surrogate, which yields '?'
                count++
                i++
            } else {
                // 21-bit character with 4 bytes
                count += 4
                i += 2
            }
        }
    }

    return count
}
