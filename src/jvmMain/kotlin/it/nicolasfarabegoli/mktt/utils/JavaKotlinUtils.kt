package it.nicolasfarabegoli.mktt.utils

import java.nio.ByteBuffer
import java.util.*

internal object JavaKotlinUtils {
    fun OptionalLong.toLongOrNull(): Long? = if (this.isPresent) this.asLong else null
    fun OptionalInt.toIntOrNull(): Int? = if (this.isPresent) this.asInt else null
    fun ByteBuffer.toByteArray(): ByteArray {
        // Duplicate the buffer to preserve its position, limit, and mark
        val duplicatedBuffer = duplicate()
        // Create a ByteArray with the same size as the buffer's remaining bytes
        val byteArray = ByteArray(duplicatedBuffer.remaining())
        // Transfer the bytes from the buffer to the array
        duplicatedBuffer.get(byteArray)
        return byteArray
    }
}
