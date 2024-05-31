package it.nicolasfarabegoli.mktt.message

sealed interface MqttMessageType {
    val code: Byte
}

data object Connect : MqttMessageType {
    override val code: Byte = 0x01.toByte()
}

data object ConnAck : MqttMessageType {
    override val code: Byte = 0x02.toByte()
}

data object Publish : MqttMessageType {
    override val code: Byte = 0x03.toByte()
}

data object PubAck : MqttMessageType {
    override val code: Byte = 0x04.toByte()
}

data object PubRec : MqttMessageType {
    override val code: Byte = 0x05.toByte()
}

data object PubRel : MqttMessageType {
    override val code: Byte = 0x06.toByte()
}

data object PubComp : MqttMessageType {
    override val code: Byte = 0x07.toByte()
}

data object Subscribe : MqttMessageType {
    override val code: Byte = 0x08.toByte()
}

data object SubAck : MqttMessageType {
    override val code: Byte = 0x09.toByte()
}

data object Unsubscribe : MqttMessageType {
    override val code: Byte = 0x0A.toByte()
}

data object UnsubAck : MqttMessageType {
    override val code: Byte = 0x0B.toByte()
}

data object PingReq : MqttMessageType {
    override val code: Byte = 0x0C.toByte()
}

data object PingResp : MqttMessageType {
    override val code: Byte = 0x0D.toByte()
}

data object Disconnect : MqttMessageType {
    override val code: Byte = 0x0E.toByte()
}

data object Auth : MqttMessageType {
    override val code: Byte = 0x0F.toByte()
}
