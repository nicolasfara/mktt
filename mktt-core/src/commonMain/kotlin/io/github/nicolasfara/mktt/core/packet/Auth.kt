package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.AuthenticationData
import io.github.nicolasfara.mktt.core.AuthenticationMethod
import io.github.nicolasfara.mktt.core.ContinueAuthentication
import io.github.nicolasfara.mktt.core.ReAuthenticate
import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.single
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.wellFormedWhen
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source

/**
 * MQTT AUTH packet used for extended authentication exchanges.
 *
 * @property reason authentication exchange reason code.
 * @property authenticationMethod authentication method in use.
 * @property authenticationData optional authentication payload.
 * @property reasonString optional human-readable reason string.
 * @property userProperties optional user properties attached to this packet.
 */
data class Auth(
    val reason: ReasonCode,
    val authenticationMethod: AuthenticationMethod,
    val authenticationData: AuthenticationData? = null,
    val reasonString: ReasonString? = null,
    val userProperties: UserProperties = EMPTY,
) : BasePacket(PacketType.AUTH) {

    init {
        wellFormedWhen(
            when (reason.code) {
                Success.code -> true
                ContinueAuthentication.code -> true
                ReAuthenticate.code -> true
                else -> false
            },
        ) { "Invalid reason code for AUTH: $reason" }
    }
}

internal fun Sink.write(auth: Auth) {
    with(auth) {
        writeByte(reason.code.toByte())
        writeProperties(
            authenticationMethod,
            authenticationData,
            reasonString,
            *userProperties.asArray,
        )
    }
}

internal fun Source.readAuth(): Auth {
    val reason = ReasonCode.from(readByte())
    val properties = readProperties()

    return Auth(
        reason = reason,
        authenticationMethod = properties.single<AuthenticationMethod>(),
        authenticationData = properties.singleOrNull<AuthenticationData>(),
        reasonString = properties.singleOrNull<ReasonString>(),
        userProperties = UserProperties.from(properties),
    )
}
