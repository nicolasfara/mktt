package io.github.nicolasfara.mktt.core

/**
 * MQTT subscription options associated with a topic filter.
 *
 * @property qoS requested quality of service for matching messages.
 * @property isNoLocal whether publications from this client are excluded.
 * @property retainAsPublished whether the RETAIN flag is forwarded as published.
 * @property retainHandling strategy for handling retained messages.
 */
data class SubscriptionOptions(
    val qoS: QoS = QoS.AT_MOST_ONCE,
    val isNoLocal: Boolean = false,
    val retainAsPublished: Boolean = false,
    val retainHandling: RetainHandling = RetainHandling.SEND_ON_SUBSCRIBE,
) {
    /** Holds default subscription-options values. */
    companion object {
        /** Default MQTT subscription options. */
        val DEFAULT: SubscriptionOptions =
            SubscriptionOptions()
    }
}
