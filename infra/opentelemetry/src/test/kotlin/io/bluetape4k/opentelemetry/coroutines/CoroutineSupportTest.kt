package io.bluetape4k.opentelemetry.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.bluetape4k.opentelemetry.currentOtelContext
import io.bluetape4k.opentelemetry.getSpan
import io.bluetape4k.opentelemetry.getSpanOrNull
import io.bluetape4k.opentelemetry.rootOtelContext
import io.bluetape4k.opentelemetry.tracer
import io.bluetape4k.opentelemetry.withCurrent
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import kotlinx.coroutines.Dispatchers
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletionException
import io.bluetape4k.assertions.assertFailsWith

class CoroutineSupportTest: AbstractOtelTest() {

    companion object: KLoggingChannel()

    private val tracer: Tracer by lazy {
        loggingOtel.tracer("io.bluetape4k.opentelemetry.coroutines.support") {}
    }

    @Test
    fun `await should throw when result fails`() = runSuspendIO {
        val result = CompletableResultCode()
        result.fail()

        assertFailsWith<CompletionException> {
            result.await()
        }
    }

    @Test
    fun `withOtelContext installs provided context in coroutine scope`() = runSuspendIO {
        val span = tracer.spanBuilder("context-span").startSpan()

        span.useSuspending { active ->
            val ctx = active.storeInContext(Context.current())
            withOtelContext(otelContext = ctx) {
                Span.fromContext(Context.current()).spanContext shouldBeEqualTo active.spanContext
            }
        }
    }

    @Test
    fun `Context withOtelContext restores context inside block`() = runSuspendIO {
        val span = tracer.spanBuilder("context-extension-span").startSpan()

        span.useSuspending {
            val ctx = it.storeInContext(Context.current())
            ctx.withOtelContext {
                Span.fromContext(Context.current()).spanContext shouldBeEqualTo it.spanContext
            }
        }
    }

    /**
     * [currentOtelContext]가 현재 OTel Context를 반환하는지 검증합니다.
     * 활성 span이 없을 때 Context.current()와 동일해야 합니다.
     */
    @Test
    fun `currentOtelContext returns current Context`() {
        val ctx = currentOtelContext()
        ctx.shouldNotBeNull()
        // Context.current()와 동일한 인스턴스임을 확인
        ctx shouldBeEqualTo Context.current()
    }

    /**
     * [rootOtelContext]가 루트 Context를 반환하는지 검증합니다.
     * 루트 Context는 항상 고정된 참조 값이어야 합니다.
     */
    @Test
    fun `rootOtelContext returns root Context`() {
        val root = rootOtelContext()
        root.shouldNotBeNull()
        root shouldBeEqualTo Context.root()
    }

    /**
     * [Context.withCurrent]가 scope를 올바르게 설치하고 블록 결과를 반환하는지 검증합니다.
     * scope 닫힘은 use {}에 위임되므로 누수가 없음을 확인합니다.
     */
    @Test
    fun `Context withCurrent executes block and returns result`() {
        val span = tracer.spanBuilder("with-current-span").startSpan()
        val ctx = span.storeInContext(Context.current())

        val result = ctx.withCurrent {
            "block-result"
        }
        span.end()

        result shouldBeEqualTo "block-result"
    }

    /**
     * [Context.getSpan]이 활성 span을 반환하는지 검증합니다.
     * span이 없을 때는 invalid span을 반환합니다.
     */
    @Test
    fun `Context getSpan returns active span or invalid`() {
        val span = tracer.spanBuilder("get-span-test").startSpan()
        val ctx = span.storeInContext(Context.current())

        // span이 있을 때 유효한 span 컨텍스트 반환
        ctx.getSpan().spanContext.isValid.shouldBeTrue()
        span.end()

        // 비어있는 루트 Context에서는 invalid span 반환
        Context.root().getSpan().spanContext.isValid.shouldBeFalse()
    }

    /**
     * [Context.getSpanOrNull]이 span 부재 시 null을 반환하는지 검증합니다.
     * [Context.getSpan]과 달리 null 반환으로 span 존재 여부를 안전하게 확인합니다.
     */
    @Test
    fun `Context getSpanOrNull returns null when no span present`() {
        // 루트 Context에는 span이 없으므로 null 반환
        Context.root().getSpanOrNull().shouldBeNull()

        // span을 주입한 Context에서는 non-null 반환
        val span = tracer.spanBuilder("get-span-or-null-test").startSpan()
        val ctx = span.storeInContext(Context.current())
        ctx.getSpanOrNull().shouldNotBeNull()
        span.end()
    }

    /**
     * [withSpanContext]가 명시적 coroutineContext를 전파하는지 검증합니다.
     * IO Dispatcher 등 비동기 경계를 넘어도 OTel context가 유지되어야 합니다.
     */
    @Test
    fun `withSpanContext with explicit coroutineContext propagates span`() = runSuspendIO {
        val span = tracer.spanBuilder("explicit-ctx-span").startSpan()

        val capturedSpanContext = withSpanContext(span, coroutineContext = Dispatchers.IO) { activeSpan ->
            // IO Dispatcher 경계를 넘어도 span context가 유지됨
            Span.fromContext(Context.current()).spanContext
        }

        // span은 withSpanContext finally 블록에서 이미 종료됨
        capturedSpanContext.isValid.shouldBeTrue()
        capturedSpanContext shouldBeEqualTo span.spanContext
    }
}
