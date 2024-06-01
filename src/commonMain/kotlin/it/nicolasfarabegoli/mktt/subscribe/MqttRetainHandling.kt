package it.nicolasfarabegoli.mktt.subscribe

sealed interface MqttRetainHandling {
    val code: Byte
}

data object Send : MqttRetainHandling {
    override val code: Byte = 0x00
}

data object SendIfSubscriptionDoesNotExist : MqttRetainHandling {
    override val code: Byte = 0x01
}

data object DoNotSend : MqttRetainHandling {
    override val code: Byte = 0x02
}
