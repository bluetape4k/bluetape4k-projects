package io.bluetape4k.examples.mutiny

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.mutiny.asUni
import io.bluetape4k.mutiny.onEach
import io.bluetape4k.mutiny.uniConvertOf
import io.bluetape4k.mutiny.uniFailureOf
import io.bluetape4k.mutiny.uniOf
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.smallrye.mutiny.subscription.UniSubscriber
import io.smallrye.mutiny.subscription.UniSubscription
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

class UniBasicExamples {

    companion object: KLoggingChannel()

    @Test
    fun `01 하나의 요소로 Uni 인스턴스 생성하기`() {
        val uni = uniOf("Hello")

        uni.subscribe().with { result ->
            log.debug { "Subscribe for Uni. result=$result" }
            result shouldBeEqualTo "Hello"
        }
    }

    @Test
    fun `02 UniSubscriber 구현체로 Uni를 구독하기`() {
        val uni = uniOf("Hello")

        uni.subscribe().withSubscriber(object: UniSubscriber<String> {
            override fun onSubscribe(subscription: UniSubscription) {
                log.debug { "onSubscribe. subscription 설정으로 요청을 보냅니다" }
            }

            override fun onFailure(failure: Throwable?) {
                log.debug { "onFailer: $failure" }
            }

            override fun onItem(item: String?) {
                log.debug { "onItem: $item" }
            }
        })
    }

    @Test
    fun `03 supplier 로 Uni 를 생성하면 subscribe 할 때마다 emit 된다`() = runTest {

        // Supplier 로 Uni 를 생성하면, subscription 을 요청할 때마다 무한대로 item이 제공된다
        val uni = uniOf { Random.nextInt() }

        // Coroutines 환경에서 await 한다 (subscribe 한다)
        // `uni.await().indefinitely()` 는 blocking 이다
        val items = List(5) { uni.awaitSuspending() }

        items shouldHaveSize 5
        log.debug { items.joinToString() }
    }

    @Test
    fun `04 Supplier와 상태를 이용하여 Uni 인스턴스 생성하기 - fold와 유사`() = runTest {
        val uni = Uni.createFrom().item({ AtomicInteger(0) }) {
            it.addAndGet(10)
        }

        val items = List(5) { uni.awaitSuspending() }
        items shouldBeEqualTo listOf(10, 20, 30, 40, 50)
    }

    @Test
    fun `05 deferred - Supplier와 상태를 이용하여, 구독 시마다 값을 계산해서 제공한다`() {
        // 구독 시점에 값을 계산해서 제공해준다
        val ids = AtomicLong(0L)
        val deferred = Uni.createFrom().deferred { Uni.createFrom().item(ids::incrementAndGet) }

        val results = CopyOnWriteArrayList<Long>()

        // Uni 지만 하나의 값만 제공하는 게 아니라 deferred 를 이용하면, subscription 요청 때마다 item을 제공한다
        repeat(5) {
            deferred.subscribe().with { results.add(it) }
        }
        results shouldBeEqualTo listOf(1, 2, 3, 4, 5)
    }

    @Test
    fun `06 emitter 를 이용하여 비동기로 emit 하기`() {
        val forkJoinPool = ForkJoinPool.commonPool()
        val emitterLatch = CountDownLatch(1)

        val uniFromEmitter: Uni<String> = Uni.createFrom().emitter { emitter ->
            forkJoinPool.submit {
                Thread.sleep(100)
                emitter.complete("Hello")
                emitterLatch.countDown()
            }
            log.debug { "Emitter를 이용하여 item 제공하기" }
        }

        var result: String? = null
        uniFromEmitter
            .onEach { item -> log.debug { "🔥: $item" } }
            .subscribe().with { result = it }

        emitterLatch.await()
        result shouldBeEqualTo "Hello"
    }

    @Test
    fun `07 Uni from emitter and state`() {
        val uni: Uni<Long> = Uni.createFrom()
            .emitter({ AtomicLong(0L) }) { state, emitter ->
                emitter.complete(state.addAndGet(10))
            }

        val results = CopyOnWriteArrayList<Long>()
        repeat(5) {
            uni.subscribe().with { results.add(it) }
        }
        results shouldBeEqualTo listOf(10, 20, 30, 40, 50)
    }

    // NOTE: 예외가 발생해도, onFailureCallback 이 없다면 예외를 rethrow 하지는 않는다
    @Test
    fun `08 uni from failure`() {
        var errormsg: String? = null

        uniFailureOf<Any> { IOException("Boom") }
            .subscribe()
            .with(::println) { e ->
                errormsg = e.message
            }
        errormsg shouldBeEqualTo "Boom"

        uniFailureOf<Any> { IOException("Badaboom") }
            .subscribe()
            .with(::println) { failure ->
                errormsg = failure.message
            }
        errormsg shouldBeEqualTo "Badaboom"

        uniFailureOf<Any> { IOException("Ahhhhh") }
            .subscribe()
            .with(::println) {
                println(it.message)
            }
    }

    @Test
    fun `09 uni from CompletionStage`() {
        val cs = CompletableFuture.supplyAsync(
            { "Hello" },
            CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS)
        )
            .thenApply { it.uppercase() }

        var result: String? = null
        cs.asUni()
            .subscribe()
            .with(
                { item -> result = item },
                { failure -> result = failure.message }
            )

        await atMost Duration.ofSeconds(2) until { result != null }
        result shouldBeEqualTo "HELLO"
    }

    @Test
    fun `10 uni misc`() {

        // no print
        Uni.createFrom().nothing<Any>()
            .subscribe().with(::println) { failure -> println(failure.message) }

        // print `null`
        Uni.createFrom().voidItem()
            .subscribe().with(::println) { failure -> println(failure.message) }

        // print `null`
        Uni.createFrom().nullItem<Any>()
            .subscribe().with(::println) { failure -> println(failure.message) }

        // print `Hello`
        Uni.createFrom().optional(Optional.of("Hello"))
            .subscribe().with(::println) { failure -> println(failure.message) }

        // print `[10]`
        // Creates a new [Uni] from the passed instance with the passed converter.
        Uni.createFrom().converter({ Uni.createFrom().item("[$it]") }, 10)
            .subscribe().with(::println) { failure -> println(failure.message) }

        uniConvertOf(10) { uniOf { "[$it]" } }
            .subscribe().with(::println) { failure -> println(failure.message) }
    }

    @Test
    fun `11 Uni delay`() {
        println("⚡️ Uni delay")

        uniOf(666)
            .onItem().delayIt().by(Duration.ofSeconds(1))
            .subscribe().with(::println)

        println("⏰")

        uniOf(666)
            .onItem().delayIt()
            .until {
                val future =
                    CompletableFuture.supplyAsync({ "OK" }, CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS))
                future.asUni()
            }
            .subscribe().with(::println)
    }

    @Test
    fun `12 Uni disjoint - convert to Multi`() {
        val list = uniOf(listOf(1, 2, 3, 4, 5))
            .onItem().disjoint<Int>() // disjoint 는 item을 분해해서 Multi 로 변환시킨다
            .collect().asList()
            .await().indefinitely()

        list shouldBeEqualTo listOf(1, 2, 3, 4, 5)
    }
}
