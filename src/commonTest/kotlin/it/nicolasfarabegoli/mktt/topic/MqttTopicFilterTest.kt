package it.nicolasfarabegoli.mktt.topic

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class MqttTopicFilterTest :
    FreeSpec({
        "The topic filter should be created correctly" {
            val filter = MqttTopicFilter.of("test/topic")
            filter.filterName shouldBe "test/topic"
            filter.levels shouldBe listOf("test", "topic")
            filter.containsWildcards shouldBe false
            filter.containsMultilevelWildcard shouldBe false
            filter.containsSingleLevelWildcard shouldBe false
        }
        "The topic filter should be created correctly with wildcards" {
            val filter = MqttTopicFilter.of("test/#")
            filter.filterName shouldBe "test/#"
            filter.levels shouldBe listOf("test", "#")
            filter.containsWildcards shouldBe true
            filter.containsMultilevelWildcard shouldBe true
            filter.containsSingleLevelWildcard shouldBe false
        }
        "The topic filter should be created correctly with single level wildcard" {
            val filter = MqttTopicFilter.of("test/+")
            filter.filterName shouldBe "test/+"
            filter.levels shouldBe listOf("test", "+")
            filter.containsWildcards shouldBe true
            filter.containsMultilevelWildcard shouldBe false
            filter.containsSingleLevelWildcard shouldBe true
        }
        "The topic filter should be created correctly with multiple wildcards" {
            val filter = MqttTopicFilter.of("test/+/topic/#")
            filter.filterName shouldBe "test/+/topic/#"
            filter.levels shouldBe listOf("test", "+", "topic", "#")
            filter.containsWildcards shouldBe true
            filter.containsMultilevelWildcard shouldBe true
            filter.containsSingleLevelWildcard shouldBe true
        }
        "The topic filter should match a topic" {
            val filter = MqttTopicFilter.of("test/topic")
            filter.matches(MqttTopic.of("test/topic")) shouldBe true
        }
        "The topic filter should match a topic with a single level wildcard" {
            val filter = MqttTopicFilter.of("test/+")
            filter.matches(MqttTopic.of("test/topic")) shouldBe true
        }
        "The topic filter should match a topic with a multilevel wildcard" {
            val filter = MqttTopicFilter.of("test/#")
            filter.matches(MqttTopic.of("test/topic")) shouldBe true
        }
        "The topic filter should match a topic with multiple wildcards" {
            val filter = MqttTopicFilter.of("test/+/topic/#")
            filter.matches(MqttTopic.of("test/1/topic/2")) shouldBe true
        }
        "The topic filter should not match a topic" {
            val filter = MqttTopicFilter.of("test/topic")
            filter.matches(MqttTopic.of("test/topic/1")) shouldBe false
        }
        "The topic filter should not match a topic with a single level wildcard" {
            val filter = MqttTopicFilter.of("test/+")
            filter.matches(MqttTopic.of("test/topic/1")) shouldBe false
        }
        "The topic filter should not match a topic with a multilevel wildcard" {
            val filter = MqttTopicFilter.of("test/#")
            filter.matches(MqttTopic.of("test/topic/1")) shouldBe false
        }
    })
