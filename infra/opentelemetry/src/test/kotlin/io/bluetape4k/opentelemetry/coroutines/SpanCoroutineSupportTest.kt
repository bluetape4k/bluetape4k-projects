package io.bluetape4k.opentelemetry.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.bluetape4k.opentelemetry.trace.export
import io.bluetape4k.opentelemetry.trace.loggingSpanExporterOf
import io.bluetape4k.opentelemetry.trace.sdkTracerProvider
import io.bluetape4k.opentelemetry.trace.simpleSpanProcessorOf
import io.bluetape4k.opentelemetry.trace.spanExportOf
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldHaveSize
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * NOTE: 테스트 시에 java agent 를 사용하면서 SdkTraceProvider 를 통해 tracer 를 얻으면 충돌이 납니다.
 *
 * 추천하는 운영 구성(한 줄)
 *
 * - 운영/통합환경: agent ON + GlobalOpenTelemetry 사용(앱에서 SDK 만들지 않기)
 * - 단위테스트: agent OFF + InMemorySpanExporter로 parentSpanId까지 정밀 assert
 * - 통합테스트: agent ON + logging/otlp exporter로 “트레이스가 붙는지”만 확인(정밀 assert는 보통 어려움)
 */
class SpanCoroutineSupportTest: AbstractOtelTest() {

    companion object: KLoggingChannel()

    private val spanExporter = InMemorySpanExporter.create()
    private val tracerProvider = sdkTracerProvider {
        addSpanProcessor(simpleSpanProcessorOf(spanExporter))
    }
    private val tracer = tracerProvider.get("io.bluetape4k.otel.coroutines")

    private fun flush() {
        tracerProvider.forceFlush().join(1, TimeUnit.SECONDS)
    }

    @Test
    fun `await should await completable result code`() = runSuspendIO {
        val resultCode = CompletableResultCode()

        resultCode.succeed()
        resultCode.await()
        resultCode.isSuccess.shouldBeTrue()
    }

    @Test
    fun `useSpanSuspending should create nested spans by explicit parent context`() = runSuspendIO {
        spanExporter.reset()

        tracer.spanBuilder("parent-span").useSpanSuspending { parentSpan ->
            parentSpan.setAttribute(AttributeKey.stringKey("parent-attribute"), "parent-value")

            val parentCtx = parentSpan.storeInContext(Context.current())

            log.debug { "parent span context: ${parentSpan.spanContext}" }
            log.debug { "parentCtx class: ${parentCtx::class.qualifiedName}" }
            log.debug { "parent span(from parentCtx) context: ${Span.fromContext(parentCtx).spanContext}" }

            tracer.spanBuilder("child-span")
                .setParent(parentCtx)
                .useSpanSuspending { childSpan ->
                    childSpan.setAttribute(AttributeKey.stringKey("child-attribute"), "child-value")
                    log.debug { "child span context: ${childSpan.spanContext}" }
                }
        }

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 2

        val parent = finished.find { it.name == "parent-span" }
        parent.shouldNotBeNull()

        val child = finished.find { it.name == "child-span" }
        child.shouldNotBeNull()

        // Same trace + correct parent/child relationship
        child.traceId shouldBeEqualTo parent.traceId
        child.parentSpanId shouldBeEqualTo parent.spanId

        parent.attributes[AttributeKey.stringKey("parent-attribute")] shouldBeEqualTo "parent-value"
        child.attributes[AttributeKey.stringKey("child-attribute")] shouldBeEqualTo "child-value"
    }

    @Test
    fun `useSuspending should end span and set ERROR status on exception`() = runSuspendIO {
        spanExporter.reset()

        val ex = kotlin.runCatching {
            tracer.spanBuilder("error-span").startSpan().useSuspending {
                it.setAttribute(AttributeKey.stringKey("before"), "true")
                error("boom")
            }
        }.exceptionOrNull()

        ex.shouldNotBeNull()

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1

        val span = finished[0]
        span.name shouldBeEqualTo "error-span"
        // StatusCode enum name: "ERROR" (SpanData.status.statusCode.name)
        span.status.statusCode.name shouldBeEqualTo "ERROR"
    }

    private class RecordingSpanExporter: SpanExporter {
        private val exportCalls = AtomicInteger(0)
        val exported = mutableListOf<SpanData>()

        override fun export(spans: Collection<SpanData>): CompletableResultCode {
            exportCalls.incrementAndGet()
            exported.addAll(spans)
            return CompletableResultCode.ofSuccess()
        }

        override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()
        override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()

        fun calledCount(): Int = exportCalls.get()
        fun exportedCount(): Int = exported.size
    }

    @Test
    fun `loggingSpanExporterOf should create exporter`() {
        loggingSpanExporterOf().shouldNotBeNull()
    }

    @Test
    fun `spanExportOf should delegate export to all exporters`() = runSuspendIO {
        spanExporter.reset()

        val exporter1 = RecordingSpanExporter()
        val exporter2 = RecordingSpanExporter()
        val composite = spanExportOf(exporter1, exporter2)

        tracer.spanBuilder("export-parent").useSpanSuspending {
            tracer.spanBuilder("export-child").useSpanSuspending { child ->
                child.setAttribute(AttributeKey.stringKey("k"), "v")
            }
        }

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 2

        // SpanExporter.export(vararg SpanData) extension coverage
        val result = composite.export(*finished.toTypedArray())
        result.isSuccess.shouldBeTrue()

        exporter1.calledCount() shouldBeEqualTo 1
        exporter2.calledCount() shouldBeEqualTo 1
        exporter1.exportedCount() shouldBeEqualTo 2
        exporter2.exportedCount() shouldBeEqualTo 2

        exporter1.exported.map { it.name }.sorted() shouldBeEqualTo listOf("export-child", "export-parent")
        exporter2.exported.map { it.name }.sorted() shouldBeEqualTo listOf("export-child", "export-parent")
    }
}
