package io.bluetape4k.examples.mutiny

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.mutiny.deferUni
import io.bluetape4k.mutiny.multiOf
import io.bluetape4k.mutiny.multiRangeOf
import io.bluetape4k.mutiny.onEach
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.subscription.MultiSubscriber
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.Flow
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

class MultiBasicExamples {

    companion object: KLoggingChannel()

    @Test
    fun `01 Multi Basic`() {
        // Just item
        multiOf(1, 2, 3)
            .subscribe().with(
                { subscription ->
                    println("Subscription: $subscription")
                    subscription.request(10)
                }, // onSubscriptoin
                { item -> log.debug { "Item:$item" } },  // onItem
                { failure -> log.debug { "Failure: ${failure.message}" } },  // onFailure
                { log.debug { "Completed" } } // onComplete
            )

        // Range (10 <= x < 15)
        val list = multiRangeOf(10, 15)
            .onEach { log.debug { "range: $it" } }
            .collect()
            .asList()
            .await()
            .indefinitely()

        list shouldBeEqualTo listOf(10, 11, 12, 13, 14)

        // from Iterable
        val randomNumbers = generateSequence { Random.nextInt() }.take(5).toList()
        val randoms = Multi.createFrom().iterable(randomNumbers)
            .onItem().invoke { item -> log.debug { "range: $item" } }
            .collect()
            .asList()
            .await()
            .indefinitely()

        randoms shouldBeEqualTo randomNumbers
    }

    @Test
    fun `02 Multi with reactivestreams subscriber`() {
        Multi.createFrom().items(1, 2, 3)
            .subscribe()
            .withSubscriber(object: MultiSubscriber<Int> {
                private var subscription: Flow.Subscription? = null
                override fun onSubscribe(s: Flow.Subscription) {
                    log.debug { "onSubscribe()" }
                    this.subscription = s
                    this.subscription!!.request(1)
                }

                override fun onFailure(failure: Throwable?) {
                    println("onError: $failure")
                }

                override fun onCompletion() {
                    println("onComplete")
                }

                override fun onItem(item: Int?) {
                    log.debug { "onNext: $item" }
                    this.subscription!!.request(1)
                }
            })
    }

    @Test
    fun `03 Multi from emitter`() {
        val service = Executors.newScheduledThreadPool(1)
        val ref = AtomicReference<ScheduledFuture<*>>(null)
        val counter = AtomicInteger(0)
        val latch = CountDownLatch(1)

        val captures = CopyOnWriteArrayList<String>()

        Multi.createFrom()
            .emitter { emitter ->
                val scheduledFuture = service.scheduleAtFixedRate(
                    {
                        emitter.emit("tick")
                        log.debug { "Emit: tick" }
                        if (counter.incrementAndGet() == 5) {
                            ref.get()?.cancel(true)
                            emitter.complete()
                            latch.countDown()
                        }
                    },
                    0,
                    500,
                    TimeUnit.MILLISECONDS
                )
                ref.set(scheduledFuture)
            }
            .subscribe()
            .with(
                { item -> captures.add(item); println(item) },
                Throwable::printStackTrace,
                { println("Done!") }
            )

        latch.await()
        service.shutdown()

        captures shouldHaveSize 5
        captures.toSet() shouldBeEqualTo setOf("tick")
    }

    @Test
    fun `04 Multi control subscription`() {
        Multi.createFrom()
            .ticks().every(Duration.ofSeconds(1))
            .subscribe()
            .withSubscriber(
                object: MultiSubscriber<Long> {
                    private lateinit var subscription: Flow.Subscription
                    private var counter = AtomicInteger()

                    override fun onSubscribe(s: Flow.Subscription) {
                        subscription = s
                        subscription.request(1)
                    }

                    override fun onItem(item: Long) {
                        log.debug { "Tick: $item" }
                        if (counter.getAndIncrement() == 10) {
                            subscription.cancel()
                        } else {
                            subscription.request(1)
                        }
                    }

                    override fun onFailure(failure: Throwable?) {
                        failure?.printStackTrace()
                    }

                    override fun onCompletion() {
                        log.debug { "Done" }
                    }
                }
            )
    }

    object Service {
        fun fetchValue(): Long = Random.nextLong(1_011_000L)
        fun queryDb(): CompletionStage<Long> = CompletableFuture.supplyAsync { fetchValue() }
        fun asyncFetchValue(): Uni<Long> = Uni.createFrom().completionStage { queryDb() }
    }

    @Test
    fun `05 Multi by repeating`() {
        println("⚡️ Multi by repeating")

        // repeat from supplier
        Multi.createBy()
            .repeating().supplier { Service.fetchValue() }.until { n -> n > 1_000_000L }
            .subscribe().with { log.debug { it } }

        println("\n----------\n")

        val latch = CountDownLatch(1)

        // repeat from Uni
        Multi.createBy()
            .repeating().deferUni { Service.asyncFetchValue() }.atMost(10)
            .subscribe()
            .with({ log.debug { it } }, Throwable::printStackTrace, latch::countDown)

        latch.await()

        println("\n----------\n")

        // supplier from completionStage
        Multi.createBy()
            .repeating()
            .completionStage(Service::queryDb)
            .whilst { it < 1_000_000L }
            .subscribe()
            .with { log.debug { it } }
    }

    class MyResource {
        fun stream(): Multi<Int> {
            println("stream()")
            return multiRangeOf(0, 10) // Multi.createFrom().range(0, 10)
        }

        fun close() {
            println("close()")
        }
    }

    @Test
    fun `06 Multi from Resource`() {
        Multi.createFrom()
            .resource(MultiBasicExamples::MyResource, MyResource::stream)
            .withFinalizer(MyResource::close)
            .subscribe()
            .with(::println)
    }
}
