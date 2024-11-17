package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.tests.withSingleThread
import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.KLogging
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit


/**
 * NOTE: 시간에 따라 Buffering 을 수행하므로, [runTest] 를 사용하지 않고, [runSuspendTest] 를 사용해야 합니다.
 */
class ReplaySubjectSizeAndTimeBoundTest {

    companion object: KLogging()

    @Test
    fun `basic online`() = runSuspendTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10, 1L, TimeUnit.MINUTES)
            val result = mutableListOf<Int>()

            val job = launch {
                replay
                    .onEach { delay(10) }
                    .log("#1")
                    .collect { result.add(it) }
            }.log("job")

            replay.awaitCollector()

            repeat(5) {
                replay.emit(it)
            }
            replay.complete()
            job.join()

            result shouldBeEqualTo listOf(0, 1, 2, 3, 4)
        }
    }

    @Test
    fun `basic offline - collector 가 등록되지 않은 상태에서 emit 한 것은 버퍼링한다`() = runSuspendTest {
        val replay = ReplaySubject<Int>(10, 1L, TimeUnit.MINUTES)

        repeat(5) {
            replay.emit(it)
        }
        replay.complete()


        val result = mutableListOf<Int>()
        replay
            .onEach { delay(10) }
            .log("#1")
            .collect { result.add(it) }

        result shouldBeEqualTo listOf(0, 1, 2, 3, 4)
    }

    @Test
    fun `timed online`() = runSuspendTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(1L, TimeUnit.MINUTES)
            val result = mutableListOf<Int>()

            val job = launch {
                replay
                    .onEach { delay(10) }
                    .log("#1")
                    .collect { result.add(it) }
            }
            replay.awaitCollector()

            repeat(5) {
                replay.emit(it)
            }
            replay.complete()
            job.join()

            result shouldBeEqualTo listOf(0, 1, 2, 3, 4)
        }
    }

    @Test
    fun `timed offline`() = runSuspendTest {
        val replay = ReplaySubject<Int>(1L, TimeUnit.MINUTES)

        // collector 가 등록되지 않은 상태에서 완료된다.
        repeat(5) {
            replay.emit(it)
        }
        replay.complete()

        val result = mutableListOf<Int>()
        replay
            .onEach { delay(10) }
            .log("#1")
            .collect { result.add(it) }

        result shouldBeEqualTo listOf(0, 1, 2, 3, 4)
    }

    @Test
    fun `timed offline with multiple collector`() = runSuspendTest {
        val replay = ReplaySubject<Int>(1L, TimeUnit.MINUTES)

        // collector 가 등록되지 않은 상태에서 완료된다.
        repeat(5) {
            replay.emit(it)
        }
        replay.complete()

        val result = mutableListOf<Int>()
        replay
            .onEach { delay(10) }
            .log("#1")
            .collect { result.add(it) }

        result shouldBeEqualTo listOf(0, 1, 2, 3, 4)

        // Cold stream 처럼 collect 반복해도 같은 값을 emit 해준다 
        val result2 = mutableListOf<Int>()
        replay
            .onEach { delay(10) }
            .log("#1")
            .collect { result2.add(it) }

        result shouldBeEqualTo listOf(0, 1, 2, 3, 4)
    }

    @Test
    fun `error online`() = runSuspendTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10, 1L, TimeUnit.MINUTES)
            val result = mutableListOf<Int>()
            val exc = atomic<Throwable?>(null)

            val job = launch {
                // 예외가 emit 되면, collect 가 중단된다
                try {
                    replay
                        .onEach { delay(10) }
                        .log("#1")
                        .collect { result.add(it) }
                } catch (ex: Throwable) {
                    exc.value = ex
                }
            }.log("job")
            replay.awaitCollector()

            repeat(5) {
                replay.emit(it)
            }
            replay.emitError(RuntimeException("Boom!"))

            // 예외 이후에는 emit 된 요소는 consume 되지 않는다
            replay.emit(5)
            replay.emit(6)

            job.join()

            result shouldBeEqualTo listOf(0, 1, 2, 3, 4)
            exc.value shouldBeInstanceOf RuntimeException::class
        }
    }

    @Test
    fun `error offline`() = runSuspendTest {
        val replay = ReplaySubject<Int>(10, 1L, TimeUnit.MINUTES)
        val exc = atomic<Throwable?>(null)

        repeat(5) {
            replay.emit(it)
        }
        replay.emitError(RuntimeException("Boom!"))
        replay.emit(5)
        replay.emit(6)

        // 예외 이후에는 emit 된 요소는 consume 되지 않는다
        val result = mutableListOf<Int>()
        try {
            replay
                .onEach { delay(10) }
                .log("#1")
                .collect { result.add(it) }
        } catch (ex: Throwable) {
            exc.value = ex
        }

        result shouldBeEqualTo listOf(0, 1, 2, 3, 4)
        exc.value shouldBeInstanceOf RuntimeException::class
    }

    @Test
    fun `take online`() = runSuspendTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10, 1L, TimeUnit.MINUTES)
            val result = mutableListOf<Int>()

            val job = launch {
                replay.take(3)
                    .onEach { delay(10) }
                    .log("#1")
                    .collect { result.add(it) }
            }.log("job")
            replay.awaitCollector()

            repeat(5) {
                replay.emit(it)
            }
            replay.complete()
            job.join()

            result shouldBeEqualTo listOf(0, 1, 2)
        }
    }

    @Test
    fun `take offline`() = runSuspendTest {
        val replay = ReplaySubject<Int>(10, 1L, TimeUnit.MINUTES)

        repeat(5) {
            replay.emit(it)
        }
        replay.complete()

        val result = mutableListOf<Int>()
        replay.take(3)
            .onEach { delay(10) }
            .log("#1")
            .collect { result.add(it) }

        result shouldBeEqualTo listOf(0, 1, 2)
    }

    @Test
    fun `bounded online`() = runSuspendTest {
        withSingleThread {
            // 2개만 버퍼링 합니다.
            val replay = ReplaySubject<Int>(2, 1L, TimeUnit.MINUTES)
            val result = mutableListOf<Int>()

            val job = launch {
                replay
                    .onEach { delay(10) }
                    .log("#1")
                    .collect { result.add(it) }
            }.log("job")

            replay.awaitCollector()
            repeat(5) {
                replay.emit(it)
            }
            replay.complete()
            job.join()

            result shouldBeEqualTo listOf(0, 1, 2, 3, 4)

            // cold-stream과 같이 consumer를 다시 수행한다면 버퍼링된 2개만 제공된다
            result.clear()
            replay
                .log("#2")
                .collect { result.add(it) }
            result shouldBeEqualTo listOf(3, 4)
        }
    }

    @Test
    fun `bounded offline`() = runSuspendTest {
        val replay = ReplaySubject<Int>(2, 1L, TimeUnit.MINUTES)

        // 5개를 emit 하지만, 버퍼링이 maxSize=2 까지만 됩니다.
        repeat(5) {
            replay.emit(it)
        }
        replay.complete()

        val result = mutableListOf<Int>()
        replay
            .onEach { delay(10) }
            .log("#1")
            .collect { result.add(it) }

        result shouldBeEqualTo listOf(3, 4)
    }

    @Test
    fun `timed offline 1`() = runSuspendTest {
        // timeout 이 지난 후에는 남기지 않는다.
        val replay = ReplaySubject<Int>(10, 100, TimeUnit.MILLISECONDS)

        repeat(5) {
            replay.emit(it)
        }
        replay.complete()

        delay(300)

        val result = mutableListOf<Int>()
        replay
            .log("#1")
            .collect { result.add(it) }

        result.isEmpty().shouldBeTrue()
    }

    @Test
    fun `timed offline 2`() = runSuspendTest {
        val replay = ReplaySubject<Int>(10, 100, TimeUnit.MILLISECONDS)

        repeat(3) {
            replay.emit(it)
        }
        // timeout 이 지난 후에는 버퍼링된 요소를 제거합니다.
        delay(300)

        // 새롭게 emit 된 놈들은 버퍼링된다
        replay.emit(3)
        replay.emit(4)
        replay.complete()

        val result = mutableListOf<Int>()
        replay
            .onEach { delay(10) }
            .log("#1")
            .collect { result.add(it) }

        result shouldBeEqualTo listOf(3, 4)
    }

    @Test
    fun `multiple online`() = runSuspendTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10, 1L, TimeUnit.MINUTES)

            val result1 = mutableListOf<Int>()
            val job1 = launch {
                replay
                    .onEach { delay(50) }
                    .log("#1")
                    .collect { result1.add(it) }
            }.log("job1")

            val result2 = mutableListOf<Int>()
            val job2 = launch {
                replay
                    .onEach { delay(100) }
                    .log("#2")
                    .collect { result2.add(it) }
            }.log("job2")

            replay.awaitCollector()

            repeat(5) {
                replay.emit(it)
            }
            replay.complete()

            job1.join()
            job2.join()

            val expected = listOf(0, 1, 2, 3, 4)
            result1 shouldBeEqualTo expected
            result2 shouldBeEqualTo expected
        }
    }

    @Test
    fun `multiple with take online`() = runSuspendTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10, 1L, TimeUnit.MINUTES)

            val result1 = mutableListOf<Int>()
            val job1 = launch {
                replay
                    .onEach { delay(50) }
                    .log("#1")
                    .collect { result1.add(it) }
            }.log("job1")

            val result2 = mutableListOf<Int>()
            val job2 = launch {
                replay.take(3)
                    .onEach { delay(50) }
                    .log("#2")
                    .collect { result2.add(it) }
            }.log("job2")

            replay.awaitCollector()

            repeat(5) {
                replay.emit(it)
            }
            replay.complete()

            job1.join()
            job2.join()

            result1 shouldBeEqualTo listOf(0, 1, 2, 3, 4)
            result2 shouldBeEqualTo listOf(0, 1, 2)
        }
    }

    @Test
    fun `cancelled consumer`() = runSuspendTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(20, 1L, TimeUnit.MINUTES)

            val expected = 3
            val n = 10
            val counter1 = atomic(0)

            val job1 = launch {
                replay
                    .log("#1")
                    .collect {
                        if (counter1.incrementAndGet() == expected) {
                            this.cancel()
                        }
                    }
            }.log("job1")

            replay.awaitCollector()

            repeat(n) {
                replay.emit(it)
            }
            replay.complete()
            yield()

            // job1 이 취소되고, replay의 collector가 모두 제거될 때까지 대기
            await until { job1.isCancelled && replay.collectorCount == 0 }

            job1.isCancelled.shouldBeTrue()
            counter1.value shouldBeEqualTo expected
            replay.collectorCount shouldBeEqualTo 0
        }
    }

    @Test
    fun `cancelled one collector and second completes`() = runSuspendTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(20, 1L, TimeUnit.MINUTES)

            val expected = 3
            val n = 10

            val counter1 = atomic(0)
            val counter2 = atomic(0)

            val job1 = launch {
                replay
                    .log("#1")
                    .collect {
                        if (counter1.incrementAndGet() == expected) {
                            this.cancel()
                        }
                    }
            }.log("job1")

            val job2 = launch {
                replay
                    .log("#2")
                    .collect { counter2.incrementAndGet() }
            }.log("job2")

            replay.awaitCollector()

            repeat(n) {
                replay.emit(it)
            }
            replay.complete()
            job1.join()
            job2.join()

            job1.isCancelled.shouldBeTrue()
            job2.isCompleted.shouldBeTrue()

            counter1.value shouldBeEqualTo expected
            counter2.value shouldBeEqualTo n
            replay.collectorCount shouldBeEqualTo 0
        }
    }
}
