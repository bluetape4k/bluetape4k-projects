package io.bluetape4k.examples.mutiny

import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import io.bluetape4k.mutiny.asUni
import io.bluetape4k.mutiny.multiOf
import io.bluetape4k.mutiny.multiRangeOf
import io.bluetape4k.mutiny.uniOf
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asFlow
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.smallrye.mutiny.tuples.Tuple2
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeIn
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldContainSame
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.random.Random

class CompositionTransformationExamples {

    companion object: KLogging()

    @Test
    fun `01 Uni transform`() {
        val item = uniOf(123)
            .onItem().transform { it * 100 }
            .onItem().transform { it.toString() }
            .await().indefinitely()

        item shouldBeEqualTo "12300"
    }

    @Test
    fun `02 Uni transformToUni`() {
        val item = uniOf(123)
            .onItem().transformToUni { item -> uniOf(item) }
            .onItem().transformToUni { item, emitter ->
                emitter.complete("[$item]")
            }
            .await().indefinitely()

        item shouldBeEqualTo "[123]"
    }

    @Test
    fun `03 Uni replaceWith and chain and map`() {
        val item = uniOf(123)
            .replaceWith(uniOf(456))
            .chain { item -> uniOf(item) }
            .map { item -> "[$item]" }
            .await().indefinitely()

        item shouldBeEqualTo "[456]"
    }

    @Test
    fun `04 Uni stage`() {

        fun processEvents(uni: Uni<Int>): Uni<Int> =
            uni
                .onSubscription().invoke { _ -> log.debug { "onSubscribe" } }
                .onItem().invoke { n -> log.debug { "item=$n" } }

        fun handleItem(uni: Uni<Int>): Uni<Int> =
            uni
                .onItem().ifNotNull().invoke { _ -> log.debug { "The item is not null" } }
                .onItem().transform { n -> n * 20 }

        fun handleFailure(uni: Uni<Int>): Uni<Int> =
            uni
                .onFailure().invoke { e -> log.debug(e) { "There is a failure" } }
                .onFailure(IOException::class.java).recoverWithItem(-1)

        val item = uniOf(123)
            .stage { uni -> processEvents(uni) }
            .stage { uni -> handleItem(uni) }
            .stage { uni -> handleFailure(uni) }
            .await().indefinitely()

        item shouldBeEqualTo 123 * 20
    }


    @Test
    fun `05 Uni combine`() {
        val first = uniOf(1)
        val second = uniOf(2)
        val third = uniOf(3)

        val tuple = Uni.combine().all().unis(first, second, third)
            .asTuple()
            .await().indefinitely()

        tuple.toList() shouldBeEqualTo listOf(1, 2, 3)

        val combined = Uni.combine().all().unis(first, second, third)
            .with { a, b, c -> a + b + c }
            .await().indefinitely()

        combined shouldBeEqualTo 6

        // 무작위로 하나를 선택 - any()
        val one = Uni.combine().any().of(first, second, third)
            .await().indefinitely()

        one shouldBeIn intArrayOf(1, 2, 3)
    }

    @Test
    fun `06 Multi transform`() = runTest {
        val evens = multiRangeOf(1, 100)
            .filter { it % 2 == 0 }
            .select().last(5)
            .onItem().transform { n -> "[$n]" }
            .asFlow().toList()

        val expected = (1..99).filter { it % 2 == 0 }.map { "[$it]" }.takeLast(5)

        evens shouldBeEqualTo expected
    }

    @Test
    fun `07 Multi transformToUni`() = runTest {
        fun increase(n: Int): Uni<Int> {
            val cs = CompletableFuture.supplyAsync(
                { n * 100 },
                CompletableFuture.delayedExecutor(Random.nextLong(100), TimeUnit.MILLISECONDS)
            )
            return cs.asUni()
        }

        val items = multiRangeOf(1, 100)
            .filter { it % 2 == 0 }
            .select().last(5)
            .onItem().transformToUniAndMerge { n -> increase(n) }       // Flow#flatMapMerge 랑 유사
            .onItem().transform { n -> "[$n]" }
            .asFlow().toList()

        items.sorted() shouldBeEqualTo listOf("[9000]", "[9200]", "[9400]", "[9600]", "[9800]")
    }

    @Test
    fun `08 Multi transformToMulti`() = runTest {
        fun query(n: Int): Multi<Int> = Multi.createFrom()
            .emitter { emitter ->
                CompletableFuture.runAsync(
                    {
                        emitter.emit(n)
                        emitter.emit(n * 10)
                        emitter.emit(n * 100)
                        emitter.complete()
                    },
                    CompletableFuture.delayedExecutor(Random.nextLong(100), TimeUnit.MILLISECONDS)
                )
            }

        val items = multiRangeOf(1, 100)
            .filter { it % 2 == 0 }
            .select().last(2)
            .onItem().transformToMultiAndMerge { n -> query(n) }   // Flow.flatMapMerge 와 같다
            .onItem().transform { "[$it]" }
            .onItem().invoke { item -> log.debug { "item: $item" } }
            .asFlow().toList()

        items shouldHaveSize 6
        items shouldContainSame listOf("[96]", "[98]", "[960]", "[980]", "[9600]", "[9800]")
    }

    @Test
    fun `09 Multi flatMap`() = runTest {
        fun query(n: Int): Multi<Int> = Multi.createFrom()
            .emitter { emitter ->
                CompletableFuture.runAsync(
                    {
                        emitter.emit(n)
                        emitter.emit(n * 10)
                        emitter.emit(n * 100)
                        emitter.complete()
                    },
                    CompletableFuture.delayedExecutor(Random.nextLong(500), TimeUnit.MILLISECONDS)
                )
            }

        val items = multiRangeOf(1, 100)
            .filter { it % 2 == 0 }
            .select().last(2)
            .flatMap { n -> query(n) }              // Flow.flatMapMerge 와 같다
            .map { n -> "[$n]" }
            .onItem().invoke { n -> log.debug { n } }
            .asFlow().toList()

        items shouldHaveSize 6
        items shouldContainSame listOf("[96]", "[98]", "[960]", "[980]", "[9600]", "[9800]")
    }

    // merge 는 복수의 Multi로 부터 먼저 오는 것부터 subscribe 한다
    @Test
    fun `10 Multi merge multiple multi instance`() = runTest {
        val generator1 = Generator(0)
        val generator2 = Generator(100)

        val multi1: Multi<Long> = Multi.createBy().repeating().uni(generator1::next).atMost(10)
        val multi2: Multi<Long> = Multi.createBy().repeating().uni(generator2::next).atMost(10)

        val merged: List<Long> = Multi.createBy()
            .merging().streams(multi1, multi2)
            .onItem().invoke { n -> log.trace { n } }
            .collect()
            .asList()
            .awaitSuspending()

        merged.sorted() shouldBeEqualTo (0L..9L).toList() + (100L..109L).toList()
    }

    // concatenate 는 첫번째 Multi 가 끝나야 다음 Multi 로부터 subscribe 한다
    @Test
    fun `11 Multi concatenate`() = runTest {
        val generator1 = Generator(0)
        val generator2 = Generator(100)

        val multi1: Multi<Long> = Multi.createBy().repeating().uni(generator1::next).atMost(10)
        val multi2: Multi<Long> = Multi.createBy().repeating().uni(generator2::next).atMost(10)

        val concated = Multi.createBy()
            .concatenating().streams(multi1, multi2)
            .onItem().invoke { n -> log.debug { n } }
            .collect()
            .asList()
            .awaitSuspending()

        concated.sorted() shouldBeEqualTo (0L..9L).toList() + (100L..109L).toList()
    }

    @Test
    fun `12 Multi combine`() = runTest {
        val generator1 = Generator(0)
        val generator2 = Generator(100)

        val multi1: Multi<Long> = Multi.createBy().repeating().uni(generator1::next).atMost(10)
        val multi2: Multi<Long> = Multi.createBy().repeating().uni(generator2::next).atMost(10)

        val results = Multi.createBy()
            .combining().streams(multi1, multi2).asTuple()
            .onItem().invoke { it -> log.debug { it } }
            .collect()
            .asList()
            .awaitSuspending()

        val expected = List(10) { Tuple2.of(it.toLong(), it + 100L) }
        results shouldBeEqualTo expected
    }

    class Generator(start: Long) {
        private val counter = atomic(start)

        fun next(): Uni<Long> = Uni.createFrom().completionStage(
            CompletableFuture.supplyAsync(
                counter::getAndIncrement,
                CompletableFuture.delayedExecutor(Random.nextLong(100), TimeUnit.MILLISECONDS)
            )
        )
    }

    @Test
    fun `13 Multi Broadcast`() {
        val counter = atomic(0)
        val executor = Executors.newCachedThreadPool()

        val multi = Multi.createBy()
            .repeating().supplier(counter::getAndIncrement)
            .atMost(10)
            .broadcast().toAllSubscribers()

        val latch = CountDownLatch(3)

        executor.submit {
            multi.onItem().transform { n -> "🚀 $n" }.subscribe().with(::println)
            latch.countDown()
        }
        executor.submit {
            multi.onItem().transform { n -> "🧪 $n" }.subscribe().with(::println)
            latch.countDown()
        }
        executor.submit {
            multi.onItem().transform { n -> "💡 $n" }.subscribe().with(::println)
            latch.countDown()
        }

        latch.await()
        executor.shutdown()
    }

    @Test
    fun `13-1 Multi Broadcast in coroutines`() = runTest {
        val counter = atomic(0)

        val multi: Multi<Int> = Multi.createBy()
            .repeating().supplier(counter::getAndIncrement)
            .atMost(10)
            .broadcast()
            .toAllSubscribers()

        val jobs = listOf(
            launch(Dispatchers.Default) {
                multi.onItem().transform { n -> "🚀 $n" }
                    .asFlow()
                    .onEach { n -> log.debug { n } }
                    .collect()
            },
            launch(Dispatchers.Default) {
                multi.onItem().transform { n -> "🧪 $n" }
                    .asFlow()
                    .onEach { n -> log.debug { n } }
                    .collect()
            },
            launch(Dispatchers.Default) {
                multi.onItem().transform { n -> "💡 $n" }
                    .asFlow()
                    .onEach { n -> log.debug { n } }
                    .collect()
            }
        )
        jobs.joinAll()
        counter.value shouldBeEqualTo 10
    }

    @Test
    fun `14 Multi aggregates`() = runTest {
        log.debug { "👀 Multi aggregates" }

        val persons = Multi.createBy()
            .repeating()
            .supplier { generate() }
            .atMost(100)

        println("\nscan\n")
        persons
            .onItem().scan({ 0 }) { count, _ -> count + 1 }
            .subscribe().with { count -> log.debug { "We have $count persons" } }

        println("\ncollect with\n")
        persons
            .collect().with(Collectors.counting())
            .subscribe().with { count -> log.debug { "We have $count persons" } }

        println("\ncollect asList()\n")
        persons
            .filter { person -> person.city == "Seoul" }
            .collect().asList()
            .subscribe().with { list ->
                log.debug { "They live in Seoul:\n${list.joinToString("\n")}" }
            }

        println("\ngroup by, merge\n")
        val agePerCity = persons
            .group().by { it.city }
            .onItem().transformToUni { group ->
                val city = group.key()
                val avg = group.collect().with(Collectors.averagingInt { it.age })
                avg.onItem().transform { res -> "Average age in $city is $res" }
            }
            .merge()
            .asFlow().toList()

        log.debug { "Average:\n${agePerCity.joinToString("\n")}" }

    }

    private fun generate(): Person =
        Person(
            identifier = UUID.randomUUID().encodeBase62(),
            age = Random.nextInt(18, 50),
            city = cities.random()
        )

    private val cities = listOf("Seoul", "LA", "Washington", "Paris", "London")

    data class Person(
        val identifier: String,
        val age: Int,
        val city: String,
    )

    @Test
    fun `15 Multi Buckets`() = runTest {
        log.debug { "👀 Multi buckets" }

        val buckets = Multi.createFrom()
            .ticks().every(Duration.ofMillis(100))
            .onItem().transform { it.toInt() }
            .group().intoLists().of(5)
            .select().first(2)
            .asFlow().toList()

        buckets shouldBeEqualTo listOf((0..4).toList(), (5..9).toList())
    }

    @Test
    fun `16 Multi Temporal Buckets`() = runTest {
        log.debug { "👀 Multi temporal buckets" }

        // NOTE: 시간 기준의 debouncing 이다
        val buckets = Multi.createFrom()
            .ticks().every(Duration.ofMillis(100))
            .group().intoLists().every(Duration.ofSeconds(1))
            .onItem().invoke { items -> log.debug { "items=$items" } }
            .select().first(2)
            .asFlow().toList()

        buckets shouldHaveSize 2
        buckets[0] shouldContain 5
        buckets[1] shouldContain 15
    }

    // Collection.flatMap 과 같은 기능
    @Test
    fun `17 Multi Disjoint`() = runTest {
        log.debug { "👀 Multi disjoint" }

        val items = Multi.createFrom().range(0, 10)
            .onItem().transformToMultiAndMerge { n -> multiOf(n, n * 2, n * 5, n, n * 10) }
            .group().intoLists().of(3)
            .onItem().invoke { items -> log.debug { "grouped: $items" } }
            .onItem().disjoint<Int>()
            .onItem().invoke { it -> log.debug { "disjoint: $it" } }
            .asFlow().toList()

        items shouldHaveSize 50
    }

    @Test
    fun `18 Multo to Uni and back`() = runTest {
        log.debug { "👀 Multi <--> Uni" }

        // toUni() 는 첫번째 요소를 취한다
        val uni = multiRangeOf(1, 10).toUni().awaitSuspending()
        log.debug { "uni=$uni" }
        uni shouldBeEqualTo 1

        val multi = uniOf(123).toMulti().asFlow().toList()
        log.debug { "multi=$multi" }
        multi shouldBeEqualTo listOf(123)
    }

}
