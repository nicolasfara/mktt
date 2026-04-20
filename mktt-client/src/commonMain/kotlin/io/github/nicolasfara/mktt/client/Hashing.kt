package io.github.nicolasfara.mktt.client

private const val COMBINED_HASH_MULTIPLIER = 31

internal fun combinedHashCode(vararg values: Any?): Int = values.fold(1) { hash, value ->
    COMBINED_HASH_MULTIPLIER * hash + (value?.hashCode() ?: 0)
}
