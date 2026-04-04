package io.bluetape4k.opentelemetry.trace

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.CancellationException
import java.time.Duration


/**
 * no-op operation을 위한 invalid [SpanContext]
 *
 * ```kotlin
 * val ctx = InvalidSpanContext
 * // ctx.isValid == false
 * ```
 */
@JvmField
val InvalidSpanContext: SpanContext = SpanContext.getInvalid()

/**
 * [Span]을 사용하여 코드 블록을 실행하고, 실행이 끝나면 Span을 자동으로 종료합니다.
 *
 * ## 동작/계약
 * - 일반 예외는 span에 `exception` 이벤트와 `ERROR` 상태를 남긴 뒤 원본 예외를 그대로 다시 던집니다.
 * - [CancellationException]은 취소 의미를 보존하기 위해 오류로 기록하지 않고 그대로 전파합니다.
 * - `waitTimeout`은 하위 호환을 위해 유지되며, 현재 구현은 trace duration 왜곡을 피하기 위해 span을 즉시 종료합니다.
 *
 * ```kotlin
 * val tracer = NoopOpenTelemetry.getTracer("example")
 * val result = tracer.spanBuilder("my-span").startSpan().use { span ->
 *     span.setAttribute("key", "value")
 *     "done"
 * }
 * // result == "done"
 * ```
 *
 * @param waitTimeout 하위 호환을 위해 남겨둔 종료 대기 시간 인자입니다. 현재 구현은 trace duration 왜곡을 막기 위해 즉시 종료합니다.
 * @param block 실행할 코드 블록
 * @return 코드 블록의 실행 결과
 */
inline fun <T> Span.use(waitTimeout: Long? = null, block: (Span) -> T): T {
    return makeCurrent().use {
        try {
            block(this)
        } catch (e: Throwable) {
            recordFailure(e)
            throw e
        } finally {
            endSafely(waitTimeout)
        }
    }
}

/**
 * [Span]을 사용하여 코드 블록을 실행하고, 실행이 끝나면 Span을 자동으로 종료합니다.
 *
 * ## 동작/계약
 * - [Duration]을 밀리초로 변환한 뒤 `0` 미만 값은 `0`으로 보정합니다.
 * - 나머지 동작은 [Span.use]와 동일합니다.
 *
 * @param waitDuration 하위 호환을 위해 남겨둔 종료 대기 시간 인자입니다. 현재 구현은 trace duration 왜곡을 막기 위해 즉시 종료합니다.
 * @param block 실행할 코드 블록
 * @return 코드 블록의 실행 결과
 */
inline fun <T> Span.use(waitDuration: Duration, block: (Span) -> T): T =
    use(waitDuration.toMillis().coerceAtLeast(0L), block)

/**
 * [SpanBuilder]를 사용하여 새로운 Span을 생성하고, 코드 블록을 실행한 후 Span을 자동으로 종료합니다.
 *
 * ## 동작/계약
 * - 새 span을 생성한 뒤 [Span.use]에 위임합니다.
 * - 예외 처리와 종료 시맨틱은 [Span.use]와 동일합니다.
 *
 * @param waitTimeout 하위 호환을 위해 남겨둔 종료 대기 시간 인자입니다. 현재 구현은 trace duration 왜곡을 막기 위해 즉시 종료합니다.
 * @param block 실행할 코드 블록
 * @return 코드 블록의 실행 결과
 */
inline fun <T> SpanBuilder.useSpan(waitTimeout: Long? = null, block: (Span) -> T): T =
    startSpan().use(waitTimeout, block)

/**
 * [SpanBuilder]를 사용하여 새로운 Span을 생성하고, 코드 블록을 실행한 후 Span을 자동으로 종료합니다.
 *
 * ## 동작/계약
 * - [Duration]을 밀리초로 변환한 뒤 `0` 미만 값은 `0`으로 보정합니다.
 * - 나머지 동작은 [SpanBuilder.useSpan]과 동일합니다.
 *
 * @param waitDuration 하위 호환을 위해 남겨둔 종료 대기 시간 인자입니다. 현재 구현은 trace duration 왜곡을 막기 위해 즉시 종료합니다.
 * @param block 실행할 코드 블록
 * @return 코드 블록의 실행 결과
 */
inline fun <T> SpanBuilder.useSpan(waitDuration: Duration, block: (Span) -> T): T =
    useSpan(waitDuration.toMillis().coerceAtLeast(0L), block)

@PublishedApi
internal fun Span.recordFailure(error: Throwable) {
    if (error is CancellationException) {
        return
    }

    recordException(error)
    setStatus(StatusCode.ERROR, error.message ?: error::class.java.simpleName)
}

@PublishedApi
internal fun Span.endSafely(@Suppress("UNUSED_PARAMETER") waitTimeout: Long?) {
    end()
}
