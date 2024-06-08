package it.nicolasfarabegoli.mktt.message

interface MqttReasonCode {
    val code: Byte
    val isError: Boolean get() = code >= 0x80
    val canBeSentByServer: Boolean get() = true
    val canBeSentByClient: Boolean get() = false
    val canBeSetByUser: Boolean get() = false
}