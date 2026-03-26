package io.github.nicolasfara.mktt.core.packet

import io.github.nicolasfara.mktt.core.ReasonCode
import io.github.nicolasfara.mktt.core.ReasonString
import io.github.nicolasfara.mktt.core.Success
import io.github.nicolasfara.mktt.core.UserProperties
import io.github.nicolasfara.mktt.core.asArray
import io.github.nicolasfara.mktt.core.readProperties
import io.github.nicolasfara.mktt.core.singleOrNull
import io.github.nicolasfara.mktt.core.wellFormedWhen
import io.github.nicolasfara.mktt.core.writeProperties
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.readUShort
import kotlinx.io.writeUShort

/**
 * Base class for PUBACK, PUBREC, PUBREL and PUBCOMP.
 *
 * @property packetIdentifier identifier of the related PUBLISH flow.
 * @property reason MQTT reason code carried by this response packet.
 * @property reasonString optional human-readable reason string.
 * @property userProperties optional user properties attached to the packet.
 */
sealed class PublishResponsePacket(
    type: PacketType,
    final override val packetIdentifier: UShort,
    val reason: ReasonCode,
    val reasonString: ReasonString? = null,
    val userProperties: UserProperties = UserProperties.EMPTY,
) : BasePacket(type),
    PacketIdentifierPacket {

    init {
        wellFormedWhen(packetIdentifier != 0.toUShort()) {
            "A zero packet identifier is not allowed for PUBACK, PUBREC , PUBREL, or PUBCOMP [MQTT-2.2.1-5]"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as PublishResponsePacket

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

/** MQTT PUBACK packet used for QoS 1 publish acknowledgment. */
class Puback(
    packetIdentifier: UShort,
    reason: ReasonCode,
    reasonString: ReasonString? = null,
    userProperties: UserProperties = UserProperties.EMPTY,
) : PublishResponsePacket(PacketType.PUBACK, packetIdentifier, reason, reasonString, userProperties) {

    /** Factory utilities for creating [Puback] packets. */
    companion object {
        /**
         * Creates a [Puback] from a [Publish] packet.
         */
        fun from(publish: Publish, reason: ReasonCode = Success, reasonString: String? = null): Puback {
            val packetIdentifier = requireNotNull(publish.packetIdentifier) {
                "Cannot create a PUBACK packet from a PUBLISH packet without packet identifier: $this"
            }
            return Puback(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }
    }
}

/** MQTT PUBREC packet used in the QoS 2 publish flow. */
class Pubrec(
    packetIdentifier: UShort,
    reason: ReasonCode,
    reasonString: ReasonString? = null,
    userProperties: UserProperties = UserProperties.EMPTY,
) : PublishResponsePacket(PacketType.PUBREC, packetIdentifier, reason, reasonString, userProperties) {

    /** Factory utilities for creating [Pubrec] packets. */
    companion object {
        /**
         * Creates a [Pubrec] from a [Publish] packet.
         */
        fun from(publish: Publish, reason: ReasonCode = Success, reasonString: String? = null): Pubrec {
            val packetIdentifier = requireNotNull(publish.packetIdentifier) {
                "Cannot create a PUBREC packet from a PUBLISH packet without packet identifier: $this"
            }
            return Pubrec(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }
    }
}

/** MQTT PUBREL packet used in the QoS 2 publish flow. */
class Pubrel(
    packetIdentifier: UShort,
    reason: ReasonCode,
    reasonString: ReasonString? = null,
    userProperties: UserProperties = UserProperties.EMPTY,
) : PublishResponsePacket(PacketType.PUBREL, packetIdentifier, reason, reasonString, userProperties) {

    // Note: this is the only response packet with a header flag!
    override val headerFlags: Int = 2

    /** Factory utilities for creating [Pubrel] packets. */
    companion object {
        /**
         * Creates a [Pubrel] from a [Publish] packet.
         */
        fun from(publish: Publish, reason: ReasonCode = Success, reasonString: String? = null): Pubrel {
            val packetIdentifier = requireNotNull(publish.packetIdentifier) {
                "Cannot create a PUBREL packet from a PUBLISH packet without packet identifier: $this"
            }
            return Pubrel(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }
    }
}

/** MQTT PUBCOMP packet used to complete the QoS 2 publish flow. */
class Pubcomp(
    packetIdentifier: UShort,
    reason: ReasonCode,
    reasonString: ReasonString? = null,
    userProperties: UserProperties = UserProperties.EMPTY,
) : PublishResponsePacket(PacketType.PUBCOMP, packetIdentifier, reason, reasonString, userProperties) {

    /** Factory utilities for creating [Pubcomp] packets. */
    companion object {
        /**
         * Creates a [Pubcomp] from a [Publish] packet.
         */
        fun from(publish: Publish, reason: ReasonCode = Success, reasonString: String? = null): Pubcomp {
            val packetIdentifier = requireNotNull(publish.packetIdentifier) {
                "Cannot create a PUBCOMP packet from a PUBLISH packet without packet identifier: $this"
            }
            return Pubcomp(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }

        /**
         * Creates a [Pubcomp] from another [PublishResponsePacket].
         */
        fun from(publish: PublishResponsePacket, reason: ReasonCode = Success, reasonString: String? = null): Pubcomp {
            val packetIdentifier = publish.packetIdentifier
            return Pubcomp(
                packetIdentifier = packetIdentifier,
                reason = reason,
                reasonString = reasonString?.let {
                    ReasonString(it)
                },
                userProperties = publish.userProperties,
            )
        }
    }
}

internal interface PublishResponseFactory<T : PublishResponsePacket> {
    operator fun invoke(
        packetIdentifier: UShort,
        reason: ReasonCode,
        reasonString: ReasonString? = null,
        userProperties: UserProperties = UserProperties.EMPTY,
    ): T
}

internal val PubackFactory = object :
    PublishResponseFactory<Puback> {
    override fun invoke(
        packetIdentifier: UShort,
        reason: ReasonCode,
        reasonString: ReasonString?,
        userProperties: UserProperties,
    ) = Puback(
        packetIdentifier,
        reason,
        reasonString,
        userProperties,
    )
}

internal val PubrecFactory = object :
    PublishResponseFactory<Pubrec> {
    override fun invoke(
        packetIdentifier: UShort,
        reason: ReasonCode,
        reasonString: ReasonString?,
        userProperties: UserProperties,
    ) = Pubrec(
        packetIdentifier,
        reason,
        reasonString,
        userProperties,
    )
}

internal val PubrelFactory = object :
    PublishResponseFactory<Pubrel> {
    override fun invoke(
        packetIdentifier: UShort,
        reason: ReasonCode,
        reasonString: ReasonString?,
        userProperties: UserProperties,
    ) = Pubrel(
        packetIdentifier,
        reason,
        reasonString,
        userProperties,
    )
}

internal val PubcompFactory = object :
    PublishResponseFactory<Pubcomp> {
    override fun invoke(
        packetIdentifier: UShort,
        reason: ReasonCode,
        reasonString: ReasonString?,
        userProperties: UserProperties,
    ) = Pubcomp(
        packetIdentifier,
        reason,
        reasonString,
        userProperties,
    )
}

internal fun Sink.write(publishResponse: PublishResponsePacket) {
    with(publishResponse) {
        writeUShort(packetIdentifier)
        if (reason != Success || reasonString != null ||
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

internal fun <T : PublishResponsePacket> Source.readPublishResponse(createResponse: PublishResponseFactory<T>): T {
    val packetIdentifier = readUShort()

    return if (!exhausted()) {
        val reason = ReasonCode.from(readByte())
        val properties = if (!exhausted()) {
            readProperties()
        } else {
            emptyList()
        }
        createResponse(
            packetIdentifier = packetIdentifier,
            reason = reason,
            reasonString = properties.singleOrNull<ReasonString>(),
            userProperties = UserProperties.from(
                properties,
            ),
        )
    } else {
        createResponse(packetIdentifier, Success)
    }
}
