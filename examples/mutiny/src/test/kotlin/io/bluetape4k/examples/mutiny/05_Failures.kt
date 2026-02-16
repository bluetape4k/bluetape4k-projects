package io.bluetape4k.examples.mutiny

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.mutiny.multiOf
import io.bluetape4k.mutiny.uniOf
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.smallrye.mutiny.subscription.MultiEmitter
import io.smallrye.mutiny.subscription.UniEmitter
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainAll
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random
import kotlin.test.assertFailsWith

class FailuresExample {

    companion object: KLoggingChannel()

    @Test
    fun `01 Uni failure transform`() = runTest {
        log.debug { "👀 Uni failure transformation" }

        assertFailsWith<IOException> {
            Uni.createFrom()
                .failure<Int>(RuntimeException("Boom"))
                .onFailure(RuntimeException::class.java).transform(::IOException)
                .log()
                .awaitSuspending()
        }
    }

    @Test
    fun `02 Uni Failure recover with item`() = runTest {
        log.debug { "👀 Uni failure recover with item" }

        val result = Uni.createFrom()
            .failure<String>(IOException("Boom"))
            .onFailure(IOException::class.java).recoverWithItem { e -> e.message }
            .log()
            .awaitSuspending()

        result shouldBeEqualTo "Boom"
    }

    @Test
    fun `03 Uni Failure recover with Uni`() = runTest {
        log.debug { "👀 Uni failure recover with Uni" }

        val result = Uni.createFrom()
            .failure<String>(IOException("Boom"))
            .onFailure(IOException::class.java).recoverWithUni { e -> uniOf("N/A -> ${e.message}") }
            .log()
            .awaitSuspending()

        result shouldBeEqualTo "N/A -> Boom"
    }

    @Test
    fun `04 Uni Failure Retry`() = runTest {
        val result = Uni.createFrom().emitter { emitter -> generateUni(emitter) }
            .onFailure().invoke { _ -> log.debug { "💥" } }
            .onFailure().retry().indefinitely()
            .log()
            .awaitSuspending()
        result shouldBeEqualTo "OK"
    }


    @Test
    fun `05 Uni Failure Retry Bounded`() = runTest {
        val result = Uni.createFrom().emitter { emitter -> generateUni(emitter) }
            .onFailure().invoke { _ -> log.debug { "💥" } }
            .onFailure().retry().atMost(5)
            .log()
            .awaitSuspending()
        result shouldBeEqualTo "OK"
    }

    @Test
    fun `06 Uni Failure Retry with Backoff`() = runTest {
        val result = Uni.createFrom().emitter { emitter -> generateUni(emitter) }
            .onFailure().invoke { _ -> println("💥") }
            .onFailure().retry().withBackOff(Duration.ofMillis(10), Duration.ofMillis(50)).expireIn(1000)
            .log()
            .awaitSuspending()
        result shouldBeEqualTo "OK"
    }

    private fun generateUni(emitter: UniEmitter<in String>) {
        if (Random.nextDouble() > 0.05) {
            emitter.complete("OK")
        } else {
            emitter.fail(RuntimeException("Boom"))
        }
    }


    @Test
    fun `07 Multi Failure recover with completion`() = runTest {
        val isFailed = AtomicBoolean(false)

        val items: List<Int> = Multi.createFrom()
            .emitter { emitter -> generateMulti(emitter) }
            // .onItem().call { _ -> nullUni<Int>().onItem().delayIt().by(Duration.ofMillis(100)) }
            .onItem().invoke { it -> log.debug { it } }
            .onFailure().invoke { _ -> log.debug("💥"); isFailed.set(true) }
            .onFailure().recoverWithCompletion()  // 예외가 발생하면 종료한다
            .onCompletion().invoke { log.debug("✅") }
            .select().first(10)
            .asFlow()
            .toList()

        log.debug { "items=$items" }

        if (!isFailed.get()) {
            items.size shouldBeEqualTo 10
        }
    }

    @Test
    fun `08 Multi Failure recover with item`() = runTest {
        val items: List<Int> = Multi.createFrom()
            .emitter { emitter -> generateMulti(emitter) }
            .onItem().invoke { it -> log.debug { it } }
            .onFailure().invoke { _ -> log.debug { "💥" } }
            .onFailure().recoverWithItem(666)
            .onCompletion().invoke { log.debug { "✅" } }
            .asFlow()
            .toList()

        items shouldContain 666
    }

    @Test
    fun `09 Multi Failure recover with Multi`() = runTest {
        val items: List<Int> = Multi.createFrom()
            .emitter { emitter -> generateMulti(emitter) }
            .onItem().invoke { it -> log.debug { it } }
            .onFailure().invoke { _ -> log.debug { "💥" } }
            .onFailure().recoverWithMulti { _ -> multiOf(666, 999) }
            .onCompletion().invoke { log.debug { "✅" } }
            .log()
            .asFlow()
            .toList()

        items shouldContainAll listOf(666, 999)
    }


    @Test
    fun `10 Multi Failure retry`() {
        log.debug { "👀 Multi faiure retry" }

        Multi.createFrom()
            .emitter { emitter -> generateMulti(emitter) }
            .onFailure().invoke { _ -> log.debug { "💥" } }
            .onFailure().retry().atMost(5)
            .onFailure().recoverWithCompletion()
            .subscribe()
            .with({ log.debug { it } }, Throwable::printStackTrace) { log.debug("✅") }
    }

    @Test
    fun `11 Multi Failure cancelling retry`() {
        log.debug { "👀 Multi faiure retry" }

        Multi.createFrom()
            .emitter { emitter -> generateMulti(emitter) }
            .onFailure().invoke { _ -> log.debug { "💥" } }
            .onFailure().retry().atMost(3)
            .onFailure().recoverWithCompletion()
            .subscribe()
            .with({ log.debug { it } }, Throwable::printStackTrace) { log.debug("✅") }
    }

    private fun generateMulti(emitter: MultiEmitter<in Int>) {
        emitter.emit(Random.nextInt(0, 200))

        while (true) {
            if (Random.nextDouble() > 0.05) {
                emitter.emit(Random.nextInt(0, 200))
            } else {
                // NOTE: 예외를 emit 한 후에는 while loop를 종료해야 합니다.
                emitter.fail(IOException("Boom"))
                return
            }
        }
    }


    @Test
    fun `12 Multi Failure guarded recovery`() = runTest {
        log.debug { "👀 Multi faiure guarded recovery" }

        val items = Multi.createFrom().range(0, 10)
            .onItem().transformToUniAndConcatenate { safeGuardedOperation(it) }
            .onFailure().recoverWithItem(666)
            .onCompletion().invoke { log.debug { "✅" } }
            .collect()
            .asList()
            .awaitSuspending()

        items shouldBeEqualTo (0..9).toList()
    }

    private fun safeGuardedOperation(i: Int): Uni<Int> {
        return Uni.createFrom().item(i)
            .onItem().invoke { n ->
                if (n == 6) {
                    log.warn { "$n recover to $i" }
                    throw RuntimeException("Boom!")
                } else {
                    log.debug { "👍 $i" }
                }
            }
            .onFailure().recoverWithItem(i)
    }
}
