package io.bluetape4k.opentelemetry.coroutines

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.opentelemetry.AbstractOtelTest
import io.bluetape4k.opentelemetry.tracer
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletionException
import kotlin.test.assertFailsWith

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
}
