package io.github.nicolasfara.mktt.core

public data class SubscriptionOptions(
    public val qoS: io.github.nicolasfara.mktt.core.QoS = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE,
    public val isNoLocal: Boolean = false,
    public val retainAsPublished: Boolean = false,
    public val retainHandling: io.github.nicolasfara.mktt.core.RetainHandling = _root_ide_package_.io.github.nicolasfara.mktt.core.RetainHandling.SEND_ON_SUBSCRIBE,
) {

    public companion object {

        public val DEFAULT: io.github.nicolasfara.mktt.core.SubscriptionOptions =
            _root_ide_package_.io.github.nicolasfara.mktt.core.SubscriptionOptions()
    }
}
