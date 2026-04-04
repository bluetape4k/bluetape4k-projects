package io.bluetape4k.logging

import org.slf4j.Logger

/**
 * TRACE 레벨 로그를 MDC와 함께 기록합니다.
 *
 * ## 동작/계약
 * - TRACE 비활성화 시 `mdcSupplier`와 `msg`를 평가하지 않습니다.
 * - 활성화 시 `withLoggingContext(mdcSupplier())` 범위에서 메시지를 기록합니다.
 *
 * ```kotlin
 * logger.traceMdc({ mapOf("traceId" to "t-1") }) { "TRACE 처리 시작" }
 * ```
 *
 * @param mdcSupplier MDC 키-값 맵을 생성하는 함수입니다.
 * @param msg 로그 메시지 람다입니다.
 */
inline fun Logger.traceMdc(
    mdcSupplier: () -> Map<String, Any?>,
    msg: () -> Any?,
) {
    if (isTraceEnabled) {
        withLoggingContext(mdcSupplier()) {
            trace(logMessageSafe(msg = msg))
        }
    }
}

/**
 * TRACE 레벨에서 MDC/예외/메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.traceMdc({ mapOf("traceId" to "t-1") }, exception) { "TRACE 예외 발생" }
 * ```
 */
inline fun Logger.traceMdc(
    mdcSupplier: () -> Map<String, Any?>,
    cause: Throwable?,
    msg: () -> Any?,
) {
    if (isTraceEnabled) {
        withLoggingContext(mdcSupplier()) {
            trace(logMessageSafe(msg = msg), cause)
        }
    }
}

/**
 * DEBUG 레벨 로그를 MDC와 함께 기록합니다.
 *
 * ```kotlin
 * logger.debugMdc({ mapOf("userId" to "u-42") }) { "사용자 조회 완료" }
 * ```
 */
inline fun Logger.debugMdc(
    mdcSupplier: () -> Map<String, Any?>,
    msg: () -> Any?,
) {
    if (isDebugEnabled) {
        withLoggingContext(mdcSupplier()) {
            debug(logMessageSafe(msg = msg))
        }
    }
}

/**
 * DEBUG 레벨에서 MDC/예외/메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.debugMdc({ mapOf("userId" to "u-42") }, exception) { "DEBUG 예외 발생" }
 * ```
 */
inline fun Logger.debugMdc(
    mdcSupplier: () -> Map<String, Any?>,
    cause: Throwable?,
    msg: () -> Any?,
) {
    if (isDebugEnabled) {
        withLoggingContext(mdcSupplier()) {
            debug(logMessageSafe(msg = msg), cause)
        }
    }
}

/**
 * INFO 레벨 로그를 MDC와 함께 기록합니다.
 *
 * ```kotlin
 * logger.infoMdc({ mapOf("requestId" to "req-99") }) { "요청 처리 완료" }
 * ```
 */
inline fun Logger.infoMdc(
    mdcSupplier: () -> Map<String, Any?>,
    msg: () -> Any?,
) {
    if (isInfoEnabled) {
        withLoggingContext(mdcSupplier()) {
            info(logMessageSafe(msg = msg))
        }
    }
}

/**
 * INFO 레벨에서 MDC/예외/메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.infoMdc({ mapOf("requestId" to "req-99") }, exception) { "INFO 예외 발생" }
 * ```
 */
inline fun Logger.infoMdc(
    mdcSupplier: () -> Map<String, Any?>,
    cause: Throwable?,
    msg: () -> Any?,
) {
    if (isInfoEnabled) {
        withLoggingContext(mdcSupplier()) {
            info(logMessageSafe(msg = msg), cause)
        }
    }
}

/**
 * WARN 레벨 로그를 MDC와 함께 기록하며 메시지에 [WARN_ERROR_PREFIX]를 붙입니다.
 *
 * ```kotlin
 * logger.warnMdc({ mapOf("service" to "payment") }) { "결제 서비스 지연" }
 * ```
 */
inline fun Logger.warnMdc(
    mdcSupplier: () -> Map<String, Any?>,
    msg: () -> Any?,
) {
    if (isWarnEnabled) {
        withLoggingContext(mdcSupplier()) {
            warn(WARN_ERROR_PREFIX + logMessageSafe(msg = msg))
        }
    }
}

/**
 * WARN 레벨에서 MDC/예외/[WARN_ERROR_PREFIX] 접두 메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.warnMdc({ mapOf("service" to "payment") }, exception) { "결제 예외 발생" }
 * ```
 */
inline fun Logger.warnMdc(
    mdcSupplier: () -> Map<String, Any?>,
    cause: Throwable?,
    msg: () -> Any?,
) {
    if (isWarnEnabled) {
        withLoggingContext(mdcSupplier()) {
            warn(WARN_ERROR_PREFIX + logMessageSafe(msg = msg), cause)
        }
    }
}

/**
 * ERROR 레벨 로그를 MDC와 함께 기록하며 메시지에 [WARN_ERROR_PREFIX]를 붙입니다.
 *
 * ```kotlin
 * logger.errorMdc({ mapOf("errorCode" to "E500") }) { "내부 서버 오류" }
 * ```
 */
inline fun Logger.errorMdc(
    mdcSupplier: () -> Map<String, Any?>,
    msg: () -> Any?,
) {
    if (isErrorEnabled) {
        withLoggingContext(mdcSupplier()) {
            error(WARN_ERROR_PREFIX + logMessageSafe(msg = msg))
        }
    }
}

/**
 * ERROR 레벨에서 MDC/예외/[WARN_ERROR_PREFIX] 접두 메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.errorMdc({ mapOf("errorCode" to "E500") }, exception) { "치명적 오류" }
 * ```
 */
inline fun Logger.errorMdc(
    mdcSupplier: () -> Map<String, Any?>,
    cause: Throwable?,
    msg: () -> Any?,
) {
    if (isErrorEnabled) {
        withLoggingContext(mdcSupplier()) {
            error(WARN_ERROR_PREFIX + logMessageSafe(msg = msg), cause)
        }
    }
}
