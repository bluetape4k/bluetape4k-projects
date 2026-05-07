package io.bluetape4k.opentelemetry.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.bluetape4k.opentelemetry.trace.sdkTracerProvider
import io.bluetape4k.opentelemetry.trace.simpleSpanProcessorOf
import io.bluetape4k.opentelemetry.trace.withSpan
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith
import java.util.concurrent.TimeUnit

/**
 * [io.opentelemetry.api.trace.Tracer.withSpan] suspend / blocking 두 오버로드를 검증합니다.
 */
class TracerWithSpanTest: AbstractOtelTest() {

    companion object: KLoggingChannel()

    private val spanExporter = InMemorySpanExporter.create()
    private val tracerProvider = sdkTracerProvider {
        addSpanProcessor(simpleSpanProcessorOf(spanExporter))
    }
    private val tracer = tracerProvider.get("io.bluetape4k.otel.tracer-with-span")

    @BeforeEach
    fun setup() {
        spanExporter.reset()
    }

    private fun flush() {
        tracerProvider.forceFlush().join(1, TimeUnit.SECONDS)
    }

    // ── suspend Tracer.withSpan ───────────────────────────────────────────────

    @Test
    fun `suspend withSpan should end span with matching name`() = runSuspendIO {
        val result = tracer.withSpan("my-op") { "done" }
        result shouldBeEqualTo "done"
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].name shouldBeEqualTo "my-op"
    }

    @Test
    fun `suspend withSpan should propagate context into withContext IO dispatcher`() = runSuspendIO {
        tracer.withSpan("io-span") {
            withContext(Dispatchers.IO) {
                // Dispatcher 전환 후에도 1개의 Span만 생성됨
            }
        }
        flush()

        spanExporter.finishedSpanItems shouldHaveSize 1
        spanExporter.finishedSpanItems[0].name shouldBeEqualTo "io-span"
    }

    @Test
    fun `suspend withSpan nested calls should produce parent-child trace`() = runSuspendIO {
        tracer.withSpan("parent") {
            tracer.withSpan("child") { }
        }
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 2

        val parent = finished.find { it.name == "parent" }.shouldNotBeNull()
        val child = finished.find { it.name == "child" }.shouldNotBeNull()

        child.traceId shouldBeEqualTo parent.traceId
        child.parentSpanId shouldBeEqualTo parent.spanId
    }

    @Test
    fun `suspend withSpan should set ERROR and rethrow on exception`() = runSuspendIO {
        val failure = IllegalStateException("boom")

        val ex = kotlin.runCatching {
            tracer.withSpan("err-span") { throw failure }
        }.exceptionOrNull()

        ex.shouldNotBeNull()
        (ex is IllegalStateException).shouldBeTrue()

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].status.statusCode shouldBeEqualTo StatusCode.ERROR
        finished[0].events.any { it.name == "exception" }.shouldBeTrue()
    }

    @Test
    fun `suspend withSpan should NOT record ERROR on CancellationException`() = runSuspendIO {
        val ex = kotlin.runCatching {
            tracer.withSpan("cancel-span") { throw CancellationException("cancelled") }
        }.exceptionOrNull()

        ex.shouldNotBeNull()
        (ex is CancellationException).shouldBeTrue()

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].status.statusCode shouldBeEqualTo StatusCode.UNSET
        finished[0].events.any { it.name == "exception" }.shouldBeFalse()
    }

    @Test
    fun `suspend withSpan configure lambda should apply attributes`() = runSuspendIO {
        tracer.withSpan("attr-span", configure = {
            setAttribute(AttributeKey.stringKey("env"), "test")
        }) { }
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].attributes[AttributeKey.stringKey("env")] shouldBeEqualTo "test"
    }

    @Test
    fun `suspend withSpan with blank spanName should throw IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            runSuspendIO {
                tracer.withSpan("  ") { }
            }
        }
    }

    // ── blocking Tracer.withSpan ──────────────────────────────────────────────

    @Test
    fun `blocking withSpan should set status OK on normal completion`() = runSuspendIO {
        val result = tracer.withSpan("block-ok") { "result" }
        result shouldBeEqualTo "result"
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].name shouldBeEqualTo "block-ok"
        finished[0].status.statusCode shouldBeEqualTo StatusCode.OK
    }

    @Test
    fun `blocking withSpan with null message should use unspecified error fallback`() = runSuspendIO {
        val ex = kotlin.runCatching {
            tracer.withSpan("block-null-msg") {
                throw RuntimeException(null as String?)
            }
        }.exceptionOrNull()

        ex.shouldNotBeNull()

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].status.statusCode shouldBeEqualTo StatusCode.ERROR
        // javaClass.simpleName("RuntimeException") 노출 없이 "unspecified error" 사용
        finished[0].status.description shouldBeEqualTo "unspecified error"
        finished[0].status.description.contains("RuntimeException").shouldBeFalse()
    }

    @Test
    fun `blocking withSpan should NOT record ERROR on CancellationException`() = runSuspendIO {
        val ex = kotlin.runCatching {
            tracer.withSpan("block-cancel") {
                throw CancellationException("cancelled")
            }
        }.exceptionOrNull()

        ex.shouldNotBeNull()
        (ex is CancellationException).shouldBeTrue()

        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].status.statusCode shouldBeEqualTo StatusCode.UNSET
        finished[0].events.any { it.name == "exception" }.shouldBeFalse()
    }

    @Test
    fun `blocking withSpan nested should produce parent-child trace`() = runSuspendIO {
        tracer.withSpan("block-parent") {
            tracer.withSpan("block-child") { }
        }
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 2

        val parent = finished.find { it.name == "block-parent" }.shouldNotBeNull()
        val child = finished.find { it.name == "block-child" }.shouldNotBeNull()

        child.traceId shouldBeEqualTo parent.traceId
        child.parentSpanId shouldBeEqualTo parent.spanId
    }

    @Test
    fun `blocking withSpan with blank spanName should throw IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            tracer.withSpan("") { }
        }
    }
}
