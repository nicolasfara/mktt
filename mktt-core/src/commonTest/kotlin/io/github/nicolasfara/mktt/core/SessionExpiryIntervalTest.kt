package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.toDuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

class SessionExpiryIntervalTest {

    @Test
    fun `duration computed correctly for values greater than Int_MAX_VALUE`() {
        val maxIntPlusOne = 2147483648
        val expiry = _root_ide_package_.io.github.nicolasfara.mktt.core.SessionExpiryInterval(maxIntPlusOne.toUInt())
        assertEquals(maxIntPlusOne, expiry.toDuration().inWholeSeconds)
    }

    @Test
    fun `duration computed correctly for infinite value`() {
        val expiry = _root_ide_package_.io.github.nicolasfara.mktt.core.SessionExpiryInterval(UInt.MAX_VALUE)
        assertEquals(Duration.INFINITE, expiry.toDuration())
    }
}
