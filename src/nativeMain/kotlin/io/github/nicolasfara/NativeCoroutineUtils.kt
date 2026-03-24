package io.github.nicolasfara

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

internal suspend inline fun <T> runSuspendCatching(crossinline block: suspend () -> T): Result<T> =
    runCatching { block() }.onFailure { error ->
        if (error is CancellationException) {
            throw error
        }
    }

internal suspend inline fun bestEffort(crossinline block: suspend () -> Unit) {
    withContext(NonCancellable) {
        runCatching { block() }
    }
}
