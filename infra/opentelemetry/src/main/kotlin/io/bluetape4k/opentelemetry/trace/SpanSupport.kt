package io.bluetape4k.opentelemetry.trace

import io.bluetape4k.exceptions.BluetapeException
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import java.time.Duration
import java.time.Instant


/**
 * no-op operation을 위한 invalid [SpanContext]
 */
@JvmField
val InvalidSpanContext: SpanContext = SpanContext.getInvalid()

/**
 * [Span]을 사용하여 코드 블록을 실행하고, 실행이 끝나면 Span을 자동으로 종료합니다.
 *
 * @param waitTimeout Span 종료를 기다리는 시간 (밀리초)
 * @param block 실행할 코드 블록
 * @return 코드 블록의 실행 결과
 */
inline fun <T> Span.use(waitTimeout: Long? = null, block: (Span) -> T): T {
    return makeCurrent().use {
        try {
            block(this)
        } catch (e: Throwable) {
            setStatus(StatusCode.ERROR, "Error while executing block")
            throw BluetapeException("Fail to execute block", e)
        } finally {
            waitTimeout?.run { end(Instant.now().plusMillis(this)) } ?: end()
        }
    }
}

/**
 * [Span]을 사용하여 코드 블록을 실행하고, 실행이 끝나면 Span을 자동으로 종료합니다.
 *
 * @param waitDuration Span 종료를 기다리는 시간
 * @param block 실행할 코드 블록
 * @return 코드 블록의 실행 결과
 */
inline fun <T> Span.use(waitDuration: Duration, block: (Span) -> T): T =
    use(waitDuration.toMillis().coerceAtLeast(0L), block)

/**
 * [SpanBuilder]를 사용하여 새로운 Span을 생성하고, 코드 블록을 실행한 후 Span을 자동으로 종료합니다.
 *
 * @param waitTimeout Span 종료를 기다리는 시간 (밀리초)
 * @param block 실행할 코드 블록
 * @return 코드 블록의 실행 결과
 */
inline fun <T> SpanBuilder.useSpan(waitTimeout: Long? = null, block: (Span) -> T): T =
    startSpan().use(waitTimeout, block)

/**
 * [SpanBuilder]를 사용하여 새로운 Span을 생성하고, 코드 블록을 실행한 후 Span을 자동으로 종료합니다.
 *
 * @param waitDuration Span 종료를 기다리는 시간
 * @param block 실행할 코드 블록
 * @return 코드 블록의 실행 결과
 */
inline fun <T> SpanBuilder.useSpan(waitDuration: Duration, block: (Span) -> T): T =
    useSpan(waitDuration.toMillis().coerceAtLeast(0L), block)
