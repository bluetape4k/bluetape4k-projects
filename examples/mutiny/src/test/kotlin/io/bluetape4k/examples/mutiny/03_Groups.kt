package io.bluetape4k.examples.mutiny

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.mutiny.multiRangeOf
import io.bluetape4k.mutiny.onEach
import io.bluetape4k.mutiny.uniOf
import io.bluetape4k.mutiny.voidUni
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.function.Supplier


class GroupsExamples {

    companion object: KLogging()

    @Test
    fun `01 Uni inspect events`() {
        log.debug { "️⚡️ Uni inspect events" }

        val result = Uni.createFrom().item("Hello")
            .onSubscription().invoke { sub -> log.debug { "onSubscription: $sub" } }
            .onCancellation().invoke { log.debug { "onCancellation" } }
            .onItem().invoke { item -> log.debug { "onItem: $item" } }
            .onFailure().invoke { failure -> log.debug { "onFailure: ${failure?.message}" } }
            .onItemOrFailure().invoke { item, failure -> log.debug { "onItemOrFailure: $item, ${failure?.message}" } }
            .onTermination().invoke { item, failure, complete ->
                log.debug { "onTermination: $item, ${failure?.message}, $complete" }
            }
            .await().atMost(Duration.ofSeconds(5))

        log.debug { "📦 uni result = $result" }
        log.debug { "⚠️ This was a blocking operation" }
    }

    @Test
    fun `02 Uni call()`() {
        log.debug { "️⚡️ Uni call()" }

        val latch = CountDownLatch(1)
        Uni.createFrom().item(123)
            .onItem().call { x -> asyncLog(">>> ", x) }
            .onTermination().call(Supplier { asyncLog("--- ", "") })
            .subscribe().with { x ->
                log.debug { x }
                latch.countDown()
            }

        latch.await()
    }

    private fun asyncLog(prefix: String, value: Any): Uni<Void> {
        val cs = CompletableFuture.runAsync {
            log.debug { "${Thread.currentThread()}::$prefix$value" }
        }
        return Uni.createFrom().completionStage(cs)
    }

    @Test
    fun `03 Uni invoke and call shortcuts`() {
        val result = uniOf(123)
            .invoke { item: Int -> log.debug { "item=$item" } }
            .call { item: Int -> Uni.createFrom().voidItem().onEach { log.debug { "call($item)" } } }
            .eventually { log.debug { "eventually()" } }
            .await().indefinitely()

        result shouldBeEqualTo 123
    }

    @Test
    fun `04 Multi - inspect events`() = runTest {
        val items = multiRangeOf(1, 6)
            .onSubscription().invoke { sub -> log.debug { "onSubscription: $sub" } }
            .onRequest().invoke { count -> log.debug { "onRequest: $count" } }
            .onCancellation().invoke { log.debug { "onCancellation" } }
            .onItem().invoke { item -> log.debug { "onItem: $item" } }
            .onFailure().invoke { failure -> log.error { "onFailure: ${failure?.message}" } }
            .onCompletion().invoke { log.debug { "onCompletion" } }
            .onTermination().invoke { failure, completed -> log.debug { "onTermination: $failure, $completed" } }
            .asFlow().toList()

        log.debug { "multi items: $items" }
        items shouldBeEqualTo listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `05 Multi - invoke and call shortcuts`() {
        multiRangeOf(1, 10)
            .invoke { item -> log.debug { "item=$item" } }
            .call { item ->
                voidUni().invoke { _ -> log.debug { "call($item)" } }
            }
            .subscribe().with { log.debug { "onItem: $it" } }
    }
}
