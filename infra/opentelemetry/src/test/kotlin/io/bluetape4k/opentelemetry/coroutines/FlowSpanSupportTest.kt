package io.bluetape4k.opentelemetry.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.bluetape4k.opentelemetry.trace.sdkTracerProvider
import io.bluetape4k.opentelemetry.trace.simpleSpanProcessorOf
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeout
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

/**
 * [Flow.traced] 및 [Flow.tracedCollect] 동작을 검증합니다.
 *
 * 핵심 계약:
 * - 1 collect = 1 Span (emit 횟수와 무관)
 * - CancellationException → UNSET (ERROR 미기록)
 * - 일반 예외 → ERROR + recordException
 */
class FlowSpanSupportTest: AbstractOtelTest() {

    companion object: KLoggingChannel()

    private val spanExporter = InMemorySpanExporter.create()
    private val tracerProvider = sdkTracerProvider {
        addSpanProcessor(simpleSpanProcessorOf(spanExporter))
    }
    private val tracer = tracerProvider.get("io.bluetape4k.otel.flow-span")

    @BeforeEach
    fun setup() {
        spanExporter.reset()
    }

    private fun flush() {
        tracerProvider.forceFlush().join(1, TimeUnit.SECONDS)
    }

    @Test
    fun `traced normal completion should produce exactly 1 span with OK status`() = runSuspendIO {
        flowOf(1, 2, 3)
            .traced(tracer, "flow-ok")
            .collect { }
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].name shouldBeEqualTo "flow-ok"
        finished[0].status.statusCode shouldBeEqualTo StatusCode.OK
    }

    @Test
    fun `traced with 3 emits should still produce exactly 1 span`() = runSuspendIO {
        val items = flowOf("a", "b", "c")
            .traced(tracer, "multi-emit")
            .toList()
        flush()

        items shouldHaveSize 3
        spanExporter.finishedSpanItems shouldHaveSize 1
    }

    @Test
    fun `traced upstream exception should set ERROR and rethrow`() = runSuspendIO {
        val upstream = flow<Int> { throw IllegalStateException("upstream-fail") }

        val ex = kotlin.runCatching {
            upstream.traced(tracer, "err-flow").collect { }
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
    fun `traced take(2) downstream early completion should produce OK span`() = runSuspendIO {
        flowOf(1, 2, 3, 4)
            .traced(tracer, "take-flow")
            .take(2)
            .collect { }
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        // channelFlow 의 버퍼로 인해 producer(upstream collect)가 take(2) 취소 전에 완료 → OK
        finished[0].status.statusCode shouldBeEqualTo StatusCode.OK
    }

    @Test
    fun `traced withTimeout cancellation should set UNSET status not ERROR`() = runSuspendIO {
        val ex = kotlin.runCatching {
            withTimeout(50.milliseconds) {
                flow<Int> {
                    emit(1)
                    kotlinx.coroutines.delay(1_000)
                    emit(2)
                }.traced(tracer, "timeout-flow").collect { }
            }
        }.exceptionOrNull()

        ex.shouldNotBeNull()
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].status.statusCode shouldBeEqualTo StatusCode.UNSET
        finished[0].events.any { it.name == "exception" }.shouldBeFalse()
    }

    @Test
    fun `traced with flowOn IO dispatcher should propagate span and produce 1 span`() = runSuspendIO {
        flowOf(1, 2, 3)
            .flowOn(Dispatchers.IO)
            .traced(tracer, "flowon-span")
            .collect { }
        flush()

        spanExporter.finishedSpanItems shouldHaveSize 1
        spanExporter.finishedSpanItems[0].name shouldBeEqualTo "flowon-span"
    }

    @Test
    fun `tracedCollect action should run inside traced Span context`() = runSuspendIO {
        var spanInsideAction: Span? = null

        flowOf(42).tracedCollect(tracer, "collect-span") { _ ->
            spanInsideAction = Span.current()
        }
        flush()

        spanInsideAction.shouldNotBeNull()
        // collect 안에서의 Span이 종료된 traced Span과 동일한 traceId
        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        finished[0].traceId shouldBeEqualTo spanInsideAction!!.spanContext.traceId
    }

    @Test
    fun `traced should not capture Authorization header as span attribute`() = runSuspendIO {
        flowOf("data")
            .traced(tracer, "secure-flow") {
                // Authorization 헤더를 attribute로 추가하지 않음 — 이 테스트는 configure에서 민감 attribute를 설정하지 않는 경우를 검증
                setAttribute(AttributeKey.stringKey("safe-key"), "safe-value")
            }
            .collect { }
        flush()

        val finished = spanExporter.finishedSpanItems
        finished shouldHaveSize 1
        // Authorization attribute가 없음을 검증
        finished[0].attributes.asMap().keys
            .none { it.key.contains("Authorization", ignoreCase = true) }
            .shouldBeTrue()
    }

    @Test
    fun `traced with blank spanName should throw IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            runSuspendIO {
                flowOf(1).traced(tracer, "").collect { }
            }
        }
    }

    @Test
    fun `tracedCollect with blank spanName should throw IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            runSuspendIO {
                flowOf(1).tracedCollect(tracer, "  ") { }
            }
        }
    }
}
