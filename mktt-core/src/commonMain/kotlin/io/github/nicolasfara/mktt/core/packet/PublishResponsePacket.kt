package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.*
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

/**
 * Base class for PUBACK, PUBREC, PUBREL and PUBCOMP.
 */
public sealed class PublishResponsePacket(
    type: io.github.nicolasfara.mktt.core.packet.PacketType,
    public final override val packetIdentifier: UShort,
    public val reason: io.github.nicolasfara.mktt.core.ReasonCode,
    public val reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    public val userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.AbstractPacket(type),
    io.github.nicolasfara.mktt.core.packet.PacketIdentifierPacket {

    init {
        _root_ide_package_.io.github.nicolasfara.mktt.core.wellFormedWhen(packetIdentifier != 0.toUShort()) {
            "A zero packet identifier is not allowed for PUBACK, PUBREC , PUBREL, or PUBCOMP [MQTT-2.2.1-5]"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as io.github.nicolasfara.mktt.core.packet.PublishResponsePacket

        if (packetIdentifier != other.packetIdentifier) return false
        if (reason != other.reason) return false
        if (reasonString != other.reasonString) return false
        if (userProperties != other.userProperties) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packetIdentifier.hashCode()
        result = 31 * result + reason.hashCode()
        result = 31 * result + (reasonString?.hashCode() ?: 0)
        result = 31 * result + userProperties.hashCode()
        return result
    }

    override fun toString(): String =
        "${this::class.simpleName}(packetIdentifier=$packetIdentifier, reason=$reason, reasonString=$reasonString, userProperties=$userProperties)"
}

public class Puback(
    packetIdentifier: UShort,
    reason: io.github.nicolasfara.mktt.core.ReasonCode,
    reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.PublishResponsePacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBACK,
    packetIdentifier,
    reason,
    reasonString,
    userProperties,
) {

    public companion object {

        public fun from(
            publish: io.github.nicolasfara.mktt.core.packet.Publish,
            reason: io.github.nicolasfara.mktt.core.ReasonCode = _root_ide_package_.io.github.nicolasfara.mktt.core.Success,
            reasonString: String? = null,
        ): io.github.nicolasfara.mktt.core.packet.Puback {
            val packetIdentifier = publish.packetIdentifier
            require(packetIdentifier != null) {
                "Cannot create a PUBACK packet from a PUBLISH packet without packet identifier: $this"
            }

            return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Puback(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }
    }
}

public class Pubrec(
    packetIdentifier: UShort,
    reason: io.github.nicolasfara.mktt.core.ReasonCode,
    reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.PublishResponsePacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBREC,
    packetIdentifier,
    reason,
    reasonString,
    userProperties,
) {

    public companion object {

        public fun from(
            publish: io.github.nicolasfara.mktt.core.packet.Publish,
            reason: io.github.nicolasfara.mktt.core.ReasonCode = _root_ide_package_.io.github.nicolasfara.mktt.core.Success,
            reasonString: String? = null,
        ): io.github.nicolasfara.mktt.core.packet.Pubrec {
            val packetIdentifier = publish.packetIdentifier
            require(packetIdentifier != null) {
                "Cannot create a PUBREC packet from a PUBLISH packet without packet identifier: $this"
            }

            return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubrec(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }
    }
}

public class Pubrel(
    packetIdentifier: UShort,
    reason: io.github.nicolasfara.mktt.core.ReasonCode,
    reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.PublishResponsePacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBREL,
    packetIdentifier,
    reason,
    reasonString,
    userProperties,
) {

    // Note: this is the only response packet with a header flag!
    override val headerFlags: Int = 2

    public companion object {

        public fun from(
            publish: io.github.nicolasfara.mktt.core.packet.Publish,
            reason: io.github.nicolasfara.mktt.core.ReasonCode = _root_ide_package_.io.github.nicolasfara.mktt.core.Success,
            reasonString: String? = null,
        ): io.github.nicolasfara.mktt.core.packet.Pubrel {
            val packetIdentifier = publish.packetIdentifier
            require(packetIdentifier != null) {
                "Cannot create a PUBREL packet from a PUBLISH packet without packet identifier: $this"
            }

            return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubrel(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }
    }
}

public class Pubcomp(
    packetIdentifier: UShort,
    reason: io.github.nicolasfara.mktt.core.ReasonCode,
    reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
    userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
) : io.github.nicolasfara.mktt.core.packet.PublishResponsePacket(
    _root_ide_package_.io.github.nicolasfara.mktt.core.packet.PacketType.PUBCOMP,
    packetIdentifier,
    reason,
    reasonString,
    userProperties,
) {

    public companion object {

        public fun from(
            publish: io.github.nicolasfara.mktt.core.packet.Publish,
            reason: io.github.nicolasfara.mktt.core.ReasonCode = _root_ide_package_.io.github.nicolasfara.mktt.core.Success,
            reasonString: String? = null,
        ): io.github.nicolasfara.mktt.core.packet.Pubcomp {
            val packetIdentifier = publish.packetIdentifier
            require(packetIdentifier != null) {
                "Cannot create a PUBCOMP packet from a PUBLISH packet without packet identifier: $this"
            }

            return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubcomp(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }

        public fun from(
            publish: io.github.nicolasfara.mktt.core.packet.PublishResponsePacket,
            reason: io.github.nicolasfara.mktt.core.ReasonCode = _root_ide_package_.io.github.nicolasfara.mktt.core.Success,
            reasonString: String? = null,
        ): io.github.nicolasfara.mktt.core.packet.Pubcomp {
            val packetIdentifier = publish.packetIdentifier

            return _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubcomp(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }
    }
}

internal interface PublishResponseFactory<T : io.github.nicolasfara.mktt.core.packet.PublishResponsePacket> {

    operator fun invoke(
        packetIdentifier: UShort,
        reason: io.github.nicolasfara.mktt.core.ReasonCode,
        reasonString: io.github.nicolasfara.mktt.core.ReasonString? = null,
        userProperties: io.github.nicolasfara.mktt.core.UserProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.EMPTY,
    ): T
}

internal val PubackFactory = object :
    io.github.nicolasfara.mktt.core.packet.PublishResponseFactory<io.github.nicolasfara.mktt.core.packet.Puback> {
    override fun invoke(
        packetIdentifier: UShort,
        reason: io.github.nicolasfara.mktt.core.ReasonCode,
        reasonString: io.github.nicolasfara.mktt.core.ReasonString?,
        userProperties: io.github.nicolasfara.mktt.core.UserProperties,
    ) = _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Puback(
        packetIdentifier,
        reason,
        reasonString,
        userProperties,
    )
}

internal val PubrecFactory = object :
    io.github.nicolasfara.mktt.core.packet.PublishResponseFactory<io.github.nicolasfara.mktt.core.packet.Pubrec> {
    override fun invoke(
        packetIdentifier: UShort,
        reason: io.github.nicolasfara.mktt.core.ReasonCode,
        reasonString: io.github.nicolasfara.mktt.core.ReasonString?,
        userProperties: io.github.nicolasfara.mktt.core.UserProperties,
    ) = _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubrec(
        packetIdentifier,
        reason,
        reasonString,
        userProperties,
    )
}

internal val PubrelFactory = object :
    io.github.nicolasfara.mktt.core.packet.PublishResponseFactory<io.github.nicolasfara.mktt.core.packet.Pubrel> {
    override fun invoke(
        packetIdentifier: UShort,
        reason: io.github.nicolasfara.mktt.core.ReasonCode,
        reasonString: io.github.nicolasfara.mktt.core.ReasonString?,
        userProperties: io.github.nicolasfara.mktt.core.UserProperties,
    ) = _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubrel(
        packetIdentifier,
        reason,
        reasonString,
        userProperties,
    )
}

internal val PubcompFactory = object :
    io.github.nicolasfara.mktt.core.packet.PublishResponseFactory<io.github.nicolasfara.mktt.core.packet.Pubcomp> {
    override fun invoke(
        packetIdentifier: UShort,
        reason: io.github.nicolasfara.mktt.core.ReasonCode,
        reasonString: io.github.nicolasfara.mktt.core.ReasonString?,
        userProperties: io.github.nicolasfara.mktt.core.UserProperties,
    ) = _root_ide_package_.io.github.nicolasfara.mktt.core.packet.Pubcomp(
        packetIdentifier,
        reason,
        reasonString,
        userProperties,
    )
}

internal fun Sink.write(publishResponse: io.github.nicolasfara.mktt.core.packet.PublishResponsePacket) {
    with(publishResponse) {
        writeUShort(packetIdentifier)
        if (reason != _root_ide_package_.io.github.nicolasfara.mktt.core.Success || reasonString != null ||
            userProperties.isNotEmpty()
        ) {
            writeByte(reason.code.toByte())
            writeProperties(
                reasonString,
                *userProperties.asArray,
            )
        }
    }
}

internal fun <T : io.github.nicolasfara.mktt.core.packet.PublishResponsePacket> Source.readPublishResponse(
    createResponse: io.github.nicolasfara.mktt.core.packet.PublishResponseFactory<T>,
): T {
    val packetIdentifier = readUShort()

    return if (!exhausted()) {
        val reason = _root_ide_package_.io.github.nicolasfara.mktt.core.ReasonCode.Companion.from(readByte())
        val properties = if (!exhausted()) {
            readProperties()
        } else {
            emptyList()
        }
        createResponse(
            packetIdentifier = packetIdentifier,
            reason = reason,
            reasonString = properties.singleOrNull<io.github.nicolasfara.mktt.core.ReasonString>(),
            userProperties = _root_ide_package_.io.github.nicolasfara.mktt.core.UserProperties.Companion.from(
                properties,
            ),
        )
    } else {
        createResponse(packetIdentifier, _root_ide_package_.io.github.nicolasfara.mktt.core.Success)
    }
}
