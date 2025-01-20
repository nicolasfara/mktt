package it.nicolasfarabegoli.mktt.errors

sealed class MqttClientError : Throwable()

class InvalidBrokerError(override val message: String) : MqttClientError()
class GenericClientError(override val message: String?) : MqttClientError()
class ClientAlreadyConnectedError : MqttClientError()
class ClientNotConnectedError : MqttClientError()