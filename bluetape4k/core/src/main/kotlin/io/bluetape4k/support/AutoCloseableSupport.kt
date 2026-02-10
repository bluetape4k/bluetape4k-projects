package io.bluetape4k.support

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * [AutoCloseable]을 안전하게 close 를 수행합니다.
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
) {
    closeTimeout(timeout.inWholeMilliseconds, errorHandler)
}

/**
 * [AutoCloseable]을 사용하는 함수를 수행합니다.
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
