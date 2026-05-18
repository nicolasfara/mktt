package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.AssignedClientIdentifier
import io.github.nicolasfara.mktt.core.AuthenticationData
import io.github.nicolasfara.mktt.core.AuthenticationMethod
import io.github.nicolasfara.mktt.core.MalformedPacket
import io.github.nicolasfara.mktt.core.MaximumPacketSize
import io.github.nicolasfara.mktt.core.MaximumQoS
import io.github.nicolasfara.mktt.core.ReAuthenticate
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.ReceiveMaximum
import io.github.nicolasfara.mktt.core.ResponseInformation
import io.github.nicolasfara.mktt.core.RetainAvailable
import io.github.nicolasfara.mktt.core.ServerKeepAlive
import io.github.nicolasfara.mktt.core.ServerReference
import io.github.nicolasfara.mktt.core.SessionExpiryInterval
import io.github.nicolasfara.mktt.core.SharedSubscriptionAvailable
import io.github.nicolasfara.mktt.core.SubscriptionIdentifierAvailable
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.TopicAliasMaximum
import io.github.nicolasfara.mktt.core.WildcardSubscriptionAvailable
import io.github.nicolasfara.mktt.core.buildUserProperties
import io.ktor.utils.io.core.buildPacket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.encodeToByteString

class ConnackTest {

    @Test
    fun `encode and decode returns same packet`() = runTest {
        assertEncodeDecodeOf(Connack(true, Success))
        assertEncodeDecodeOf(
            Connack(
                false,
                MalformedPacket,
                SessionExpiryInterval(1u),
                ReceiveMaximum(42u),
                MaximumQoS(0.toByte()),
                RetainAvailable(true),
                MaximumPacketSize(2000u),
                AssignedClientIdentifier("123"),
                TopicAliasMaximum(5u),
                ReasonString("error"),
                buildUserProperties { "key" to "value" },
                WildcardSubscriptionAvailable(true),
                SubscriptionIdentifierAvailable(true),
                SharedSubscriptionAvailable(true),
                ServerKeepAlive(180u),
                ResponseInformation("response-info"),
                ServerReference("ref"),
                AuthenticationMethod("auth"),
                AuthenticationData("123".encodeToByteString()),
            ),
        )
    }

    @Test
    fun `all bytes are written correctly`() {
        val userProperties = buildUserProperties {
            "key1" to "value1"
            "key2" to "value2"
        }
        val connack = Connack(
            isSessionPresent = true,
            reason = ReAuthenticate,
            receiveMaximum = ReceiveMaximum(27u),
            serverKeepAlive = ServerKeepAlive(99u),
            userProperties = userProperties,
        )

        val reader = buildPacket {
            write(connack)
        }

        val actual = reader.readConnack()

        assertTrue(actual.isSessionPresent)
        assertEquals(ReAuthenticate, actual.reason)
        assertEquals(27u, actual.receiveMaximum?.value)
        assertEquals(99u, actual.serverKeepAlive?.value)
        assertEquals(userProperties, actual.userProperties)
        assertNull(actual.sessionExpiryInterval)
        assertNull(actual.maximumQoS)
        assertNull(actual.retainAvailable)
        assertNull(actual.maximumPacketSize)
        assertNull(actual.assignedClientIdentifier)
        assertNull(actual.topicAliasMaximum)
        assertNull(actual.reasonString)
        assertNull(actual.wildcardSubscriptionAvailable)
        assertNull(actual.subscriptionIdentifierAvailable)
        assertNull(actual.sharedSubscriptionAvailable)
        assertNull(actual.responseInformation)
        assertNull(actual.serverReference)
        assertNull(actual.authenticationMethod)
        assertNull(actual.authenticationData)
    }
}
