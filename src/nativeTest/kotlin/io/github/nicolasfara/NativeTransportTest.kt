package io.github.nicolasfara

import io.ktor.utils.io.errors.PosixException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class NativeTransportTest {
    @Test
    fun ignoresErrnoZeroSocketCloseFailures() {
        val error = PosixException.forErrno(0)

        assertTrue(error.isIgnorableNativeSocketCloseFailure())
    }

    @Test
    fun ignoresWrappedErrnoZeroSocketCloseFailures() {
        val error = IllegalStateException("wrapper", PosixException.forErrno(0))

        assertTrue(error.isIgnorableNativeSocketCloseFailure())
    }

    @Test
    fun keepsNonZeroSocketFailuresVisible() {
        val error = PosixException.forErrno(1)

        assertFalse(error.isIgnorableNativeSocketCloseFailure())
    }
}
