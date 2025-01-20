package it.nicolasfarabegoli.mktt.facade

import it.nicolasfarabegoli.mktt.subscribe.MqttSubscription

external interface IClientSubscribeOptions {
    val qos: Number
    val nl: Boolean?
    val rap: Boolean?
    val rh: Number?
}

fun createSubscribeOption(
    qos: Number,
    nl: Boolean,
    rap: Boolean,
    rh: Number,
): IClientSubscribeOptions {
    return object : IClientSubscribeOptions {
        override val qos: Number = qos
        override val nl: Boolean? = nl
        override val rap: Boolean? = rap
        override val rh: Number? = rh
    }
}

external interface ISubscriptionMap {
    val resubscribe: Boolean?
}

fun MqttSubscription.toISubscriptionMap(): ISubscriptionMap {
    this.topicFilter
    val subMap = object : ISubscriptionMap {
        override val resubscribe: Boolean? = false
    }
    subMap.set(topicFilter.filterName, createSubscribeOption(qos.code, noLocal, retainAsPublished, retainHandling.code))
    return subMap
}

external interface ISubscriptionGrant {
    val topic: String
    val qos: Number
    val nl: Boolean?
    val rap: Boolean?
    val rh: Number?
}

fun ISubscriptionMap.get(topic: String): IClientSubscribeOptions? = asDynamic().get(topic)
fun ISubscriptionMap.set(topic: String, option: IClientSubscribeOptions) {
    asDynamic()[topic] = option
}