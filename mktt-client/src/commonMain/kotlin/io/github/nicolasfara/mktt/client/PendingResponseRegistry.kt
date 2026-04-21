package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.TimeoutException
import io.github.nicolasfara.mktt.core.packet.Packet
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

internal class PendingResponseRegistry(private val ackMessageTimeout: kotlin.time.Duration) {
    private val mutex = Mutex()
    private val pendingResponses = mutableListOf<PendingResponse>()

    suspend inline fun <reified P : Packet> awaitResponseOf(
        noinline predicate: (P) -> Boolean,
        crossinline request: suspend () -> Result<Unit>,
    ): Result<P> {
        val deferred = CompletableDeferred<P>()
        val response = PendingResponse { packet: Packet ->
            val typedPacket = packet as? P ?: return@PendingResponse false
            if (!predicate(typedPacket)) {
                return@PendingResponse false
            }
            deferred.complete(typedPacket)
            true
        }

        mutex.withLock {
            pendingResponses += response
        }

        val requestResult = request()
        requestResult.onFailure {
            remove(response)
            return Result.failure(it)
        }

        return try {
            withTimeout(ackMessageTimeout) {
                Result.success(deferred.await())
            }
        } catch (e: TimeoutCancellationException) {
            Result.failure(
                TimeoutException(
                    "Didn't receive requested packet within $ackMessageTimeout",
                    e,
                ),
            )
        } finally {
            withContext(NonCancellable) {
                remove(response)
            }
        }
    }

    suspend fun dispatch(packet: Packet) {
        mutex.withLock {
            val iterator = pendingResponses.iterator()
            while (iterator.hasNext()) {
                if (iterator.next().tryComplete(packet)) {
                    iterator.remove()
                    return
                }
            }
        }
    }

    suspend fun reset() {
        mutex.withLock {
            pendingResponses.clear()
        }
    }

    private suspend fun remove(response: PendingResponse) {
        mutex.withLock {
            pendingResponses.remove(response)
        }
    }
}

internal class PendingResponse(
    private val matcher: (Packet) -> Boolean,
) {
    fun tryComplete(packet: Packet): Boolean = matcher(packet)
}
