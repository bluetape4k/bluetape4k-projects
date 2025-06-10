package io.bluetape4k.logging

import org.slf4j.Logger
import org.slf4j.Marker

const val LOG_FALLBACK_MSG = "Fail to generate log message."

inline fun logMessageSafe(msg: () -> Any?, fallbackMessage: String = LOG_FALLBACK_MSG): String {
    return try {
        msg().toString()
    } catch (e: Exception) {
        "$fallbackMessage: $e"
    }
}

inline fun Logger.trace(msg: () -> Any?) {
    if (isTraceEnabled) {
        trace(logMessageSafe(msg))
    }
}

inline fun Logger.trace(cause: Throwable?, msg: () -> Any?) {
    if (isTraceEnabled) {
        trace(logMessageSafe(msg), cause)
    }
}


inline fun Logger.trace(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isTraceEnabled) {
        trace(marker, logMessageSafe(msg), cause)
    }
}


inline fun Logger.debug(msg: () -> Any?) {
    if (isDebugEnabled) {
        debug(logMessageSafe(msg))
    }
}

inline fun Logger.debug(cause: Throwable?, msg: () -> Any?) {
    if (isDebugEnabled) {
        debug(logMessageSafe(msg), cause)
    }
}

inline fun Logger.debug(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isDebugEnabled) {
        debug(marker, logMessageSafe(msg), cause)
    }
}

inline fun Logger.info(msg: () -> Any?) {
    if (isInfoEnabled) {
        info(logMessageSafe(msg))
    }
}

inline fun Logger.info(cause: Throwable?, msg: () -> Any?) {
    if (isInfoEnabled) {
        info(logMessageSafe(msg), cause)
    }
}

inline fun Logger.info(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isInfoEnabled) {
        info(marker, logMessageSafe(msg), cause)
    }
}

inline fun Logger.warn(msg: () -> Any?) {
    if (isWarnEnabled) {
        warn("🔥" + logMessageSafe(msg))
    }
}

inline fun Logger.warn(cause: Throwable?, msg: () -> Any?) {
    if (isWarnEnabled) {
        warn("🔥" + logMessageSafe(msg), cause)
    }
}

inline fun Logger.warn(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isWarnEnabled) {
        warn(marker, "🔥" + logMessageSafe(msg), cause)
    }
}

inline fun Logger.error(msg: () -> Any?) {
    if (isErrorEnabled) {
        error("🔥" + logMessageSafe(msg))
    }
}

inline fun Logger.error(cause: Throwable?, msg: () -> Any?) {
    if (isErrorEnabled) {
        error("🔥" + logMessageSafe(msg), cause)
    }
}

inline fun Logger.error(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isErrorEnabled) {
        error(marker, "🔥" + logMessageSafe(msg), cause)
    }
}
