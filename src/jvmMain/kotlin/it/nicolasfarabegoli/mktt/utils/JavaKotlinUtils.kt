package it.nicolasfarabegoli.mktt.utils

import java.util.OptionalInt
import java.util.OptionalLong

internal object JavaKotlinUtils {
    fun OptionalLong.toLongOrNull(): Long? = if (this.isPresent) this.asLong else null
    fun OptionalInt.toIntOrNull(): Int? = if (this.isPresent) this.asInt else null
}
