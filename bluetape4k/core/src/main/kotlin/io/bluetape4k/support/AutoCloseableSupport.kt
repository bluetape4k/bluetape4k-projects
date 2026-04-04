package io.bluetape4k.support

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [AutoCloseable]을 안전하게 닫습니다. 예외가 발생해도 전파하지 않습니다.
 *
 * ```kotlin
 * val conn: Connection = getConnection()
 * conn.closeSafe()  // 예외 무시
 *
 * // 예외 발생 시 로깅
 * conn.closeSafe { error -> logger.warn("close 실패", error) }
 * ```
 *
 * @param errorHandler close 시 예외 발생 시 수행할 handler, 기본은 아무 일도 하지 않는다
 */
inline fun AutoCloseable.closeSafe(errorHandler: (error: Throwable) -> Unit = {}) {
    try {
        close()
    } catch (ignored: Throwable) {
        errorHandler(ignored)
    }
}

/**
 * close 시에 제한시간[timeoutMillis]을 지정하여 수행합니다.
 *
 * 지정한 시간 내에 close가 완료되지 않으면 [errorHandler]를 호출하고 종료합니다.
 *
 * ```kotlin
 * // 1초 내에 close, 초과 시 오류 핸들러 호출
 * slowResource.closeTimeout(1_000L) { error ->
 *     logger.warn("close 타임아웃", error)
 * }
 * ```
 *
 * @param timeoutMillis close 의 최대 수행 시간
 * @param errorHandler close 에서 예외 발생 시 수행할 함수
 */
inline fun AutoCloseable.closeTimeout(
    timeoutMillis: Long = 3_000L,
    crossinline errorHandler: (error: Throwable) -> Unit = {},
) {
    try {
        asyncRunWithTimeout(timeoutMillis) { closeSafe(errorHandler) }.join()
    } catch (e: Throwable) {
        errorHandler(e.cause ?: e)
    }
}

/**
 * close 시에 제한시간[timeout]을 지정하여 수행합니다.
 *
 * @param timeout close 의 최대 수행 시간
 * @param errorHandler close 에서 예외 발생 시 수행할 함수
 */
inline fun AutoCloseable.closeTimeout(
    timeout: Duration = 3.seconds,
    crossinline errorHandler: (error: Throwable) -> Unit = {},
) = closeTimeout(timeout.inWholeMilliseconds, errorHandler)

/**
 * [AutoCloseable] 리소스를 사용하고 완료 후 안전하게 닫습니다.
 *
 * 표준 라이브러리의 `use`와 달리, close 중 예외가 발생해도 전파되지 않습니다.
 *
 * ```kotlin
 * val result = connection useSafe { conn ->
 *     conn.prepareStatement("SELECT 1").executeQuery()
 * }
 * // action 완료 후 connection.closeSafe() 자동 호출
 * ```
 *
 * @param action 수행할 함수
 * @return 수행 결과
 */
inline infix fun <T: AutoCloseable, R> T.useSafe(action: (T) -> R): R {
    return try {
        action(this)
    } finally {
        closeSafe()
    }
}

/**
 * [AutoCloseable]을 사용하는 함수를 수행합니다.
 *
 * @param action 수행할 함수
 * @return 수행 결과
 */
@Deprecated("use useSafe instead", ReplaceWith("this.useSafe(action)"))
inline infix fun <T: AutoCloseable, R> T.using(action: (T) -> R): R {
    return try {
        action(this)
    } finally {
        closeSafe()
    }
}
