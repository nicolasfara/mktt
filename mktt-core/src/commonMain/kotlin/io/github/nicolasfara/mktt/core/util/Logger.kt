package io.github.nicolasfara.mktt.core.util

public object Logger {
    public fun v(throwable: Throwable? = null, message: () -> String) {}
    public fun d(throwable: Throwable? = null, message: () -> String) {}
    public fun i(throwable: Throwable? = null, message: () -> String) {}
    public fun w(throwable: Throwable? = null, message: () -> String) {}
    public fun e(throwable: Throwable? = null, message: () -> String) {}
}
