package io.github.nicolasfara.mktt.client

internal actual fun isRemoteBrokerTestEnabled(): Boolean = System.getenv(RUN_REMOTE_BROKER_TEST_ENV) == "true"
