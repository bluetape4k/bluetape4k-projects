package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.coroutines.flow.extensions.flowRangeOf
import io.bluetape4k.coroutines.flow.extensions.log
import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.coroutines.tests.withSingleThread
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class UnicastWorkSubjectTest {

    companion object: KLoggingChannel() {
        private const val BUFFER_SIZE = 500_000
        private val expectedList = (0..4).toFastList()
    }

    private suspend fun generateInts(uws: UnicastWorkSubject<Int>, start: Int, count: Int) {
        flowRangeOf(start, count).collect { uws.emit(it) }
        uws.complete()
    }

    @Test
    fun `unicast 가 취소되면 다음 collector가 연속해서 items 를 받는다`() = runTest {
        val subject = UnicastWorkSubject<Int>()

        coroutineScope {
            generateInts(subject, 1, 15)

            // emit 1..5
            val result1 = subject.take(5).log("#1").toFastList()
            result1 shouldBeEqualTo listOf(1, 2, 3, 4, 5)

            // emit 6..10
            val result2 = subject.take(5).log("#2").toFastList()
            result2 shouldBeEqualTo listOf(6, 7, 8, 9, 10)

            // emit 11..15
            val result3 = subject.take(5).log("#3").toFastList()
            result3 shouldBeEqualTo listOf(11, 12, 13, 14, 15)
        }
    }

    @Test
    fun `offline - after complete`() = runTest {
        val us = UnicastWorkSubject<Int>()
        repeat(5) {
            us.emit(it)
        }
        us.complete()
        us.log("#1").toFastList() shouldBeEqualTo expectedList
    }

    @Test
    fun `offline - after error`() = runTest {
        val us = UnicastWorkSubject<Int>()
        repeat(5) {
            us.emit(it)
        }
        us.emitError(RuntimeException("Boom!"))
        us.emit(6)

        val result = us
            .catch { it shouldBeInstanceOf RuntimeException::class }
            .log("#1")
            .toFastList()
        result shouldBeEqualTo listOf(0, 1, 2, 3, 4, 6)
    }

    @Test
    fun `offline - complete 된 flow 에 대해서 다시 collect 를 수행한다`() = runTest {
        val us = UnicastWorkSubject<Int>()
        repeat(5) {
            us.emit(it)
        }
        us.complete()

        val result = us.take(3).log("#1").toFastList()
        result shouldBeEqualTo listOf(0, 1, 2)

        val result2 = us.log("#2").toFastList()
        result2 shouldBeEqualTo listOf(3, 4)
    }

    @Test
    fun `offline - complete 된 unicast subject 로부터 n 개만 take 한다`() = runTest {
        val us = UnicastWorkSubject<Int>()
        repeat(5) {
            us.emit(it)
        }
        us.complete()

        val result = us.take(3).log("#1").toFastList()
        result shouldBeEqualTo listOf(0, 1, 2)
    }

    @Test
    fun `online - basic usage`() = runTest {
        withSingleThread {
            val us = UnicastWorkSubject<Int>()

            launch {
                us.awaitCollector()

                repeat(5) {
                    us.emit(it)
                }
                us.complete()
            }.log("job")
            yield()

            val result = us.log("#1").toFastList()
            result shouldBeEqualTo expectedList
        }
    }

    @Test
    fun `online - 아주 많은 요소를 emit 하기`() = runTest {
        withSingleThread {
            val us = UnicastWorkSubject<Int>()

            launch {
                us.awaitCollector()

                repeat(BUFFER_SIZE) {
                    us.emit(it)
                }
                us.complete()
            }

            var result = 0
            us.collect { result++ }
            result shouldBeEqualTo BUFFER_SIZE
        }
    }

    @Test
    fun `online - 몇 개의 요소만 take 하기`() = runTest {
        withSingleThread {
            val us = UnicastWorkSubject<Int>()

            launch {
                us.awaitCollector()

                repeat(BUFFER_SIZE) {
                    us.emit(it)
                }
                us.complete()
            }

            var result = 0
            us.take(BUFFER_SIZE / 2).collect { result++ }
            result shouldBeEqualTo BUFFER_SIZE / 2
        }
    }

    @Test
    fun `online - chunk 로 쪼개서 collect 하기`() = runTest {
        withSingleThread {
            val us = UnicastWorkSubject<Int>()

            launch {
                us.awaitCollector()

                repeat(BUFFER_SIZE) {
                    us.emit(it)
                }
                us.complete()
            }

            var result = 0
            while (result < BUFFER_SIZE) {
                us.take(500).collect { result++ }
            }
            result shouldBeEqualTo BUFFER_SIZE
        }
    }
}
