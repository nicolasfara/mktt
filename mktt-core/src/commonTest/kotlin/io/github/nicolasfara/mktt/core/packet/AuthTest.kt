package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.AuthenticationData
import io.github.nicolasfara.mktt.core.AuthenticationMethod
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.buildUserProperties
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.encodeToByteString
import kotlin.test.Test

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
}
