package it.nicolasfarabegoli.mktt.topic

import kotlin.test.Test
import kotlin.test.assertEquals

class MqttTopicFilterTest {
    @Test
    fun `The topic filter should be created correctly`() {
        val filter = MqttTopicFilter.of("test/topic")
        assertEquals("test/topic", filter.filterName)
        assertEquals(listOf("test", "topic"), filter.levels)
        assertEquals(false, filter.containsWildcards)
        assertEquals(false, filter.containsMultilevelWildcard)
        assertEquals(false, filter.containsSingleLevelWildcard)
    }
    @Test
    fun `The topic filter should be created correctly with wildcards`() {
        val filter = MqttTopicFilter.of("test/#")
        assertEquals("test/#", filter.filterName)
        assertEquals(listOf("test", "#"), filter.levels)
        assertEquals(true, filter.containsWildcards)
        assertEquals(true, filter.containsMultilevelWildcard)
        assertEquals(false, filter.containsSingleLevelWildcard)
    }

    @Test
    fun `The topic filter should be created correctly with single level wildcard`(){
        val filter = MqttTopicFilter.of("test/+")
        assertEquals("test/+", filter.filterName)
        assertEquals(listOf("test", "+"), filter.levels)
        assertEquals(true, filter.containsWildcards)
        assertEquals(false, filter.containsMultilevelWildcard)
        assertEquals(true, filter.containsSingleLevelWildcard)
    }

    @Test
    fun `The topic filter should be created correctly with multiple wildcards`() {
        val filter = MqttTopicFilter.of("test/+/topic/#")
        assertEquals("test/+/topic/#", filter.filterName)
        assertEquals(listOf("test", "+", "topic", "#"), filter.levels)
        assertEquals(true, filter.containsWildcards)
        assertEquals(true, filter.containsMultilevelWildcard)
        assertEquals(true, filter.containsSingleLevelWildcard)
    }

    @Test
    fun `The topic filter should match a topic`() {
        val filter = MqttTopicFilter.of("test/topic")
        assertEquals(true, filter.matches(MqttTopic.of("test/topic")))
    }

    @Test
    fun `The topic filter should not match a topic with multiple wildcards`() {
        val filter = MqttTopicFilter.of("test/+/topic/#")
        assertEquals(false, filter.matches(MqttTopic.of("test/1/topic/2/3")))
    }

    @Test
    fun `The topic filter should match a topic with a single level wildcard`() {
        val filter = MqttTopicFilter.of("test/+")
        assertEquals(true, filter.matches(MqttTopic.of("test/topic")))
    }

    @Test
    fun `The topic filter should match a topic with a multilevel wildcard`() {
        val filter = MqttTopicFilter.of("test/#")
        assertEquals(true, filter.matches(MqttTopic.of("test/topic")))
    }

    @Test
    fun `The topic filter should match a topic with multiple wildcards`() {
        val filter = MqttTopicFilter.of("test/+/topic/#")
        assertEquals(true, filter.matches(MqttTopic.of("test/1/topic/2")))
    }

    @Test
    fun `The topic filter should not match a topic`() {
        val filter = MqttTopicFilter.of("test/topic")
        assertEquals(false, filter.matches(MqttTopic.of("test/topic/1")))
    }

    @Test
    fun `The topic filter should not match a topic with a single level wildcard`() {
        val filter = MqttTopicFilter.of("test/+")
        assertEquals(false, filter.matches(MqttTopic.of("test/topic/1")))
    }

    @Test
    fun `The topic filter should not match a topic with a multilevel wildcard`() {
        val filter = MqttTopicFilter.of("test/#")
        assertEquals(false, filter.matches(MqttTopic.of("test/topic/1")))
    }
}
