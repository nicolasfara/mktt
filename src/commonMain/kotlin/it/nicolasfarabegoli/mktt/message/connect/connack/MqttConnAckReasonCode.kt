package it.nicolasfarabegoli.mktt.message.connect.connack

import it.nicolasfarabegoli.mktt.message.MqttCommonReasonCode
import it.nicolasfarabegoli.mktt.message.MqttReasonCode

/**
 * Represents the reason codes of the CONNACK message.
 */
enum class MqttConnAckReasonCode(
    override val code: Byte,
) : MqttReasonCode {
    /**
     * TODO.
     */
    Success(MqttCommonReasonCode.Success.code),

    /**
     * TODO.
     */
    UnspecifiedError(MqttCommonReasonCode.UnspecifiedError.code),

    /**
     * TODO.
     */
    MalformedPacket(MqttCommonReasonCode.MalformedPacket.code),

    /**
     * TODO.
     */
    ProtocolError(MqttCommonReasonCode.ProtocolError.code),

    /**
     * TODO.
     */
    ImplementationSpecificError(MqttCommonReasonCode.ImplementationSpecificError.code),

    /**
     * TODO.
     */
    UnsupportedProtocolVersion(0x84.toByte()),

    /**
     * TODO.
     */
    ClientIdentifierNotValid(0x85.toByte()),

    /**
     * TODO.
     */
    BadUserNameOrPassword(0x86.toByte()),

    /**
     * TODO.
     */
    NotAuthorized(MqttCommonReasonCode.NotAuthorized.code),

    /**
     * TODO.
     */
    ServerUnavailable(0x88.toByte()),

    /**
     * TODO.
     */
    ServerBusy(MqttCommonReasonCode.ServerBusy.code),

    /**
     * TODO.
     */
    Banned(0x8A.toByte()),

    /**
     * TODO.
     */
    BadAuthenticationMethod(MqttCommonReasonCode.BadAuthenticationMethod.code),

    /**
     * TODO.
     */
    TopicNameInvalid(MqttCommonReasonCode.TopicNameInvalid.code),

    /**
     * TODO.
     */
    PacketTooLarge(MqttCommonReasonCode.PacketTooLarge.code),

    /**
     * TODO.
     */
    QuotaExceeded(MqttCommonReasonCode.QuotaExceeded.code),

    /**
     * TODO.
     */
    PayloadFormatInvalid(MqttCommonReasonCode.PayloadFormatInvalid.code),

    /**
     * TODO.
     */
    RetainNotSupported(MqttCommonReasonCode.RetainNotSupported.code),

    /**
     * TODO.
     */
    QoSNotSupported(MqttCommonReasonCode.QoSNotSupported.code),

    /**
     * TODO.
     */
    UseAnotherServer(MqttCommonReasonCode.UseAnotherServer.code),

    /**
     * TODO.
     */
    ServerMoved(MqttCommonReasonCode.ServerMoved.code),

    /**
     * TODO.
     */
    ConnectionRateExceeded(MqttCommonReasonCode.ConnectionRateExceeded.code),
    ;

    override val canBeSentByClient: Boolean get() = false

    /**
     * Companion object for [MqttConnAckReasonCode].
     */
    companion object {
        /**
         * Returns the [MqttConnAckReasonCode] from the given [code].
         */
        fun from(code: Byte): MqttConnAckReasonCode = MqttConnAckReasonCode.entries.first { it.code == code }
    }
}
