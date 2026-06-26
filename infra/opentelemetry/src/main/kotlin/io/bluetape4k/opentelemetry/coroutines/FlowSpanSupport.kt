package io.bluetape4k.opentelemetry.coroutines

import io.bluetape4k.opentelemetry.trace.recordFailure
import io.bluetape4k.support.requireNotBlank
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext

/**
 * 이 [Flow]의 collect를 단일 [io.opentelemetry.api.trace.Span]으로 감싸는 새로운 [Flow]를 반환합니다.
 *
 * ## 동작/계약
 * - **1 collect = 1 Span**: collect가 한 번 실행될 때마다 Span이 하나 생성됩니다.
 *   emit 횟수와 무관합니다. 아이템별 Span이 필요하면 `onEach { }` 안에서 직접 Span을 관리하세요.
 * - 정상 종료 시 [StatusCode.OK]를 설정하고 Span을 종료합니다.
 * - [CancellationException] (upstream 또는 downstream 취소) 시 상태를 UNSET으로 유지하고 Span을 종료합니다.
 * - 일반 예외 발생 시 redacted `exception` event와 [StatusCode.ERROR] 설정 후 Span을 종료하고 예외를 재던집니다.
 *   status description과 event message는 `"unspecified error"`를 사용해 원본 예외 메시지를 기본 노출하지 않습니다.
 *
 * ## 구현 주의
 * - 내부적으로 `channelFlow { }` 를 사용합니다. `flow { }` + `withContext` + `emit()` 조합은
 *   Flow invariant를 위반하므로 사용하지 않습니다.
 * - OTel Context 전파에 `span.storeInContext(Context.current()).asContextElement()`를 직접 사용합니다.
 *   `withSpanContext`는 내부적으로 `endSafely()`를 호출하므로 재사용하면 이중 `end()`가 발생합니다.
 * - [configure] 람다는 collect마다 새 [SpanBuilder]에서 실행됩니다.
 *   카운터 증가 등 부수 효과를 [configure] 안에 넣지 마세요.
 *
 * ## traced vs tracedCollect 선택 기준
 * | 시나리오 | 권장 |
 * |---------|------|
 * | Flow를 변환 체인에 연결해야 함 | `traced()` |
 * | collect와 동시에 Span을 설정하고 싶음 | `tracedCollect()` |
 *
 * ## 보안 경고
 * - [configure] 람다에서 PII, Authorization 토큰, 민감 헤더를 attribute로 설정하지 마세요.
 * - [configure] 람다에서 `.startSpan()`을 직접 호출하면 이중 Span이 생성됩니다 (footgun).
 *
 * ```kotlin
 * flowOf(1, 2, 3)
 *     .traced(tracer, "my-flow") { setAttribute("key", "value") }
 *     .collect { println(it) }
 * ```
 *
 * @param tracer OTel [Tracer]
 * @param spanName Span 이름 (공백 불가)
 * @param configure [SpanBuilder] 설정 람다 — attribute, kind, parent 등. collect마다 실행됨.
 * @return Span으로 감싸진 새로운 [Flow]
 */
public fun <T> Flow<T>.traced(
    tracer: Tracer,
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
): Flow<T> {
    spanName.requireNotBlank("spanName")
    return channelFlow {
        val span = tracer.spanBuilder(spanName).apply(configure).startSpan()
        try {
            val otelContext = span.storeInContext(Context.current())
            withContext(otelContext.asContextElement()) {
                this@traced.collect { value -> send(value) }
            }
            span.setStatus(StatusCode.OK)
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            span.recordFailure(t)
            throw t
        } finally {
            span.end()
        }
    }
}

/**
 * 이 [Flow]를 단일 [io.opentelemetry.api.trace.Span]으로 감싸 collect합니다.
 *
 * ## 동작/계약
 * - [traced]와 달리 [action]도 Span의 OTel Context 안에서 실행됩니다.
 *   따라서 [action] 안에서 `Span.current()`를 호출하면 이 함수가 생성한 Span이 반환됩니다.
 * - 정상 종료 시 [StatusCode.OK]를 설정하고 Span을 종료합니다.
 * - [CancellationException] 시 상태를 UNSET으로 유지하고 Span을 종료합니다.
 * - 일반 예외 발생 시 redacted `exception` event와 [StatusCode.ERROR] 설정 후 재던집니다.
 *
 * ## [traced] 와의 차이
 * - `traced()` 는 OTel Context 를 producer(upstream) 코루틴에만 설치합니다.
 *   `tracedCollect` 는 OTel Context 를 [action] 이 실행되는 consumer 코루틴에도 설치합니다.
 *
 * ```kotlin
 * flowOf(1, 2, 3).tracedCollect(tracer, "my-flow") { item ->
 *     Span.current()  // 이 Span이 tracedCollect 가 생성한 Span 입니다
 *     println(item)
 * }
 * ```
 *
 * @param tracer OTel [Tracer]
 * @param spanName Span 이름 (공백 불가)
 * @param configure [SpanBuilder] 설정 람다
 * @param action collect 시 각 아이템에 대해 실행할 suspend 함수
 */
public suspend fun <T> Flow<T>.tracedCollect(
    tracer: Tracer,
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
    action: suspend (T) -> Unit,
) {
    spanName.requireNotBlank("spanName")
    val span = tracer.spanBuilder(spanName).apply(configure).startSpan()
    try {
        val otelContext = span.storeInContext(Context.current())
        withContext(otelContext.asContextElement()) {
            this@tracedCollect.collect { value -> action(value) }
        }
        span.setStatus(StatusCode.OK)
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        span.recordFailure(t)
        throw t
    } finally {
        span.end()
    }
}
