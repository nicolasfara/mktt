package io.github.nicolasfara.mktt.client

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
internal actual fun isRemoteBrokerTestEnabled(): Boolean = getenv(RUN_REMOTE_BROKER_TEST_ENV)?.toKString() == "true"
