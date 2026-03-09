package io.bluetape4k.opentelemetry.coroutines

import io.bluetape4k.coroutines.support.getOrCurrent
import io.bluetape4k.opentelemetry.trace.endSafely
import io.bluetape4k.opentelemetry.trace.recordFailure
import io.bluetape4k.opentelemetry.trace.use
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * [Span]을 현재 코루틴 컨텍스트에 전파한 뒤 [block]을 실행하고, 실행이 끝나면 Span을 자동으로 종료합니다.
 *
 * ## 동작/계약
 * - [withSpanContext]를 이용해 현재 span을 코루틴 컨텍스트에 설치합니다.
 * - 일반 예외는 span에 기록하고 `ERROR` 상태로 바꾼 뒤 원본 예외를 그대로 다시 던집니다.
 * - [CancellationException]은 취소 의미를 보존하기 위해 span 상태를 바꾸지 않고 그대로 전파합니다.
 * - `waitTimeout`은 하위 호환용 인자이며, 현재 구현은 trace duration 왜곡을 막기 위해 즉시 종료합니다.
 */
suspend inline fun <T> Span.useSuspending(
    waitTimeout: Long? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T =
    withSpanContext(this@useSuspending, coroutineContext, waitTimeout) {
        block(it)
    }

/**
 * [Duration] 기반 overload 입니다.
 *
 * `waitDuration` 역시 하위 호환용 인자이며 종료 시각을 미래로 밀어 쓰지 않습니다.
 */
suspend inline fun <T> Span.useSuspending(
    waitDuration: Duration,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T = useSuspending(waitDuration.toMillis().coerceAtLeast(0L), coroutineContext, block)

/**
 * 새로운 [Span]을 생성해 Coroutines 환경에서 실행합니다.
 */
suspend inline fun <T> SpanBuilder.useSpanSuspending(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T = startSpan().useSuspending(coroutineContext = coroutineContext) { span ->
    block(span)
}

@Deprecated(
    "use useSpanSuspending instead.",
    replaceWith = ReplaceWith("useSpanSuspending(coroutineContext, block)")
)
suspend inline fun <T> SpanBuilder.useSuspendSpan(
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T = useSpanSuspending(coroutineContext, block)

/**
 * `waitTimeout` 기반 overload 입니다.
 *
 * `waitTimeout`은 하위 호환용 인자이며 종료 시각을 미래로 밀어 쓰지 않습니다.
 */
suspend inline fun <T> SpanBuilder.useSpanSuspending(
    waitTimeout: Long? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T = startSpan().useSuspending(waitTimeout, coroutineContext) { span ->
    block(span)
}

@Deprecated(
    "use useSpanSuspending instead.",
    replaceWith = ReplaceWith("useSpanSuspending(waitTimeout, coroutineContext, block)")
)
suspend inline fun <T> SpanBuilder.useSuspendSpan(
    waitTimeout: Long? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T = startSpan().useSuspending(waitTimeout, coroutineContext) { span ->
    block(span)
}

/**
 * [Duration] 기반 overload 입니다.
 *
 * `waitDuration` 역시 하위 호환용 인자이며 종료 시각을 미래로 밀어 쓰지 않습니다.
 */
suspend inline fun <T> SpanBuilder.useSpanSuspending(
    waitDuration: Duration,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T = useSpanSuspending(
    waitDuration.toMillis().coerceAtLeast(0L),
    coroutineContext,
    block,
)

@Deprecated(
    "use useSpanSuspending instead.",
    replaceWith = ReplaceWith("useSpanSuspending(waitDuration, coroutineContext, block)")
)
suspend inline fun <T> SpanBuilder.useSuspendSpan(
    waitDuration: Duration,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T = useSpanSuspending(
    waitDuration.toMillis().coerceAtLeast(0L),
    coroutineContext,
    block,
)

/**
 * Coroutines 환경에서 [span]을 명시적으로 전파합니다.
 *
 * ## 동작/계약
 * - `Context.current()`에 [span]을 저장한 뒤 [asContextElement]로 코루틴 컨텍스트에 연결합니다.
 * - 일반 예외는 span에 기록하고 `ERROR` 상태를 남긴 뒤 원본 예외를 다시 던집니다.
 * - [CancellationException]은 취소 전파를 보존하기 위해 가공하지 않습니다.
 * - `waitTimeout`은 하위 호환용 인자이며, 현재 구현은 trace duration 왜곡을 막기 위해 즉시 종료합니다.
 */
suspend inline fun <T> withSpanContext(
    span: Span,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    waitTimeout: Long? = null,
    crossinline block: suspend (Span) -> T,
): T {
    // javaagent + coroutine 환경에서 Context.current().with(span) 가
    // AgentContextWrapper 에 의해 무시되는 경우가 있어 storeInContext 사용
    return try {
        val otelContext: Context = span.storeInContext(Context.current())
        withContext(coroutineContext.getOrCurrent() + otelContext.asContextElement()) {
            block(span)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        span.recordFailure(e)
        throw e
    } finally {
        span.endSafely(waitTimeout)
    }
}
