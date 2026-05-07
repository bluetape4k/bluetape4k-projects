package io.bluetape4k.opentelemetry.trace

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import kotlinx.coroutines.CancellationException
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.TimeUnit

/**
 * NOTE: 테스트 시에 java agent 를 사용하면서 SdkTraceProvider 를 통해 tracer 를 얻으면 충돌이 납니다.
 *
 * 추천하는 운영 구성(한 줄)
 *
 * - 운영/통합환경: agent ON + GlobalOpenTelemetry 사용(앱에서 SDK 만들지 않기)
 * - 단위테스트: agent OFF + InMemorySpanExporter로 parentSpanId까지 정밀 assert
 * - 통합테스트: agent ON + logging/otlp exporter로 “트레이스가 붙는지”만 확인(정밀 assert는 보통 어려움)
 */
class SpanSupportTest: AbstractOtelTest() {

    private val spanExporter = InMemorySpanExporter.create()
    private val tracerProvider = sdkTracerProvider {
        addSpanProcessor(simpleSpanProcessorOf(spanExporter))
    }
    private val tracer = tracerProvider.get("io.bluetape4k.otel.trace.span-support")

    private fun flush() {
        tracerProvider.forceFlush().join(1, TimeUnit.SECONDS)
    }

    @Test
    fun `Span use should end span`() = runSuspendIO {
        spanExporter.reset()

        val span = tracer.spanBuilder("use-span").startSpan()
        span.use {
            it.setAttribute(AttributeKey.stringKey("k"), "v")
        }

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].name shouldBeEqualTo "use-span"
        finished[0].attributes[AttributeKey.stringKey("k")] shouldBeEqualTo "v"
    }

    @Test
    fun `Span use should set ERROR status and record exception if supported`() = runSuspendIO {
        spanExporter.reset()

        val span = tracer.spanBuilder("error-span").startSpan()
        val failure = IllegalStateException("boom")

        val ex = kotlin.runCatching {
            span.use {
                it.setAttribute(AttributeKey.stringKey("before"), "true")
                throw failure
            }
        }.exceptionOrNull()

        ex.shouldNotBeNull()
        (ex === failure).shouldBeTrue()

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        val s = finished[0]
        s.name shouldBeEqualTo "error-span"
        s.status.statusCode shouldBeEqualTo StatusCode.ERROR
        s.events.any { it.name == "exception" }.shouldBeTrue()
    }

    @Test
    fun `Span use should propagate cancellation without converting it to error`() = runSuspendIO {
        spanExporter.reset()

        val ex = kotlin.runCatching {
            tracer.spanBuilder("cancel-span").startSpan().use {
                throw CancellationException("cancelled")
            }
        }.exceptionOrNull()

        ex.shouldNotBeNull()
        (ex is CancellationException).shouldBeTrue()

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1

        val span = finished[0]
        span.name shouldBeEqualTo "cancel-span"
        span.status.statusCode shouldBeEqualTo StatusCode.UNSET
        span.events.any { it.name == "exception" }.shouldBeFalse()
    }

    @Test
    fun `SpanSupport should allow building child span with explicit parent context`() = runSuspendIO {
        spanExporter.reset()

        val parent = tracer.spanBuilder("parent").startSpan()
        val parentCtx = parent.storeInContext(io.opentelemetry.context.Context.current())

        val child = tracer.spanBuilder("child")
            .setParent(parentCtx)
            .startSpan()

        child.end()
        parent.end()

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 2

        val parentData = finished.find { it.name == "parent" }
        parentData.shouldNotBeNull()

        val childData = finished.find { it.name == "child" }
        childData.shouldNotBeNull()

        childData.traceId shouldBeEqualTo parentData.traceId
        childData.parentSpanId shouldBeEqualTo parentData.spanId
    }

    /**
     * [SpanBuilder.useSpan]이 span을 시작하고 블록 실행 후 자동으로 종료하는지 검증합니다.
     * SpanBuilder DSL을 통해 span 생성부터 종료까지 단일 흐름으로 처리됩니다.
     */
    @Test
    fun `SpanBuilder useSpan should start and end span`() = runSuspendIO {
        spanExporter.reset()

        val result = tracer.spanBuilder("builder-use-span").useSpan { span ->
            span.setAttribute(AttributeKey.stringKey("key"), "value")
            "done"
        }

        result shouldBeEqualTo "done"
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].name shouldBeEqualTo "builder-use-span"
        finished[0].attributes[AttributeKey.stringKey("key")] shouldBeEqualTo "value"
    }

    /**
     * [SpanBuilder.useSpan]이 예외 발생 시 ERROR 상태를 설정하고 span을 반드시 종료하는지 검증합니다.
     * try-finally 보장으로 예외 경로에서도 span 누수가 없음을 확인합니다.
     */
    @Test
    fun `SpanBuilder useSpan should set ERROR and end span on exception`() = runSuspendIO {
        spanExporter.reset()
        val failure = RuntimeException("useSpan-error")

        val ex = kotlin.runCatching {
            tracer.spanBuilder("builder-error-span").useSpan {
                throw failure
            }
        }.exceptionOrNull()

        ex.shouldNotBeNull()
        (ex is RuntimeException).shouldBeTrue()
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].status.statusCode shouldBeEqualTo StatusCode.ERROR
        finished[0].events.any { it.name == "exception" }.shouldBeTrue()
    }

    /**
     * [SpanBuilder.useSpan]의 Duration 오버로드가 정상적으로 동작하는지 검증합니다.
     * 하위 호환용 waitDuration 인자를 허용하면서 span이 즉시 종료됩니다.
     */
    @Test
    fun `SpanBuilder useSpan with Duration should end span`() = runSuspendIO {
        spanExporter.reset()

        val result = tracer.spanBuilder("builder-duration-span").useSpan(Duration.ofMillis(10)) {
            "done"
        }

        result shouldBeEqualTo "done"
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].name shouldBeEqualTo "builder-duration-span"
    }

    /**
     * [Tracer.startSpan] DSL 확장 함수가 올바르게 span을 생성하는지 검증합니다.
     * SpanBuilder 람다를 통해 속성을 설정한 span이 수동 종료 후 완료됩니다.
     */
    @Test
    fun `Tracer startSpan DSL should create span with attributes`() = runSuspendIO {
        spanExporter.reset()

        val span = tracer.startSpan("dsl-span") {
            setAttribute(AttributeKey.stringKey("dsl-key"), "dsl-value")
        }
        span.end()
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].name shouldBeEqualTo "dsl-span"
        finished[0].attributes[AttributeKey.stringKey("dsl-key")] shouldBeEqualTo "dsl-value"
    }

    /**
     * [Task 1.5 회귀] message=null 예외 발생 시 `recordFailure` 가 클래스명 대신 "unspecified error" 를 사용하는지 검증합니다.
     * 이전에는 `error::class.java.simpleName` 을 사용하여 내부 클래스명("RuntimeException")이 노출됐습니다.
     */
    @Test
    fun `recordFailure with null message should use unspecified error fallback not class name`() = runSuspendIO {
        spanExporter.reset()

        val ex = kotlin.runCatching {
            tracer.spanBuilder("null-msg-span").startSpan().use {
                throw RuntimeException(null as String?)
            }
        }.exceptionOrNull()

        ex.shouldNotBeNull()

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1

        val span = finished[0]
        span.status.statusCode shouldBeEqualTo StatusCode.ERROR
        span.status.description shouldBeEqualTo "unspecified error"
        // 내부 클래스명이 누출되지 않음을 검증
        span.status.description.contains("RuntimeException").shouldBeFalse()
    }
}
