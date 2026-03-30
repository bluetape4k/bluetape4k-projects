package io.bluetape4k.resilience4j

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

class CompletionStageSupportTest {

    @Test
    fun `recover with exceptionTypes 는 CompletionException cause 가 null 이어도 NPE 를 내지 않는다`() {
        val future = CompletableFuture<Int>()
        future.completeExceptionally(CompletionException(null))

        val recovered = future.recover(listOf(IOException::class.java)) { 42 }

        val failure = runCatching { recovered.toCompletableFuture().join() }

        failure.isFailure.shouldBeTrue()
        failure.exceptionOrNull() shouldBeInstanceOf CompletionException::class
    }

    @Test
    fun `supplier recover 는 stage 생성 전 동기 예외도 복구한다`() {
        val supplier: () -> CompletableFuture<Int> = {
            throw IOException("boom")
        }

        val recovered = supplier.recover(IOException::class) { -1 }

        recovered().toCompletableFuture().join() shouldBeEqualTo -1
    }

    @Test
    fun `supplier recover with exceptionTypes 는 stage 생성 전 동기 예외 타입을 보존한다`() {
        val supplier: () -> CompletableFuture<Int> = {
            throw IllegalStateException("boom")
        }

        val recovered = supplier.recover(listOf(IOException::class.java)) { -1 }

        val failure = runCatching { recovered().toCompletableFuture().join() }

        failure.isFailure.shouldBeTrue()
        failure.exceptionOrNull() shouldBeInstanceOf CompletionException::class
        failure.exceptionOrNull()?.cause shouldBeInstanceOf IllegalStateException::class
        failure.exceptionOrNull()?.cause?.message shouldBeEqualTo "boom"
    }
}
