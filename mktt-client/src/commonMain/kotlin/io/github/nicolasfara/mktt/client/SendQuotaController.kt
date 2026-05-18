package io.github.nicolasfara.mktt.client

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock

internal class SendQuotaController(initialQuota: Int = DEFAULT_QUOTA) {
    private val semaphore = Semaphore(initialQuota)
    private val mutex = Mutex()
    private var inUse = 0

    suspend fun acquire() {
        semaphore.acquire()
        mutex.withLock {
            inUse += 1
        }
    }

    suspend fun release() {
        val shouldRelease = mutex.withLock {
            if (inUse == 0) {
                false
            } else {
                inUse -= 1
                true
            }
        }
        if (shouldRelease) {
            semaphore.release()
        }
    }

    suspend fun reset() {
        val permitsToRelease = mutex.withLock {
            val permitsToRelease = inUse
            inUse = 0
            permitsToRelease
        }
        repeat(permitsToRelease) {
            semaphore.release()
        }
    }

    suspend fun updateLimit(previousLimit: UShort, newLimit: UShort) {
        val difference = newLimit.toInt() - previousLimit.toInt()
        when {
            difference > 0 -> repeat(difference) {
                semaphore.release()
            }
            difference < 0 -> repeat(-difference) {
                semaphore.acquire()
            }
        }
    }

    private companion object {
        private const val DEFAULT_QUOTA = 65535
    }
}
