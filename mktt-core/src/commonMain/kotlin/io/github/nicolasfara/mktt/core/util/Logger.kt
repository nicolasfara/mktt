package io.github.nicolasfara.mktt.core.util

/**
 * Minimal logger facade used across common code.
 */
object Logger {
    /** Logs a verbose-level message. */
    fun v(throwable: Throwable? = null, message: () -> String) = consoleLog(throwable, message)

    /** Logs a debug-level message. */
    fun d(throwable: Throwable? = null, message: () -> String) = consoleLog(throwable, message)

    /** Logs an info-level message. */
    fun i(throwable: Throwable? = null, message: () -> String) = consoleLog(throwable, message)

    /** Logs a warning-level message. */
    fun w(throwable: Throwable? = null, message: () -> String) = consoleLog(throwable, message)

    /** Logs an error-level message. */
    fun e(throwable: Throwable? = null, message: () -> String) = consoleLog(throwable, message)

    private fun consoleLog(throwable: Throwable?, message: () -> String) {
        // Keep this logger as a no-op while still consuming parameters for static analysis.
        throwable?.let(::println)
        throwable?.let {
            println(message())
            throwable.printStackTrace()
        }
    }
}
