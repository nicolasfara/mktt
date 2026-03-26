package io.github.nicolasfara.mktt.client

internal object LocalBrokerSupport {
    private const val RUN_INTEGRATION_TESTS_ENV = "MKTT_RUN_INTEGRATION_TESTS"

    fun startBrokerOrSkip(): MosquittoContainer? {
        if (!shouldRunIntegrationTests()) {
            return null
        }
        return runCatching {
            MosquittoContainer().also { it.start() }
        }.getOrNull()
    }

    private fun shouldRunIntegrationTests(): Boolean {
        val envValue = System.getenv(RUN_INTEGRATION_TESTS_ENV)?.lowercase()
        return when (envValue) {
            "1", "true", "yes" -> true
            "0", "false", "no" -> false
            // In CI run integration tests only when explicitly enabled.
            else -> System.getenv("CI") != "true"
        }
    }
}
