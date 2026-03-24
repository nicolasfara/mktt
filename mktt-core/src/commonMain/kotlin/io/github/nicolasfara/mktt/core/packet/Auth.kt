package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.single
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source

public data class Auth(
    val reason: io.github.nicolasfara.mktt.core.ReasonCode,
    val authenticationMethod: io.github.nicolasfara.mktt.core.AuthenticationMethod,
    val authenticationData: io.github.nicolasfara.mktt.core.AuthenticationData? = null,
    val reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.AUTH,
) {

    init {
        _root_ide_package_.io.github.nicolasfara.mktt.core.wellFormedWhen(
            when (reason.code) {
                _root_ide_package_.io.github.nicolasfara.mktt.core.Success.code -> true
                _root_ide_package_.io.github.nicolasfara.mktt.core.ContinueAuthentication.code -> true
                _root_ide_package_.io.github.nicolasfara.mktt.core.ReAuthenticate.code -> true
                else -> false
            },
        ) { "Invalid reason code for AUTH: $reason" }
    }
}

internal fun Sink.write(auth: io.github.nicolasfara.mktt.core.packet.Auth) {
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

internal fun Source.readAuth(): io.github.nicolasfara.mktt.core.packet.Auth {
    val reason = _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode.Companion.from(readByte())
    val properties = readProperties()

    return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Auth(
        reason = reason,
        authenticationMethod = properties.single<io.github.nicolasfara.mktt.core.AuthenticationMethod>(),
        authenticationData = properties.singleOrNull<io.github.nicolasfara.mktt.core.AuthenticationData>(),
        reasonString = properties.singleOrNull<io.github.nicolasfara.mktt.core.ReasonString>(),
        userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(properties),
    )
}
