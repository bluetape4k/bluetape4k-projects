package io.bluetape4k.logging

import org.slf4j.Logger
import org.slf4j.Marker

/**
 * 메시지 람다 실패 시 사용할 기본 fallback 문구입니다.
 */
const val LOG_FALLBACK_MSG = "Fail to generate log message."

/**
 * WARN/ERROR 레벨 로그 메시지 앞에 붙이는 접두어입니다.
 */
const val WARN_ERROR_PREFIX = "🔥"

/**
 * 로그 메시지 람다를 안전하게 문자열로 변환합니다.
 *
 * ## 동작/계약
 * - `msg()` 평가 중 예외가 나면 예외를 다시 던지지 않고 fallback 문자열을 반환합니다.
 * - `msg()`가 `null`을 반환하면 문자열 `"null"`을 반환합니다.
 * - 정상 경로에서는 `msg()?.toString()` 결과를 반환합니다.
 * - 예외 삼킴 동작이므로 메시지 생성 실패가 로깅 호출을 중단시키지 않습니다.
 *
 * ```kotlin
 * val text = logMessageSafe { "ok" }
 * // text == "ok"
 * ```
 *
 * @param fallbackMessage 메시지 생성 실패 시 사용할 문구입니다.
 * @param msg 메시지를 계산하는 람다입니다.
 */
inline fun logMessageSafe(fallbackMessage: String = LOG_FALLBACK_MSG, msg: () -> Any?): String {
    return try {
        msg()?.toString() ?: "null"
    } catch (e: Throwable) {
        "$fallbackMessage: $e"
    }
}

/**
 * TRACE 레벨이 활성화된 경우에만 메시지 람다를 평가해 기록합니다.
 *
 * ```kotlin
 * logger.trace { "요청 처리 시작" }
 * ```
 */
inline fun Logger.trace(msg: () -> Any?) {
    if (isTraceEnabled) {
        trace(logMessageSafe(msg = msg))
    }
}

/**
 * TRACE 레벨이 활성화된 경우에만 예외와 메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.trace(exception) { "예외 발생" }
 * ```
 */
inline fun Logger.trace(cause: Throwable?, msg: () -> Any?) {
    if (isTraceEnabled) {
        trace(logMessageSafe(msg = msg), cause)
    }
}

/**
 * TRACE 레벨이 활성화된 경우 marker/예외/메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.trace(marker, exception) { "마커 포함 TRACE 로그" }
 * ```
 */
inline fun Logger.trace(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isTraceEnabled) {
        trace(marker, logMessageSafe(msg = msg), cause)
    }
}

/**
 * DEBUG 레벨이 활성화된 경우에만 메시지 람다를 평가해 기록합니다.
 *
 * ```kotlin
 * logger.debug { "쿼리 실행: $sql" }
 * ```
 */
inline fun Logger.debug(msg: () -> Any?) {
    if (isDebugEnabled) {
        debug(logMessageSafe(msg = msg))
    }
}

/**
 * DEBUG 레벨이 활성화된 경우에만 예외와 메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.debug(exception) { "처리 중 예외" }
 * ```
 */
inline fun Logger.debug(cause: Throwable?, msg: () -> Any?) {
    if (isDebugEnabled) {
        debug(logMessageSafe(msg = msg), cause)
    }
}

/**
 * DEBUG 레벨이 활성화된 경우 marker/예외/메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.debug(marker, exception) { "마커 포함 DEBUG 로그" }
 * ```
 */
inline fun Logger.debug(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isDebugEnabled) {
        debug(marker, logMessageSafe(msg = msg), cause)
    }
}

/**
 * INFO 레벨이 활성화된 경우에만 메시지 람다를 평가해 기록합니다.
 *
 * ```kotlin
 * logger.info { "서버 시작 완료" }
 * ```
 */
inline fun Logger.info(msg: () -> Any?) {
    if (isInfoEnabled) {
        info(logMessageSafe(msg = msg))
    }
}

/**
 * INFO 레벨이 활성화된 경우에만 예외와 메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.info(exception) { "요청 처리 완료(예외 포함)" }
 * ```
 */
inline fun Logger.info(cause: Throwable?, msg: () -> Any?) {
    if (isInfoEnabled) {
        info(logMessageSafe(msg = msg), cause)
    }
}

/**
 * INFO 레벨이 활성화된 경우 marker/예외/메시지를 함께 기록합니다.
 *
 * ```kotlin
 * logger.info(marker, exception) { "마커 포함 INFO 로그" }
 * ```
 */
inline fun Logger.info(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isInfoEnabled) {
        info(marker, logMessageSafe(msg = msg), cause)
    }
}

/**
 * WARN 레벨이 활성화된 경우 메시지 앞에 [WARN_ERROR_PREFIX] 접두어를 붙여 기록합니다.
 *
 * ```kotlin
 * logger.warn { "재시도 한도 초과" }
 * ```
 */
inline fun Logger.warn(msg: () -> Any?) {
    if (isWarnEnabled) {
        warn(WARN_ERROR_PREFIX + logMessageSafe(msg = msg))
    }
}

/**
 * WARN 레벨이 활성화된 경우 예외와 [WARN_ERROR_PREFIX] 접두 메시지를 기록합니다.
 *
 * ```kotlin
 * logger.warn(exception) { "외부 서비스 연결 실패" }
 * ```
 */
inline fun Logger.warn(cause: Throwable?, msg: () -> Any?) {
    if (isWarnEnabled) {
        warn(WARN_ERROR_PREFIX + logMessageSafe(msg = msg), cause)
    }
}

/**
 * WARN 레벨이 활성화된 경우 marker/예외/[WARN_ERROR_PREFIX] 접두 메시지를 기록합니다.
 *
 * ```kotlin
 * logger.warn(marker, exception) { "마커 포함 WARN 로그" }
 * ```
 */
inline fun Logger.warn(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isWarnEnabled) {
        warn(marker, WARN_ERROR_PREFIX + logMessageSafe(msg = msg), cause)
    }
}

/**
 * ERROR 레벨이 활성화된 경우 메시지 앞에 [WARN_ERROR_PREFIX] 접두어를 붙여 기록합니다.
 *
 * ```kotlin
 * logger.error { "치명적 오류 발생" }
 * ```
 */
inline fun Logger.error(msg: () -> Any?) {
    if (isErrorEnabled) {
        error(WARN_ERROR_PREFIX + logMessageSafe(msg = msg))
    }
}

/**
 * ERROR 레벨이 활성화된 경우 예외와 [WARN_ERROR_PREFIX] 접두 메시지를 기록합니다.
 *
 * ```kotlin
 * logger.error(exception) { "DB 연결 실패" }
 * ```
 */
inline fun Logger.error(cause: Throwable?, msg: () -> Any?) {
    if (isErrorEnabled) {
        error(WARN_ERROR_PREFIX + logMessageSafe(msg = msg), cause)
    }
}

/**
 * ERROR 레벨이 활성화된 경우 marker/예외/[WARN_ERROR_PREFIX] 접두 메시지를 기록합니다.
 *
 * ```kotlin
 * logger.error(marker, exception) { "마커 포함 ERROR 로그" }
 * ```
 */
inline fun Logger.error(marker: Marker?, cause: Throwable?, msg: () -> Any?) {
    if (isErrorEnabled) {
        error(marker, WARN_ERROR_PREFIX + logMessageSafe(msg = msg), cause)
    }
}
