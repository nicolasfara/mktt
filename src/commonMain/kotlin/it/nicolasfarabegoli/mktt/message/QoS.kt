package it.nicolasfarabegoli.mktt.message

sealed interface QoS
data object AtMostOnce : QoS
data object AtLeastOnce : QoS
data object ExactlyOnce : QoS
