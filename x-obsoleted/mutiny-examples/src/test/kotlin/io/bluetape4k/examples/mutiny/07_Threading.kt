package io.bluetape4k.examples.mutiny

import io.bluetape4k.concurrent.NamedThreadFactory
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.mutiny.asUni
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.smallrye.mutiny.infrastructure.Infrastructure
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.function.Supplier
import java.util.stream.Collectors
import kotlin.concurrent.thread
import kotlin.random.Random

class ThreadingExamples {

    companion object: KLoggingChannel()

    private val executor = Executors.newFixedThreadPool(8, NamedThreadFactory("mutiny"))

    private val counter = atomic(0)
    private var count by counter

    @BeforeEach
    fun setup() {
        count = 0
    }

    @Test
    fun `01 Threading runSubscriptionOn`() {
        log.debug { "👀 runSubscriptionOn (do not block the subscriber)" }

        // runSubscriptonOn 은 onSubscribe() 메소드 내에서 (upstream에 item을 요청할 때) 사용할 Thread pool 을 지정합니다.
        Multi.createBy()
            .repeating().uni(Supplier { generate() }).atMost(10) // Supplier 이여만 emit 된다.
            .runSubscriptionOn(executor)
            .subscribe().with { log.debug { it } }

        await until { count >= 10 }
    }

    @Test
    fun `02 Thread emitOn`() {
        log.debug { "👀 emitOn (dispatch blocking event processing)" }

        // emitOn 은 subscribe 작업 시 (onItem, onFailure, onComple) 사용할 Thread Pool을 지정합니다.
        Multi.createBy()
            .repeating().uni(Supplier { generate() }).atMost(10) // Supplier 이여만 emit 된다.
            .emitOn(executor)
            .subscribe().with { log.debug { it } }

        await until { count >= 10 }
    }

    private fun generate(): Uni<Int> = Uni.createFrom().completionStage {
        CompletableFuture.supplyAsync(
            {
                log.debug { "Produce counter." }
                counter.getAndIncrement()
            },
            CompletableFuture.delayedExecutor(Random.nextLong(100), TimeUnit.MILLISECONDS)
        )
    }

    @Test
    fun `03 Infra Executor`() {
        log.debug { "👀 emitOn (dispatch blocking event processing to the Mutiny default worker pool)" }

        Multi.createBy()
            .repeating().uni(Supplier { generateInWorkerPool() }).atMost(10) // Supplier 이여만 emit 된다.
            .emitOn(executor)
            .subscribe().with { log.debug { it } }

        await until { count >= 10 }
    }

    private fun generateInWorkerPool(): Uni<Int> =
        CompletableFuture.supplyAsync(
            {
                log.debug { "Produce counter." }
                counter.getAndIncrement()
            },
            Infrastructure.getDefaultExecutor()
        ).asUni()

    @Test
    fun `04 Threading Blocking`() {
        log.debug { "👀 Blocking" }

        val iterable = Multi.createFrom().range(0, 10)
            .subscribe().asIterable()

        val list = iterable.toList()
        list shouldBeEqualTo (0..9).toList()

        val sequence = Multi.createFrom().range(0, 10)
            .subscribe().asIterable().asSequence()

        sequence.toList() shouldBeEqualTo (0..9).toList()

        val someInt = Uni.createFrom().item(42).await().indefinitely()
        someInt shouldBeEqualTo 42
    }

    @OptIn(DelicateCoroutinesApi::class)
    @Test
    fun `04-1 Suspending`() = runTest {
        log.debug { "👀 Coroutines" }

        val flow = Multi.createFrom().range(0, 10).asFlow()
        flow.toList() shouldBeEqualTo (0..9).toList()

        val dispatcher = newFixedThreadPoolContext(2, "user")
        Multi.createFrom().range(0, 10)
            .asFlow()
            .map {
                log.debug { "mapping $it" }
                it * 2
            }
            .flowOn(dispatcher)
            .collect {
                log.debug { "collect item=$it" }
            }

        Uni.createFrom().item(42).awaitSuspending() shouldBeEqualTo 42
    }

    @Test
    fun `05 Threading blocking check`() {
        log.debug { "👀 Blocking" }

        // Blocking 작업이 실행 될 수 있는 Thread에 대한 조건을 설정합니다.
        Infrastructure.setCanCallerThreadBeBlockedSupplier {
            // Thread name이 "yolo" 를 포함하면 blocking 코드를 포함할 수 없다
            !Thread.currentThread().name.contains("yolo")
        }

        try {
            thread(name = "yolo-1") {
                val iterable = Multi.createFrom().range(0, 10).subscribe().asIterable()
                // stream() 함수가 내부에서 Infrastructure 를 사용한다. Kotlin toList() 사용 시에는 검출되지 않는다.
                val list = iterable.stream().collect(Collectors.toList())
                list shouldBeEqualTo (0..9).toList()
            }
            Thread.sleep(100)

            thread(name = "yolo-2") {
                val someInt = Uni.createFrom().item(42).await().atMost(Duration.ofSeconds(3))
                someInt shouldBeEqualTo 42
            }
            Thread.sleep(100)
        } finally {
            Infrastructure.resetCanCallerThreadBeBlockedSupplier()
        }
    }
}
