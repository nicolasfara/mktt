package it.nicolasfarabegoli.mktt.message

/**
 * Represents an MQTT message type.
 */
sealed interface MqttMessageType {
    /**
     * The byte code of the [MqttMessageType].
     */
    val code: Byte

    companion object {
        /**
         * Returns the [MqttMessageType] from the given [code].
         */
        fun values(): List<MqttMessageType> = listOf(
            Connect,
            ConnAck,
            Publish,
            PubAck,
            PubRec,
            PubRel,
            PubComp,
            Subscribe,
            SubAck,
            Unsubscribe,
            UnsubAck,
            PingReq,
            PingResp,
            Disconnect,
            Auth,
        )
    }

    /**
     * The CONNECT message type.
     */
    data object Connect : MqttMessageType {
        override val code: Byte = 0x01.toByte()
    }

    /**
     * The CONNACK message type.
     */
    data object ConnAck : MqttMessageType {
        override val code: Byte = 0x02.toByte()
    }

    /**
     * The PUBLISH message type.
     */
    data object Publish : MqttMessageType {
        override val code: Byte = 0x03.toByte()
    }

    /**
     * The PUBACK message type.
     */
    data object PubAck : MqttMessageType {
        override val code: Byte = 0x04.toByte()
    }

    /**
     * The PUBREC message type.
     */
    data object PubRec : MqttMessageType {
        override val code: Byte = 0x05.toByte()
    }

    /**
     * The PUBREL message type.
     */
    data object PubRel : MqttMessageType {
        override val code: Byte = 0x06.toByte()
    }

    /**
     * The PUBCOMP message type.
     */
    data object PubComp : MqttMessageType {
        override val code: Byte = 0x07.toByte()
    }

    /**
     * The SUBSCRIBE message type.
     */
    data object Subscribe : MqttMessageType {
        override val code: Byte = 0x08.toByte()
    }

    /**
     * The SUBACK message type.
     */
    data object SubAck : MqttMessageType {
        override val code: Byte = 0x09.toByte()
    }

    /**
     * The UNSUBSCRIBE message type.
     */
    data object Unsubscribe : MqttMessageType {
        override val code: Byte = 0x0A.toByte()
    }

    /**
     * The UNSUBACK message type.
     */
    data object UnsubAck : MqttMessageType {
        override val code: Byte = 0x0B.toByte()
    }

    /**
     * The PINGREQ message type.
     */
    data object PingReq : MqttMessageType {
        override val code: Byte = 0x0C.toByte()
    }

    /**
     * The PINGRESP message type.
     */
    data object PingResp : MqttMessageType {
        override val code: Byte = 0x0D.toByte()
    }

    /**
     * The DISCONNECT message type.
     */
    data object Disconnect : MqttMessageType {
        override val code: Byte = 0x0E.toByte()
    }

    /**
     * The AUTH message type.
     */
    data object Auth : MqttMessageType {
        override val code: Byte = 0x0F.toByte()
    }
}
