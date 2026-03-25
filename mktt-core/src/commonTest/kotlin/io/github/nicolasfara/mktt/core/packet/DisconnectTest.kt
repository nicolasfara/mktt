package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.DisconnectWithWillMessage
import io.github.nicolasfara.mktt.core.GrantedQoS0
import io.github.nicolasfara.mktt.core.MalformedPacketException
import io.github.nicolasfara.mktt.core.NormalDisconnection
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.UnspecifiedError
import io.github.nicolasfara.mktt.core.buildUserProperties
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class DisconnectTest {

    @Test
    fun `encode and decode returns same packet`() = runTest {
        assertEncodeDecodeOf(Disconnect(NormalDisconnection))
        assertEncodeDecodeOf(
            Disconnect(
                NormalDisconnection,
                SessionExpiryInterval(60u),
                ReasonString("reason"),
                buildUserProperties { "user" to "value" },
            ),
        )
    }

    @Test
    fun `constructor fails when reason code is Success or GrantedQoS0`() {
        Disconnect(NormalDisconnection)
        Disconnect(DisconnectWithWillMessage)
        Disconnect(UnspecifiedError)

        assertFailsWith<MalformedPacketException> { Disconnect(Success) }
        assertFailsWith<MalformedPacketException> { Disconnect(GrantedQoS0) }
    }
}
