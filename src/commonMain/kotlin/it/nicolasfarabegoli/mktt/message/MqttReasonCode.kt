package it.nicolasfarabegoli.mktt.message

/**
 * Represents a generic MQTT reason code with a specific [code].
 *
 * The [isError] property indicates if the reason code is an error.
 * The [canBeSentByServer] property indicates if the reason code can be sent by the server.
 * The [canBeSentByClient] property indicates if the reason code can be sent by the client.
 * The [canBeSetByUser] property indicates if the reason code can be set by the user.
 */
interface MqttReasonCode {
    val code: Byte
    val isError: Boolean get() = code >= ERROR_CODE
    val canBeSentByServer: Boolean get() = true
    val canBeSentByClient: Boolean get() = false
    val canBeSetByUser: Boolean get() = false

    companion object {
        private const val ERROR_CODE = 0x80.toByte()
    }
}
