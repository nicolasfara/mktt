package io.github.nicolasfara.mktt.client

internal const val RUN_REMOTE_BROKER_TEST_ENV = "RUN_REMOTE_BROKER_TEST"

internal expect fun isRemoteBrokerTestEnabled(): Boolean
