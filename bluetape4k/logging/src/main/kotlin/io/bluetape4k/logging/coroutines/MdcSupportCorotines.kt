package io.bluetape4k.logging.coroutines

import io.bluetape4k.logging.withLoggingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext

/**
 * Coroutine 환경 하에서 MDC를 사용하여 로깅 컨텍스트를 설정합니다.
 *
 * ```
 * withCoroutineLoggingContext("traceId" to traceId) {
 *     log.debug { "Some messages ..." }
 * }
 *
 * withCoroutineLoggingContext("userId" to userId, preservePrevious = false) {
 *     log.debug { "Some messages ..." }
 * }
 * ```
 *
 * logback log pattern 을 다음과 같이 `traceId=%X{traceId}` 를 추가해야 MDC `traceId` 가 로그애 출력됩니다.
 *
 * ```
 * %d{HH:mm:ss.SSS} %highlight(%-5level)[traceId=%X{traceId}][%.24thread] %logger{36}:%line: %msg%n%throwable
 * ```
 *
 * @param pair 로깅 컨텍스트에 추가할 키-값 쌍
 * @param restorePrevious 이전에 설정된 로깅 컨텍스트를 유지할지 여부
 * @param block 로깅 컨텍스트가 설정된 블록
 * @return 블록의 실행 결과
 */
suspend inline fun <T> withCoroutineLoggingContext(
    pair: Pair<String, Any?>,
    restorePrevious: Boolean = true,
    crossinline block: suspend CoroutineScope.() -> T,
): T = coroutineScope {
    withContext(currentCoroutineContext() + MDCContext()) {
        withLoggingContext(pair, restorePrevious) {
            block(this)
        }
    }
}

/**
 * Coroutine 환경 하에서 MDC를 사용하여 로깅 컨텍스트를 설정합니다.
 *
 * ```
 * withCoroutineLoggingContext("traceId" to traceId) {
 *     log.debug { "Some messages ..." }
 * }
 *
 * withCoroutineLoggingContext("userId" to userId, preservePrevious = false) {
 *     log.debug { "Some messages ..." }
 * }
 * ```
 *
 * logback log pattern 을 다음과 같이 `traceId=%X{traceId}` 를 추가해야 MDC `traceId` 가 로그애 출력됩니다.
 *
 * ```
 * %d{HH:mm:ss.SSS} %highlight(%-5level)[traceId=%X{traceId}][%.24thread] %logger{36}:%line: %msg%n%throwable
 * ```
 *
 * @param restorePrevious 이전에 설정된 로깅 컨텍스트를 유지할지 여부
 * @param pairs 로깅 컨텍스트에 추가할 키-값 쌍
 * @param block 로깅 컨텍스트가 설정된 블록
 * @return 블록의 실행 결과
 */
suspend inline fun <T> withCoroutineLoggingContext(
    vararg pairs: Pair<String, Any?>,
    restorePrevious: Boolean = true,
    crossinline block: suspend CoroutineScope.() -> T,
): T = withCoroutineLoggingContext(pairs.toMap(), restorePrevious, block)

/**
 * Coroutine 환경 하에서 MDC를 사용하여 로깅 컨텍스트를 설정합니다.
 *
 * ```
 * withCoroutineLoggingContext("traceId" to traceId) {
 *     log.debug { "Some messages ..." }
 * }
 *
 * withCoroutineLoggingContext("userId" to userId, preservePrevious = false) {
 *     log.debug { "Some messages ..." }
 * }
 * ```
 *
 * logback log pattern 을 다음과 같이 `traceId=%X{traceId}` 를 추가해야 MDC `traceId` 가 로그애 출력됩니다.
 *
 * ```
 * %d{HH:mm:ss.SSS} %highlight(%-5level)[traceId=%X{traceId}][%.24thread] %logger{36}:%line: %msg%n%throwable
 * ```
 *
 * @param map 로깅 컨텍스트에 추가할 키-값 쌍
 * @param restorePrevious 이전에 설정된 로깅 컨텍스트를 유지할지 여부
 * @param block 로깅 컨텍스트가 설정된 블록
 * @return 블록의 실행 결과
 */
suspend inline fun <T> withCoroutineLoggingContext(
    map: Map<String, Any?>,
    restorePrevious: Boolean = true,
    crossinline block: suspend CoroutineScope.() -> T,
): T = coroutineScope {
    withContext(MDCContext()) {
        withLoggingContext(map, restorePrevious) {
            block(this)
        }
    }
}
