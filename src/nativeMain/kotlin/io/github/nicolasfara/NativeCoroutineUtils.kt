package io.github.nicolasfara

import kotlinx.coroutines.CancellationException

@Suppress("TooGenericExceptionCaught")
internal suspend inline fun <T> recoverNonCancellation(
    crossinline block: suspend () -> T,
    crossinline onFailure: suspend (Throwable) -> T,
): T = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: Throwable) {
    onFailure(error)
}

internal suspend inline fun ignoreNonCancellation(crossinline block: suspend () -> Unit) {
    recoverNonCancellation(
        block = { block() },
        onFailure = { },
    )
}

@Suppress("TooGenericExceptionCaught")
internal suspend inline fun <T> cleanupOnFailure(
    crossinline block: suspend () -> T,
    crossinline onFailure: suspend (Throwable) -> Unit,
): T = try {
    block()
} catch (error: CancellationException) {
    onFailure(error)
    throw error
} catch (error: Throwable) {
    onFailure(error)
    throw error
}
