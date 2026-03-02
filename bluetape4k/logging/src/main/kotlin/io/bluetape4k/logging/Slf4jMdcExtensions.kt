package io.bluetape4k.logging

import org.slf4j.Logger

/**
 * TRACE 레벨 로그를 MDC와 함께 기록합니다.
 *
 * ## 동작/계약
 * - TRACE 비활성화 시 `mdcSupplier`와 `msg`를 평가하지 않습니다.
 * - 활성화 시 `withLoggingContext(mdcSupplier())` 범위에서 메시지를 기록합니다.
 *
 * @param mdcSupplier MDC 키-값 맵을 생성하는 함수입니다.
 * @param msg 로그 메시지 람다입니다.
 */
inline fun Logger.traceMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isTraceEnabled) {
        withLoggingContext(mdcSupplier()) {
            trace(logMessageSafe(msg = msg))
        }
    }
}

/** TRACE 레벨에서 MDC/예외/메시지를 함께 기록합니다. */
inline fun Logger.traceMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isTraceEnabled) {
        withLoggingContext(mdcSupplier()) {
            trace(logMessageSafe(msg = msg), cause)
        }
    }
}

/** DEBUG 레벨 로그를 MDC와 함께 기록합니다. */
inline fun Logger.debugMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isDebugEnabled) {
        withLoggingContext(mdcSupplier()) {
            debug(logMessageSafe(msg = msg))
        }
    }
}

/** DEBUG 레벨에서 MDC/예외/메시지를 함께 기록합니다. */
inline fun Logger.debugMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isDebugEnabled) {
        withLoggingContext(mdcSupplier()) {
            debug(logMessageSafe(msg = msg), cause)
        }
    }
}

/** INFO 레벨 로그를 MDC와 함께 기록합니다. */
inline fun Logger.infoMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isInfoEnabled) {
        withLoggingContext(mdcSupplier()) {
            info(logMessageSafe(msg = msg))
        }
    }
}

/** INFO 레벨에서 MDC/예외/메시지를 함께 기록합니다. */
inline fun Logger.infoMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isInfoEnabled) {
        withLoggingContext(mdcSupplier()) {
            info(logMessageSafe(msg = msg), cause)
        }
    }
}

/** WARN 레벨 로그를 MDC와 함께 기록하며 메시지에 `🔥`를 붙입니다. */
inline fun Logger.warnMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isWarnEnabled) {
        withLoggingContext(mdcSupplier()) {
            warn("🔥" + logMessageSafe(msg = msg))
        }
    }
}

/** WARN 레벨에서 MDC/예외/`🔥` 접두 메시지를 함께 기록합니다. */
inline fun Logger.warnMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isWarnEnabled) {
        withLoggingContext(mdcSupplier()) {
            warn("🔥" + logMessageSafe(msg = msg), cause)
        }
    }
}

/** ERROR 레벨 로그를 MDC와 함께 기록하며 메시지에 `🔥`를 붙입니다. */
inline fun Logger.errorMdc(mdcSupplier: () -> Map<String, Any?>, msg: () -> Any?) {
    if (isErrorEnabled) {
        withLoggingContext(mdcSupplier()) {
            error("🔥" + logMessageSafe(msg = msg))
        }
    }
}

/** ERROR 레벨에서 MDC/예외/`🔥` 접두 메시지를 함께 기록합니다. */
inline fun Logger.errorMdc(mdcSupplier: () -> Map<String, Any?>, cause: Throwable?, msg: () -> Any?) {
    if (isErrorEnabled) {
        withLoggingContext(mdcSupplier()) {
            error("🔥" + logMessageSafe(msg = msg), cause)
        }
    }
}
