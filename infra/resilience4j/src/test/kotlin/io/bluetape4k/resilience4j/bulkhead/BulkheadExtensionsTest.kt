package io.bluetape4k.resilience4j.bulkhead

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadFullException
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import io.bluetape4k.assertions.assertFailsWith

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

    @Test
    fun `runnable - 성공 시 실행된다`() {
        val bulkhead = defaultBulkhead()
        var executed = false
        bulkhead.runnable { executed = true }.invoke()
        executed shouldBeEqualTo true
    }

    @Test
    fun `runnable - bulkhead 초과 시 BulkheadFullException 발생한다`() {
        val bulkhead = Bulkhead.of("test-zero-runnable-${System.nanoTime()}") {
            BulkheadConfig.custom()
                .maxConcurrentCalls(0)
                .maxWaitDuration(Duration.ZERO)
                .build()
        }
        assertFailsWith<BulkheadFullException> {
            bulkhead.runnable { }.invoke()
        }
    }

    @Test
    fun `checkedRunnable - 성공 시 실행된다`() {
        val bulkhead = defaultBulkhead()
        var executed = false
        bulkhead.checkedRunnable { executed = true }.run()
        executed shouldBeEqualTo true
    }

    @Test
    fun `callable - 결과를 반환한다`() {
        val bulkhead = defaultBulkhead()
        val result = bulkhead.callable { 42 }.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `supplier - 결과를 반환한다`() {
        val bulkhead = defaultBulkhead()
        val result = bulkhead.supplier { "hello" }.invoke()
        result shouldBeEqualTo "hello"
    }

    @Test
    fun `checkedSupplier - 결과를 반환한다`() {
        val bulkhead = defaultBulkhead()
        val result = bulkhead.checkedSupplier { 42 }.invoke()
        result shouldBeEqualTo 42
    }

    @Test
    fun `consumer - 입력을 처리한다`() {
        val bulkhead = defaultBulkhead()
        var received: String? = null
        bulkhead.consumer<String> { received = it }.invoke("hello")
        received shouldBeEqualTo "hello"
    }

    @Test
    fun `checkedConsumer - 입력을 처리한다`() {
        val bulkhead = defaultBulkhead()
        var received: String? = null
        bulkhead.checkedConsumer<String> { received = it }.accept("hello")
        received shouldBeEqualTo "hello"
    }

    @Test
    fun `function - 결과를 변환한다`() {
        val bulkhead = defaultBulkhead()
        val result = bulkhead.function { input: Int -> input * 2 }.invoke(21)
        result shouldBeEqualTo 42
    }

    @Test
    fun `checkedFunction - 결과를 변환한다`() {
        val bulkhead = defaultBulkhead()
        val result = bulkhead.checkedFunction { input: Int -> input * 2 }.invoke(21)
        result shouldBeEqualTo 42
    }

    @Test
    fun `completionStage - 비동기 결과를 반환한다`() {
        val bulkhead = defaultBulkhead()
        val supplier = bulkhead.completionStage {
            CompletableFuture.supplyAsync { 42 }
        }
        val result = supplier().toCompletableFuture().get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `completableFuture - 비동기 결과를 변환한다`() {
        val bulkhead = defaultBulkhead()
        val func = bulkhead.completableFuture { input: Int ->
            CompletableFuture.supplyAsync { input * 2 }
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }

    @Test
    fun `decorateCompletableFuture - 비동기 결과를 변환한다`() {
        val bulkhead = defaultBulkhead()
        val func = bulkhead.decorateCompletableFuture { input: Int ->
            CompletableFuture.supplyAsync { input * 2 }
        }
        val result = func(21).get()
        result shouldBeEqualTo 42
    }
}
