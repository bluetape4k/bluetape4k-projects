package io.bluetape4k.mutiny

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

class UniSupportTest {

    companion object: KLoggingChannel()

    @Test
    fun `uniOf by value`() {
        val uni = uniOf(42)
        uni.await().indefinitely() shouldBeEqualTo 42
        uni.await().indefinitely() shouldBeEqualTo 42
    }

    @Test
    fun `uni by supplier`() {
        val uni = uniOf { Random.nextInt() }
        val items = List(5) { uni.await().indefinitely() }

        items.toSet() shouldHaveSize 5
        log.debug { "Items=${items.joinToString()}" }
    }

    @Test
    fun `onEach with callback`() {
        uniOf { Random.nextInt() }
            .onEach {
                log.debug { "onEach: $it" }
            }
            .subscribe()
            .with {
                log.debug { "subscribe: $it" }
            }
    }

    @Test
    fun `convert CompletionStage to Uni`() {
        val uni = CompletableFuture.supplyAsync { 42 }.asUni<Int>()
        uni.await().indefinitely() shouldBeEqualTo 42
    }

    @Test
    fun `convert uni to uni`() {
        uniConvertOf(10) { uniOf { "[$it]" } }
            .subscribe()
            .with(::println) { failure -> println(failure.message) }
    }

    @Test
    fun `voidUni는 null을 방출한다`() {
        val result = voidUni().await().indefinitely()
        result.shouldBeNull()
    }

    @Test
    fun `nullUni는 null을 방출한다`() {
        val result = nullUni<String>().await().indefinitely()
        result.shouldBeNull()
    }

    @Test
    fun `uniFailureOf는 실패 Uni를 생성한다`() {
        val error = IllegalStateException("boom")
        val uni = uniFailureOf<Int>(error)

        assertFailsWith<IllegalStateException> {
            uni.await().indefinitely()
        }
    }

    @Test
    fun `uniFailureOf supplier는 실패 Uni를 생성한다`() {
        val uni = uniFailureOf<Int> { IllegalArgumentException("invalid") }

        assertFailsWith<IllegalArgumentException> {
            uni.await().indefinitely()
        }
    }

    @Test
    fun `Future를 Uni로 변환한다`() {
        val future = CompletableFuture.completedFuture(99)
        val result = future.asUni(java.time.Duration.ofSeconds(1)).await().indefinitely()
        result shouldBeEqualTo 99
    }

    @Test
    fun `uniOf with state와 mapper`() {
        val uni = uniOf(10) { "[$it]" }
        val result = uni.await().indefinitely()
        result shouldBeEqualTo "[10]"
    }

    @Test
    fun `uniCompletionStageOf supplier`() {
        val uni = uniCompletionStageOf { CompletableFuture.completedFuture("ok") }
        val result = uni.await().indefinitely()
        result shouldBeEqualTo "ok"
    }
}
