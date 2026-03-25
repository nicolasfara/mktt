package io.github.nicolasfara.mktt.core

data class SubscriptionOptions(
    val qoS: QoS = QoS.AT_MOST_ONCE,
    val isNoLocal: Boolean = false,
    val retainAsPublished: Boolean = false,
    val retainHandling: RetainHandling = RetainHandling.SEND_ON_SUBSCRIBE,
) {
    companion object {
        val DEFAULT: SubscriptionOptions =
            SubscriptionOptions()
    }
}
