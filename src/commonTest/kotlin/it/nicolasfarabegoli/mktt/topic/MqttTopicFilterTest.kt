package it.nicolasfarabegoli.mktt.topic

import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class MqttTopicFilterTest : FreeSpec({
    "A topic filter should not have empty filter" {
        shouldThrowUnit<IllegalArgumentException> {
            MqttTopicFilter("")
        }
    }
    "A topic filter should not exceed 65535 characters" {
        shouldThrowUnit<IllegalArgumentException> {
            MqttTopicFilter("a".repeat(65536))
        }
    }
    "A topic filter by default should be the multi-level wildcard" {
        val filter = MqttTopicFilter()
        filter.filter shouldBe "#"
    }
    "A topic filter should match a topic with the same levels" {
        val filter = MqttTopicFilter("a/b/c")
        val topic = MqttTopic("a/b/c")
        filter.matches(topic) shouldBe true
    }
    "A topic filter should match a topic with the same levels and multi-level wildcard" {
        val filter = MqttTopicFilter("a/#")
        val topic = MqttTopic("a/b/c")
        filter.matches(topic) shouldBe true
    }
    "A topic filter should match a topic with the same levels and single-level wildcard" {
        val filter = MqttTopicFilter("a/+/c")
        val topic = MqttTopic("a/b/c")
        filter.matches(topic) shouldBe true
    }
    "A topic filter should not match a topic with different levels" {
        val filter = MqttTopicFilter("a/b/c")
        val topic = MqttTopic("a/b")
        filter.matches(topic) shouldBe false
    }
    "A topic filter should not match a topic with the same levels and single-level wildcard" {
        val filter = MqttTopicFilter("a/+/c")
        val topic = MqttTopic("a/b")
        filter.matches(topic) shouldBe false
    }
    "A topic filter should match a topic with no levels and multi-level wildcard" {
        val filter = MqttTopicFilter("a/#")
        val topic = MqttTopic("a")
        filter.matches(topic) shouldBe true
    }
    "A topic filter should not match a topic when a single-level wildcard is at the beginning" {
        val filter = MqttTopicFilter("+/b/c")
        val topic = MqttTopic("a/b/c")
        filter.matches(topic) shouldBe true
    }
    "A topic filter should match a topic when a single-level wildcard is at the end" {
        val filter = MqttTopicFilter("a/b/+")
        val topic = MqttTopic("a/b/c")
        filter.matches(topic) shouldBe true
    }
    "A topic filter should match valid topics" {
        val filters = listOf(
            "home/+/temperature",
            "home/#",
            "+/temperature",
            "home/livingroom",
            "#",
            "home/+/+/temperature",
            "home/+/+/#"
        )
        val topics = listOf(
            "home/livingroom/temperature",
            "home/kitchen/temperature",
            "home/temperature",
            "home/livingroom",
            "home",
            "home/bedroom/bed/temperature",
            "home/bedroom/floor1/temperature"
        )
        filters.zip(topics).forEach { (filter, topic) ->
            MqttTopicFilter(filter).matches(MqttTopic(topic)) shouldBe true
        }
    }
})