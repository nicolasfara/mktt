package it.nicolasfarabegoli.mktt.topic

import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class MqttTopicTest : FreeSpec({
    "A topic should not exceed 65535 characters" {
        shouldThrowUnit<IllegalArgumentException> {
            MqttTopic("a".repeat(65536))
        }
    }
    "A topic should not be empty" {
        shouldThrowUnit<IllegalArgumentException> {
            MqttTopic("")
        }
    }
    "A topic should be split into levels" {
        val topic = MqttTopic("a/b/c")
        val levels = topic.levels()
        levels.size shouldBe 3
        levels[0] shouldBe "a"
        levels[1] shouldBe "b"
        levels[2] shouldBe "c"
    }
})