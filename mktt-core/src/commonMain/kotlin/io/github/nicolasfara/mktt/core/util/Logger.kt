package io.github.nicolasfara.mktt.core.util

object Logger {
    fun v(throwable: Throwable? = null, message: () -> String) {}
    fun d(throwable: Throwable? = null, message: () -> String) {}
    fun i(throwable: Throwable? = null, message: () -> String) {}
    fun w(throwable: Throwable? = null, message: () -> String) {}
    fun e(throwable: Throwable? = null, message: () -> String) {}
}
