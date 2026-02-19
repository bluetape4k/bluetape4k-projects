package io.bluetape4k.opentelemetry.coroutines

import io.bluetape4k.coroutines.support.getOrCurrent
import io.bluetape4k.exceptions.BluetapeException
import io.bluetape4k.opentelemetry.trace.use
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * [Span]을 사용하여 코드 블록을 실행하고, 실행이 끝나면 Span을 자동으로 종료합니다.
 */
suspend inline fun <T> Span.useSuspending(
    waitTimeout: Long? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T =
    withSpanContext(this@useSuspending, coroutineContext, waitTimeout) {
        block(it)
    }

/** Duration overload */
suspend inline fun <T> Span.useSuspending(
    waitDuration: Duration,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    crossinline block: suspend (Span) -> T,
): T = useSuspending(waitDuration.toMillis().coerceAtLeast(0L), coroutineContext, block)

/**
 * 새로운 Span 을 생성하여 Coroutines 환경에서 실행합니다.
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
): T = startSpan().use { span ->
    withSpanContext(span, coroutineContext) {
        block(it)
    }
}

/** waitTimeout overload */
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

/** Duration overload */
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
 * Coroutines 환경에서 Span Context 를 명시적으로 전파한다.
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
    } catch (e: Throwable) {
        span.setStatus(StatusCode.ERROR, "Error while executing block")
        throw BluetapeException("Fail to execute block", e)
    } finally {
        waitTimeout?.let { span.end(Instant.now().plusMillis(it)) } ?: span.end()
    }
}
