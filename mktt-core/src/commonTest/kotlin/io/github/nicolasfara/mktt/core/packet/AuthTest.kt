package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.AuthenticationData
import io.github.nicolasfara.mktt.core.AuthenticationMethod
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.buildUserProperties
import io.github.nicolasfara.mktt.core.util.readVariableByteInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.bytestring.encodeToByteString

class AuthTest {

    @Test
    fun `encode and decode returns same packet`() = runTest {
        assertEncodeDecodeOf(Auth(Success, AuthenticationMethod("auth")))
        assertEncodeDecodeOf(
            Auth(
                Success,
                AuthenticationMethod("auth"),
                AuthenticationData("data".encodeToByteString()),
                ReasonString("reason"),
                buildUserProperties { "key" to "prop" },
            ),
        )
    }

    @Test
    fun `authentication data contributes its length prefix to properties length`() {
        val reader = Buffer().apply {
            write(
                Auth(
                    Success,
                    AuthenticationMethod("auth"),
                    AuthenticationData("data".encodeToByteString()),
                ),
            )
        }

        assertEquals(Success.code.toByte(), reader.readByte())
        assertEquals(14, reader.readVariableByteInt())
    }
}
