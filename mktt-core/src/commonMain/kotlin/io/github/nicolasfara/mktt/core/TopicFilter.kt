package io.github.nicolasfara.mktt.core

import io.github.nicolasfara.mktt.core.util.MqttDslMarker

public data class TopicFilter(
    public val filter: io.github.nicolasfara.mktt.core.Topic,
    public val subscriptionOptions: io.github.nicolasfara.mktt.core.SubscriptionOptions = _root_ide_package_.io.github.nicolasfara.mktt.core.SubscriptionOptions.Companion.DEFAULT,
) {
    init {
        require(filter.isNotBlank()) { "Empty topics are not allowed in topic filters" }
    }
}

public fun List<io.github.nicolasfara.mktt.core.TopicFilter>.hasSharedTopic(): Boolean = any { it.filter.isShared() }

public fun List<io.github.nicolasfara.mktt.core.TopicFilter>.hasWildcard(): Boolean = any {
    it.filter.containsWildcard()
}

public fun buildFilterList(
    init: io.github.nicolasfara.mktt.core.TopicFilterBuilder.() -> Unit,
): List<io.github.nicolasfara.mktt.core.TopicFilter> =
    _root_ide_package_.io.github.nicolasfara.mktt.core.TopicFilterBuilder().also(init).build()

@io.github.nicolasfara.mktt.core.util.MqttDslMarker
public class TopicFilterBuilder {

    private val filters = mutableListOf<io.github.nicolasfara.mktt.core.TopicFilter>()

    public fun add(
        topic: String,
        qoS: io.github.nicolasfara.mktt.core.QoS = _root_ide_package_.io.github.nicolasfara.mktt.core.QoS.AT_MOST_ONCE,
        isNoLocal: Boolean = false,
        retainAsPublished: Boolean = false,
        retainHandling: io.github.nicolasfara.mktt.core.RetainHandling = _root_ide_package_.io.github.nicolasfara.mktt.core.RetainHandling.SEND_ON_SUBSCRIBE,
    ) {
        filters.add(
            _root_ide_package_.io.github.nicolasfara.mktt.core.TopicFilter(
                _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(
                    topic,
                ),
                _root_ide_package_.io.github.nicolasfara.mktt.core.SubscriptionOptions(
                    qoS,
                    isNoLocal,
                    retainAsPublished,
                    retainHandling,
                ),
            ),
        )
    }

    public operator fun String.unaryPlus() {
        filters.add(
            _root_ide_package_.io.github.nicolasfara.mktt.core.TopicFilter(
                _root_ide_package_.io.github.nicolasfara.mktt.core.Topic(
                    this,
                ),
            ),
        )
    }

    public fun build(): List<io.github.nicolasfara.mktt.core.TopicFilter> = filters.toList()
}
