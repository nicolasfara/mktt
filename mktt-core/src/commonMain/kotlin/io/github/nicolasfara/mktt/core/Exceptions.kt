package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.packet.Publish

open class MqttException internal constructor(message: String? = null, cause: Throwable? = null) :
    RuntimeException(message, cause)

/**
 * Indicates that a received packet could not be parsed.
 */
class MalformedPacketException(message: String? = null) : MqttException(message)

/**
 * Indicates that a server connection cannot be established or was disconnected.
 */
class ConnectionException(message: String? = null, cause: Throwable? = null) :
    MqttException(message = message, cause = cause)

/**
 * Indicates that a packet was not received within the expected time.
 */
class TimeoutException(message: String) : MqttException(message)

/**
 * Indicates that the topic alias value of a publish request exceed the "Topic Alias Maximum" sent by the server.
 */
class TopicAliasException(message: String?) : MqttException(message)

/**
 * Indicates that the handshake procedure for [io.github.nicolasfara.mktt.core.QoS.AT_LEAST_ONCE] or for [io.github.nicolasfara.mktt.core.QoS.EXACTLY_ONE] has failed for the specified
 * source packet. This exception is never raised when publishing [io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE] packets. The failed packets will
 * be retransmitted upon reconnection to the server.
 */
class HandshakeFailedException(message: String, val source: Publish) : MqttException(message)
