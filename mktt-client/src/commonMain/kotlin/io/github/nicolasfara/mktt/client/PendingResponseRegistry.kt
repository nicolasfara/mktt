package io.github.nicolasfara.mktt.client

import io.github.nicolasfara.mktt.core.ConnectionException
import io.github.nicolasfara.mktt.core.TimeoutException
import io.github.nicolasfara.mktt.core.packet.Packet
import kotlin.coroutines.cancellation.CancellationException
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
        val response = PendingResponse(deferred) { packet: Packet ->
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

        return try {
            val requestFailure = request().exceptionOrNull()
            if (requestFailure == null) {
                awaitResponse(deferred)
            } else {
                Result.failure(requestFailure)
            }
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

    suspend fun reset(
        cause: ConnectionException = ConnectionException("Connection closed while waiting for response"),
    ) {
        val removed = mutex.withLock {
            pendingResponses.toList().also {
                pendingResponses.clear()
            }
        }
        removed.forEach {
            it.completeExceptionally(cause)
        }
    }

    private suspend fun remove(response: PendingResponse) {
        mutex.withLock {
            pendingResponses.remove(response)
        }
    }

    private suspend fun <P : Packet> awaitResponse(deferred: CompletableDeferred<P>): Result<P> = try {
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
    } catch (e: CancellationException) {
        throw e
    } catch (e: ConnectionException) {
        Result.failure(e)
    }
}

internal class PendingResponse(
    private val deferred: CompletableDeferred<*>,
    private val matcher: (Packet) -> Boolean,
) {
    fun tryComplete(packet: Packet): Boolean = matcher(packet)

    fun completeExceptionally(cause: Throwable): Boolean = deferred.completeExceptionally(cause)
}
