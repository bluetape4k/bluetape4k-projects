package io.bluetape4k.logging


import org.slf4j.Logger


inline fun Logger.traceMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isTraceEnabled) {
        withLoggingContext(mdcSupplier()) {
            trace(logMessageSafe(msg = msg))
        }
    }
}

inline fun Logger.traceMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isTraceEnabled) {
        withLoggingContext(mdcSupplier()) {
            trace(logMessageSafe(msg = msg), cause)
        }
    }
}

inline fun Logger.debugMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isDebugEnabled) {
        withLoggingContext(mdcSupplier()) {
            debug(logMessageSafe(msg = msg))
        }
    }
}

inline fun Logger.debugMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isDebugEnabled) {
        withLoggingContext(mdcSupplier()) {
            debug(logMessageSafe(msg = msg), cause)
        }
    }
}

inline fun Logger.infoMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isInfoEnabled) {
        withLoggingContext(mdcSupplier()) {
            info(logMessageSafe(msg = msg))
        }
    }
}

inline fun Logger.infoMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isInfoEnabled) {
        withLoggingContext(mdcSupplier()) {
            info(logMessageSafe(msg = msg), cause)
        }
    }
}

inline fun Logger.warnMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isWarnEnabled) {
        withLoggingContext(mdcSupplier()) {
            warn("🔥" + logMessageSafe(msg = msg))
        }
    }
}

inline fun Logger.warnMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isWarnEnabled) {
        withLoggingContext(mdcSupplier()) {
            warn("🔥" + logMessageSafe(msg = msg), cause)
        }
    }
}

inline fun Logger.errorMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isErrorEnabled) {
        withLoggingContext(mdcSupplier()) {
            error("🔥" + logMessageSafe(msg = msg))
        }
    }
}

inline fun Logger.errorMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isErrorEnabled) {
        withLoggingContext(mdcSupplier()) {
            error("🔥" + logMessageSafe(msg = msg), cause)
        }
    }
}
