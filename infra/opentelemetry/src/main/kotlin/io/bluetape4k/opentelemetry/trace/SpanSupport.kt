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

inline fun <T> Span.use(waitDuration: Duration, block: (Span) -> T): T =
    use(waitDuration.toMillis().coerceAtLeast(0L), block)

inline fun <T> SpanBuilder.useSpan(waitTimeout: Long? = null, block: (Span) -> T): T =
    startSpan().use(waitTimeout, block)

inline fun <T> SpanBuilder.useSpan(waitDuration: Duration, block: (Span) -> T): T =
    useSpan(waitDuration.toMillis().coerceAtLeast(0L), block)
