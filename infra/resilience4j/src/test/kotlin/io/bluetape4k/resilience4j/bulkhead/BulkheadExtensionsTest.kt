package io.bluetape4k.resilience4j.bulkhead

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class BulkheadExtensionsTest {

    companion object: KLoggingChannel()

    private fun defaultBulkhead() = Bulkhead.ofDefaults("test-${System.nanoTime()}")

    private fun fullBulkhead() = Bulkhead.of("test-full-${System.nanoTime()}") {
        BulkheadConfig.custom()
            .maxConcurrentCalls(1)
            .maxWaitDuration(Duration.ZERO)
            .build()
    }

    @Test
    fun `withBulkhead - 성공하는 함수가 정상 실행된다`() = runSuspendTest {
        val bulkhead = defaultBulkhead()
        val result = withBulkhead(bulkhead) { "hello" }

        result shouldBeEqualTo "hello"
        bulkhead.metrics.availableConcurrentCalls shouldBeEqualTo bulkhead.bulkheadConfig.maxConcurrentCalls
    }

    @Test
    fun `withBulkhead - 1개 파라미터 함수에 적용한다`() = runSuspendTest {
        val bulkhead = defaultBulkhead()
        val result = withBulkhead(bulkhead, 21) { input -> input * 2 }

        result shouldBeEqualTo 42
    }

    @Test
    fun `withBulkhead - 2개 파라미터 함수에 적용한다`() = runSuspendTest {
        val bulkhead = defaultBulkhead()
        val result = withBulkhead(bulkhead, 20, 22) { a, b -> a + b }

        result shouldBeEqualTo 42
    }

    @Test
    fun `withBulkhead - bulkhead 초과 시 BulkheadFullException 발생한다`() = runSuspendTest {
        // maxConcurrentCalls=0 설정으로 즉시 차단
        val bulkhead = Bulkhead.of("test-zero") {
            BulkheadConfig.custom()
                .maxConcurrentCalls(0)
                .maxWaitDuration(Duration.ZERO)
                .build()
        }

        assertFailsWith<BulkheadFullException> {
            withBulkhead(bulkhead) { "should not run" }
        }
    }

    @Test
    fun `decorateSuspendFunction1 - 정상 실행된다`() = runSuspendTest {
        val bulkhead = defaultBulkhead()
        val decorated = bulkhead.decorateSuspendFunction1 { input: String ->
            "Hello, $input!"
        }

        decorated("world") shouldBeEqualTo "Hello, world!"
    }

    @Test
    fun `decorateSuspendBiFunction - 정상 실행된다`() = runSuspendTest {
        val bulkhead = defaultBulkhead()
        val decorated = bulkhead.decorateSuspendBiFunction { a: Int, b: Int ->
            a + b
        }

        decorated(20, 22) shouldBeEqualTo 42
    }
}
