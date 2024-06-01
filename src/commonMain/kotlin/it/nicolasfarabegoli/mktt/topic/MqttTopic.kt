package it.nicolasfarabegoli.mktt.topic

data class MqttTopic(val value: String) {
    init {
        require(value.isNotEmpty()) { "Topic cannot be empty" }
        require(value.length <= 65535) { "Topic length cannot exceed 65535 characters" }
    }

    fun levels(): List<String> = value.split(SEPARATOR)

    companion object {
        const val SEPARATOR = "/"
        fun String.toMqttTopic(): MqttTopic = MqttTopic(this)
    }
}