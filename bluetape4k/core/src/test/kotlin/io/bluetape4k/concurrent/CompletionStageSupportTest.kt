package io.bluetape4k.concurrent

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import kotlin.test.assertFailsWith

/**
 * [CompletionStage] 확장 함수를 테스트합니다.
 */
class CompletionStageSupportTest {

    private val success: CompletionStage<Int> = CompletableFuture.completedFuture(42)
    private val failed: CompletionStage<Int> = CompletableFuture.failedFuture(RuntimeException("error"))

    @Nested
    inner class GetExceptionTest {
        @Test
        fun `실패한 CompletionStage의 예외를 가져온다`() {
            val ex = failed.getException()
            ex.shouldNotBeNull()
            ex shouldBeInstanceOf RuntimeException::class
        }

        @Test
        fun `성공한 CompletionStage에서 getException 호출 시 IllegalStateException이 발생한다`() {
            assertFailsWith<IllegalStateException> {
                success.getException()
            }
        }
    }

    @Nested
    inner class GetExceptionOrNullTest {
        @Test
        fun `실패한 CompletionStage의 예외를 가져온다`() {
            val ex = failed.getExceptionOrNull()
            ex.shouldNotBeNull()
            ex shouldBeInstanceOf RuntimeException::class
        }

        @Test
        fun `성공한 CompletionStage에서 getExceptionOrNull 호출 시 null을 반환한다`() {
            success.getExceptionOrNull().shouldBeNull()
        }

        @Test
        fun `아직 완료되지 않은 CompletionStage에서 getExceptionOrNull 호출 시 null을 반환한다`() {
            val pending: CompletionStage<Int> = CompletableFuture<Int>()
            pending.getExceptionOrNull().shouldBeNull()
        }
    }

    @Nested
    inner class SequenceTest {
        @Test
        fun `CompletionStage 리스트를 sequence로 변환한다`() {
            val stages: List<CompletionStage<Int>> = listOf(
                CompletableFuture.completedFuture(1),
                CompletableFuture.completedFuture(2),
                CompletableFuture.completedFuture(3),
            )
            val result = stages.sequence().toCompletableFuture().get()
            result shouldBeEqualTo listOf(1, 2, 3)
        }
    }

    @Nested
    inner class FlattenAndDereferenceTest {
        @Test
        fun `CompletionStage flatten으로 중첩을 풀 수 있다`() {
            val nested: CompletionStage<CompletionStage<Int>> =
                CompletableFuture.completedFuture(CompletableFuture.completedFuture(42))

            val result = nested.flatten().toCompletableFuture().get()
            result shouldBeEqualTo 42
        }

        @Test
        fun `dereference는 flatten과 동일하게 동작한다`() {
            val nested: CompletionStage<CompletionStage<Int>> =
                CompletableFuture.completedFuture(CompletableFuture.completedFuture(99))

            val result = nested.dereference().toCompletableFuture().get()
            result shouldBeEqualTo 99
        }
    }

    @Nested
    inner class CombineOfTest {
        @Test
        fun `두 개의 CompletionStage를 결합한다`() {
            val a = CompletableFuture.completedFuture(1)
            val b = CompletableFuture.completedFuture("hello")

            val result = combineOf(a, b) { x, y -> "$x-$y" }.toCompletableFuture().get()
            result shouldBeEqualTo "1-hello"
        }

        @Test
        fun `세 개의 CompletionStage를 결합한다`() {
            val a = CompletableFuture.completedFuture(1)
            val b = CompletableFuture.completedFuture("hello")
            val c = CompletableFuture.completedFuture(true)

            val result = combineOf(a, b, c) { x, y, z -> "$x-$y-$z" }.toCompletableFuture().get()
            result shouldBeEqualTo "1-hello-true"
        }

        @Test
        fun `네 개의 CompletionStage를 결합한다`() {
            val a = CompletableFuture.completedFuture(1)
            val b = CompletableFuture.completedFuture("hello")
            val c = CompletableFuture.completedFuture(true)
            val d = CompletableFuture.completedFuture(3.14)

            val result = combineOf(a, b, c, d) { x, y, z, w -> "$x-$y-$z-$w" }.toCompletableFuture().get()
            result shouldBeEqualTo "1-hello-true-3.14"
        }

        @Test
        fun `다섯 개의 CompletionStage를 결합한다`() {
            val a = CompletableFuture.completedFuture(1)
            val b = CompletableFuture.completedFuture("hello")
            val c = CompletableFuture.completedFuture(true)
            val d = CompletableFuture.completedFuture(3.14)
            val e = CompletableFuture.completedFuture(100L)

            val result = combineOf(a, b, c, d, e) { v1, v2, v3, v4, v5 ->
                "$v1-$v2-$v3-$v4-$v5"
            }.toCompletableFuture().get()
            result shouldBeEqualTo "1-hello-true-3.14-100"
        }

        @Test
        fun `여섯 개의 CompletionStage를 결합한다`() {
            val a = CompletableFuture.completedFuture(1)
            val b = CompletableFuture.completedFuture("hello")
            val c = CompletableFuture.completedFuture(true)
            val d = CompletableFuture.completedFuture(3.14)
            val e = CompletableFuture.completedFuture(100L)
            val f = CompletableFuture.completedFuture('A')

            val result = combineOf(a, b, c, d, e, f) { v1, v2, v3, v4, v5, v6 ->
                "$v1-$v2-$v3-$v4-$v5-$v6"
            }.toCompletableFuture().get()
            result shouldBeEqualTo "1-hello-true-3.14-100-A"
        }
    }
}
