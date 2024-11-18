package io.bluetape4k.mutiny

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture
import kotlin.random.Random

class UniSupportTest {

    companion object: KLogging()

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
            .subscribe().with(::println) { failure -> println(failure.message) }
    }
}
