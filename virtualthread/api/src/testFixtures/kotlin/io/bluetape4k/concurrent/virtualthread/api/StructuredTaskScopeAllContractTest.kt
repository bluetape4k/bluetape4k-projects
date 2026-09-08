package io.bluetape4k.concurrent.virtualthread.api

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** 두 JDK provider에서 실패 handler의 예외 우선순위를 동일하게 검증합니다. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class StructuredTaskScopeAllContractTest {
    protected abstract val provider: StructuredTaskScopeProvider

    @Test
    fun `실패 handler는 원래 작업 예외를 한 번 전달받는다`() {
        verifyHandler(null)
    }

    @Test
    fun `handler 실패는 원래 작업 예외의 suppressed에 보존된다`() {
        verifyHandler(AssertionError("handler failure"))
    }

    @Test
    fun `handler의 취소 예외도 원래 실패를 가리지 않는다`() {
        verifyHandler(CancellationException("handler cancellation"))
    }

    @Test
    fun `handler가 원래 예외를 재전파해도 self suppression이 발생하지 않는다`() {
        verifyHandler(null, rethrowPrimary = true)
    }

    private fun verifyHandler(handlerFailure: Throwable?, rethrowPrimary: Boolean = false) {
        val primary = IllegalStateException("task failure")
        val received = AtomicReference<Throwable>()
        val calls = AtomicInteger()
        val actual = assertFailsWith<IllegalStateException> {
            provider.withAll { scope ->
                scope.fork<Int> { throw primary }
                scope.join().throwIfFailed {
                    received.set(it)
                    calls.incrementAndGet()
                    if (rethrowPrimary) throw it
                    if (handlerFailure != null) throw handlerFailure
                }
            }
        }
        actual shouldBeEqualTo primary
        received.get() shouldBeEqualTo primary
        calls.get() shouldBeEqualTo 1
        actual.suppressed.toList() shouldBeEqualTo listOfNotNull(handlerFailure)
    }
}
