package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.tests.withSingleThread
import io.bluetape4k.junit5.awaitility.suspendUntil
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test

class ReplaySubjectSizeBoundTest {

    companion object: KLoggingChannel()

    @Test
    fun `basic online`() = runTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10)
            val result = mutableListOf<Int>()

            val job = launch {
                replay
                    .log("#1")
                    .onEach { delay(10) }
                    .collect { result.add(it) }
            }.log("job1")

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
    fun `basic offline`() = runTest {
        val replay = ReplaySubject<Int>(10)

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
    fun `error online`() = runTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10)

            val result = mutableListOf<Int>()
            val exc = atomic<Throwable?>(null)

            val job = launch {
                try {
                    replay
                        .onEach { delay(10) }
                        .log("#1")
                        .collect { result.add(it) }
                } catch (e: Throwable) {
                    exc.value = e
                }
            }.log("job")

            replay.awaitCollector()

            repeat(5) {
                replay.emit(it)
            }
            replay.emitError(RuntimeException("Boom!"))
            replay.emit(10) // 예외 발생 이후는 emit 하지 않습니다.
            replay.emit(11)

            job.join()

            result shouldBeEqualTo listOf(0, 1, 2, 3, 4)
            exc.value shouldBeInstanceOf RuntimeException::class
        }
    }

    @Test
    fun `error offline`() = runTest {
        val replay = ReplaySubject<Int>(10)

        val result = mutableListOf<Int>()
        val exc = atomic<Throwable?>(null)

        repeat(5) {
            replay.emit(it)
        }
        replay.emitError(RuntimeException("Boom!"))
        replay.emit(10)  // 예외 발생 이후는 emit 하지 않습니다.
        replay.emit(11)

        try {
            replay
                .log("#1")
                .collect { result.add(it) }
        } catch (e: Throwable) {
            exc.value = e
        }

        result shouldBeEqualTo listOf(0, 1, 2, 3, 4)
        exc.value shouldBeInstanceOf RuntimeException::class
    }

    @Test
    fun `take online`() = runTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10)
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
    fun `take offline`() = runTest {
        val replay = ReplaySubject<Int>(10)

        repeat(5) {
            replay.emit(it)
        }
        replay.complete()

        val result = mutableListOf<Int>()
        replay.take(3)
            .log("#1")
            .collect {
                result.add(it)
            }

        result shouldBeEqualTo listOf(0, 1, 2)
    }

    @Test
    fun `bounded online`() = runTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(2)
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

            // replay -------------------------

            result.clear()

            replay
                .log("#2")
                .collect {
                    result.add(it)
                }
            result shouldBeEqualTo listOf(3, 4)
        }
    }

    @Test
    fun `bounded offline`() = runTest {
        // 마지막 2개를 버퍼링한다 
        val replay = ReplaySubject<Int>(2)

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
    fun `multiple online`() = runTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10)

            val result1 = mutableListOf<Int>()
            val result2 = mutableListOf<Int>()

            val job1 = launch {
                replay
                    .onEach { delay(10) }
                    .log("#1")
                    .collect { result1.add(it) }
            }.log("job1")

            val job2 = launch {
                replay
                    .onEach { delay(20) }
                    .log("#2")
                    .collect { result2.add(it) }
            }.log("jbo2")

            replay.awaitCollector()

            repeat(5) {
                replay.emit(it)
            }
            replay.complete()

            job1.join()
            job2.join()

            result1 shouldBeEqualTo listOf(0, 1, 2, 3, 4)
            result2 shouldBeEqualTo listOf(0, 1, 2, 3, 4)
        }
    }

    @Test
    fun `multiple with take online`() = runTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(10)

            val result1 = mutableListOf<Int>()
            val job1 = launch {
                replay
                    .onEach { delay(10) }
                    .log("#1")
                    .collect { result1.add(it) }
            }.log("job1")

            val result2 = mutableListOf<Int>()
            val job2 = launch {
                replay.take(3)
                    .onEach { delay(20) }
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
    fun `cancelled consumer`() = runTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(20)

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
            }

            replay.awaitCollector()

            repeat(n) {
                replay.emit(it)
            }

            await suspendUntil { job1.isCancelled && replay.collectorCount == 0 }

            job1.isCancelled.shouldBeTrue()
            counter1.value shouldBeEqualTo expected
            replay.collectorCount shouldBeEqualTo 0
        }
    }

    @Test
    fun `cancelled one collector second completes`() = runTest {
        withSingleThread {
            val replay = ReplaySubject<Int>(20)

            val expected = 3
            val n = 10

            val counter1 = atomic(0)
            val counter2 = atomic(0)

            val job1 = launch {
                replay
                    .onEach { delay(1) }
                    .log("#1")
                    .collect {
                        if (counter1.incrementAndGet() == expected) {
                            this.cancel()
                        }
                    }
            }.log("job1")

            val job2 = launch {
                replay
                    .onEach { delay(1) }
                    .log("#2")
                    .collect { counter2.incrementAndGet() }
            }.log("job2")

            replay.awaitCollector()

            repeat(n) {
                replay.emit(it)
            }

            replay.complete()
            await suspendUntil { job1.isCancelled && job2.isCompleted && replay.collectorCount == 0 }

            job1.isCancelled.shouldBeTrue()
            job2.isCompleted.shouldBeTrue()

            counter1.value shouldBeEqualTo expected
            counter2.value shouldBeEqualTo n
            replay.collectorCount shouldBeEqualTo 0
        }
    }
}
