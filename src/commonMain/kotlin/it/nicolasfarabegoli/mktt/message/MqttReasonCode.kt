package it.nicolasfarabegoli.mktt.message

/**
 * Represents a generic MQTT reason code with a specific [code].
 *
 * The [isError] property indicates if the reason code is an error.
 * The [canBeSentByServer] property indicates if the reason code can be sent by the server.
 * The [canBeSentByClient] property indicates if the reason code can be sent by the client.
 * The [canBeSetByUser] property indicates if the reason code can be set by the user.
 * The [userCodes] property contains the list of reason codes that can be set by the user.
 */
interface MqttReasonCode {
    val code: Byte
    val isError: Boolean get() = code >= ERROR_CODE
    val canBeSentByServer: Boolean get() = true
    val canBeSentByClient: Boolean get() = false
    val canBeSetByUser: Boolean get() = false
    val userCodes: List<Byte> get() =
        listOf(
            MqttCommonReasonCode.Success.code,
            MqttCommonReasonCode.UnspecifiedError.code,
            MqttCommonReasonCode.ImplementationSpecificError.code,
            MqttCommonReasonCode.NotAuthorized.code,
            MqttCommonReasonCode.TopicNameInvalid.code,
            MqttCommonReasonCode.QuotaExceeded.code,
            MqttCommonReasonCode.PayloadFormatInvalid.code,
        )

    /**
     * Companion object for [MqttReasonCode].
     */
    companion object {
        private const val ERROR_CODE = 0x80.toByte()
    }
}
