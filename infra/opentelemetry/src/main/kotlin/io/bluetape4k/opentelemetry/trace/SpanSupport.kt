package io.bluetape4k.opentelemetry.trace

import io.bluetape4k.support.requireNotBlank
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import kotlinx.coroutines.CancellationException
import java.time.Duration

private const val UNSPECIFIED_ERROR = "unspecified error"
private const val EXCEPTION_EVENT_NAME = "exception"
private val EXCEPTION_TYPE_ATTRIBUTE = AttributeKey.stringKey("exception.type")
private val EXCEPTION_MESSAGE_ATTRIBUTE = AttributeKey.stringKey("exception.message")


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
 * - 일반 예외는 redacted `exception` 이벤트와 `ERROR` 상태를 남긴 뒤 원본 예외를 그대로 다시 던집니다.
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
 * - 예외 처리와 종료 시맨틱은 [Span.use]와 동일하며, 원본 예외 메시지는 기본 export하지 않습니다.
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

    addEvent(
        EXCEPTION_EVENT_NAME,
        Attributes.builder()
            .put(EXCEPTION_TYPE_ATTRIBUTE, error::class.java.name)
            .put(EXCEPTION_MESSAGE_ATTRIBUTE, UNSPECIFIED_ERROR)
            .build(),
    )
    setStatus(StatusCode.ERROR, UNSPECIFIED_ERROR)
}

/**
 * blocking 환경에서 새로운 [Span]을 생성하고 [block]을 실행한 뒤 Span을 자동으로 종료합니다.
 *
 * ## 동작/계약
 * - 정상 종료 시 [StatusCode.OK]를 설정하고 Span을 종료합니다.
 * - 일반 예외 발생 시 redacted `exception` event와 [StatusCode.ERROR] 설정 후 재던집니다.
 *   status description과 event message는 `"unspecified error"`를 사용해 원본 예외 메시지를 기본 노출하지 않습니다.
 * - [CancellationException]은 상태 변경 없이 그대로 전파합니다 (UNSET 유지).
 *
 * ## 보안 경고
 * - [configure] 람다에서 PII, Authorization 토큰, 민감 헤더를 attribute로 설정하지 마세요.
 * - [configure] 람다에서 `.startSpan()`을 직접 호출하면 이중 Span이 생성됩니다 (footgun).
 *
 * ## 코루틴 사용 주의
 * - 이 함수는 blocking 전용입니다. suspend 환경에서는 `SpanCoroutineSupport.kt`의
 *   `suspend Tracer.withSpan(...)` 을 사용하세요.
 * - Dispatcher 전환 시 ThreadLocal 기반 OTel Context가 유실될 수 있습니다.
 *
 * ```kotlin
 * val result = tracer.withSpan("my-operation") { span ->
 *     span.setAttribute("key", "value")
 *     "done"
 * }
 * ```
 *
 * @param spanName Span 이름 (공백 불가)
 * @param configure [SpanBuilder] 설정 람다 — attribute, kind, parent 등
 * @param block Span을 인자로 받는 실행 블록
 * @return [block]의 실행 결과
 */
public inline fun <T> Tracer.withSpan(
    spanName: String,
    configure: SpanBuilder.() -> Unit = {},
    block: (Span) -> T,
): T {
    spanName.requireNotBlank("spanName")
    val span = spanBuilder(spanName).apply(configure).startSpan()
    return span.makeCurrent().use {
        try {
            block(span).also { span.setStatus(StatusCode.OK) }
        } catch (e: Throwable) {
            span.recordFailure(e)
            throw e
        } finally {
            span.end()
        }
    }
}

@PublishedApi
internal fun Span.endSafely(@Suppress("UNUSED_PARAMETER") waitTimeout: Long?) {
    end()
}
