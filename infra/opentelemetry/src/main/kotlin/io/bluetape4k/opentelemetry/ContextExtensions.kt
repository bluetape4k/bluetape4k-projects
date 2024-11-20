package io.bluetape4k.opentelemetry

import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context

/**
 * 현 [io.opentelemetry.context.Scope]에 연관된 [Context]를 반환합니다.
 */
fun currentOtelContext(): Context = Context.current()

/**
 * Root [Context]를 반환합니다.
 *
 * It should generally not be required to use the root [Context] directly - instead,
 * use [Context.current()] to operate on the current [Context].
 *
 * Only use this method if you are absolutely sure you need to disregard the current [Context]
 * - this almost always is only a workaround hiding an underlying context propagation issue.
 */
fun rootOtelContext(): Context = Context.root()

/**
 * Current Opentelemetry [Context] 하에서 [action]을 실행합니다.
 *
 * @param T [action]의 실행 결과 타입입니다.
 * @param action 실행할 코드 블록입니다.
 * @return [action]의 실행 결과입니다.
 */
inline fun <T> Context.withCurrent(action: () -> T): T {
    return makeCurrent().use { action() }
}

/**
 * Otel current scope의 [Span]을 반환합니다. 만약 Span이 존재하지 않다면,
 * [io.opentelemetry.api.trace.PropagatedSpan.INVALID]을 반환합니다.
 *
 * @see [io.opentelemetry.api.trace.Span]
 * @see [io.opentelemetry.api.trace.PropagatedSpan.INVALID]
 */
fun Context.getSpan(): Span = Span.fromContext(this)

/**
 * Otel current scope의 [Span]을 반환합니다. 만약 Span이 존재하지 않다면, null 을 반환합니다.
 */
fun Context.getSpanOrNull(): Span? = Span.fromContextOrNull(this)
