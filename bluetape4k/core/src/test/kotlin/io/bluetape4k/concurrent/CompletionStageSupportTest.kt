package io.bluetape4k.concurrent

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import kotlin.test.assertFailsWith

/**
 * [CompletionStage] 확장 함수를 테스트합니다.
 */
class CompletionStageSupportTest {

    private val success: CompletionStage<Int> = CompletableFuture.completedFuture(42)
    private val failed: CompletionStage<Int> = CompletableFuture.failedFuture(RuntimeException("error"))

    @Test
    fun `getException - 실패 시 예외 반환, 성공 시 IllegalStateException`() {
        val ex = failed.getException()
        ex.shouldNotBeNull(); ex shouldBeInstanceOf RuntimeException::class

        assertFailsWith<IllegalStateException> { success.getException() }
    }

    @Test
    fun `getExceptionOrNull - 상태별 예외 반환`() {
        val ex = failed.getExceptionOrNull()
        ex.shouldNotBeNull(); ex shouldBeInstanceOf RuntimeException::class

        success.getExceptionOrNull().shouldBeNull()
        CompletableFuture<Int>().getExceptionOrNull().shouldBeNull()

        val cancelledEx = CompletableFuture<Int>().apply { cancel(true) }.getExceptionOrNull()
        cancelledEx.shouldNotBeNull(); cancelledEx shouldBeInstanceOf CancellationException::class
    }

    @Test
    fun `sequence - CompletionStage 리스트를 단일 결과로 변환`() {
        val stages: List<CompletionStage<Int>> = listOf(
            CompletableFuture.completedFuture(1),
            CompletableFuture.completedFuture(2),
            CompletableFuture.completedFuture(3),
        )
        stages.sequence().toCompletableFuture().get() shouldBeEqualTo listOf(1, 2, 3)
    }

    @Test
    fun `firstCompleted 와 firstSucceeded`() {
        val failedFirst = failedCompletableFutureOf<Int>(IllegalStateException("boom"))
        val pending = CompletableFuture<Int>()
        val result = listOf<CompletionStage<Int>>(failedFirst, pending).firstCompleted()
        assertFailsWith<java.util.concurrent.ExecutionException> { result.get() }
            .cause shouldBeInstanceOf IllegalStateException::class
        pending.isCancelled.shouldBeTrue()

        val failedStage = failedCompletableFutureOf<Int>(IllegalStateException("boom"))
        val successStage = CompletableFuture.completedFuture(42)
        val pending2 = CompletableFuture<Int>()
        listOf<CompletionStage<Int>>(failedStage, successStage, pending2).firstSucceeded().get() shouldBeEqualTo 42
        pending2.isCancelled.shouldBeTrue()
    }

    @Test
    fun `flatten 과 dereference 는 동일하게 중첩을 풀어낸다`() {
        CompletableFuture.completedFuture(CompletableFuture.completedFuture(42))
            .flatten().toCompletableFuture().get() shouldBeEqualTo 42
        CompletableFuture.completedFuture(CompletableFuture.completedFuture(99))
            .dereference().toCompletableFuture().get() shouldBeEqualTo 99
    }

    @Test
    fun `combineOf - 2개부터 6개까지 CompletionStage 결합`() {
        val a = CompletableFuture.completedFuture(1)
        val b = CompletableFuture.completedFuture("hello")
        val c = CompletableFuture.completedFuture(true)
        val d = CompletableFuture.completedFuture(3.14)
        val e = CompletableFuture.completedFuture(100L)
        val f = CompletableFuture.completedFuture('A')

        combineOf(a, b) { x, y -> "$x-$y" }.toCompletableFuture().get() shouldBeEqualTo "1-hello"
        combineOf(a, b, c) { x, y, z -> "$x-$y-$z" }.toCompletableFuture().get() shouldBeEqualTo "1-hello-true"
        combineOf(a, b, c, d) { x, y, z, w -> "$x-$y-$z-$w" }.toCompletableFuture().get() shouldBeEqualTo "1-hello-true-3.14"
        combineOf(a, b, c, d, e) { v1, v2, v3, v4, v5 ->
            "$v1-$v2-$v3-$v4-$v5"
        }.toCompletableFuture().get() shouldBeEqualTo "1-hello-true-3.14-100"
        combineOf(a, b, c, d, e, f) { v1, v2, v3, v4, v5, v6 ->
            "$v1-$v2-$v3-$v4-$v5-$v6"
        }.toCompletableFuture().get() shouldBeEqualTo "1-hello-true-3.14-100-A"
    }
}
