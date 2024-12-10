package it.nicolasfarabegoli.mktt.message

/**
 * Represents the type of MQTT message and its [code].
 */
enum class MqttMessageType(val code: Byte) {
    /**
     * TODO.
     */
    CONNECT(0x01.toByte()),

    /**
     * TODO.
     */
    CONNACK(0x02.toByte()),

    /**
     * TODO.
     */
    PUBLISH(0x03.toByte()),

    /**
     * TODO.
     */
    PUBACK(0x04.toByte()),

    /**
     * TODO.
     */
    PUBREC(0x05.toByte()),

    /**
     * TODO.
     */
    PUBREL(0x06.toByte()),

    /**
     * TODO.
     */
    PUBCOMP(0x07.toByte()),

    /**
     * TODO.
     */
    SUBSCRIBE(0x08.toByte()),

    /**
     * TODO.
     */
    SUBACK(0x09.toByte()),

    /**
     * TODO.
     */
    UNSUBSCRIBE(0x0A.toByte()),

    /**
     * TODO.
     */
    UNSUBACK(0x0B.toByte()),

    /**
     * TODO.
     */
    PINGREQ(0x0C.toByte()),

    /**
     * TODO.
     */
    PINGRESP(0x0D.toByte()),

    /**
     * TODO.
     */
    DISCONNECT(0x0E.toByte()),

    /**
     * TODO.
     */
    AUTH(0x0F.toByte()),
}
