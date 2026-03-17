package io.bluetape4k.opentelemetry.trace

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import kotlinx.coroutines.CancellationException
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
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
}
