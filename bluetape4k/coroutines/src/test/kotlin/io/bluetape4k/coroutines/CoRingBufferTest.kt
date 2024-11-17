package io.bluetape4k.coroutines

import io.bluetape4k.coroutines.flow.extensions.bufferedSliding
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CoRingBufferTest {

    companion object: KLogging()

    @Test
    fun `push more items than size, reset to 0`() = runTest {
        val buffer = CoRingBuffer(20, Double.NaN)

        // size가 20 이므로, 11 ~ 30 까지의 숫자를 더한 값을 가진다.
        for (i in 1..30) {
            buffer.push(i.toDouble())
        }
        buffer.sumOf { it!! } shouldBeEqualTo 410.0

        buffer.forEach {
            log.debug { it }
        }
    }

    @Test
    fun `push items in multi-jobs`() = runTest {
        val buffer = CoRingBuffer(16, Double.NaN)
        val counter = atomic(0)

        MultijobTester()
            .numThreads(8)
            .roundsPerJob(2)
            .add {
                buffer.push(counter.incrementAndGet().toDouble())
            }
            .run()

        buffer.toList().sortedBy { it } shouldBeEqualTo List(16) { (it + 1).toDouble() }
    }

    @Test
    fun `windowed ring buffer`() = runTest {
        val flow = flow {
            var i = 0
            while (true) emit(i++)
        }

        val windowed: Flow<List<Int>> = flow.bufferedSliding(10)

        // flow를 중복 사용할 시에 초기 설정을 유지하는지 확인하기 위해 사용해본다.
        windowed.take(1).single()

        val avgs = windowed.take(15)
            .map {
                log.debug { "windowed: $it" }
                it.average()
            }.toList()

        avgs.forEachIndexed { index, avg ->
            log.debug { "avgs[$index]=$avg" }
        }
        avgs[0] shouldBeEqualTo 0.0
        avgs[9] shouldBeEqualTo 4.5
        avgs[14] shouldBeEqualTo 9.5
    }
}
