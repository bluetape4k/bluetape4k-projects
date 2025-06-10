package io.bluetape4k.coroutines

import io.bluetape4k.coroutines.flow.extensions.bufferedSliding
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.random.Random

@Disabled("use SuspendRingBufferTest instead")
class CoRingBufferTest {

    companion object: KLoggingChannel()

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
        val bufferSize = 16
        val buffer = CoRingBuffer(bufferSize, Double.NaN)
        val counter = atomic(0)

        SuspendedJobTester()
            .numThreads(8)
            .roundsPerJob(bufferSize)
            .add {
                delay(Random.nextLong(10))
                buffer.push(counter.incrementAndGet().toDouble())
            }
            .run()

        // CoRingBuffer 의 크기가 16 이므로, 2바퀴 돌아서 17 ~ 32 까지의 숫자를 가진다.
        buffer.toList().sortedBy { it } shouldBeEqualTo List(bufferSize) { (it + 1).toDouble() }
    }

    @Test
    fun `windowed ring buffer`() = runTest {
        val flow = channelFlow {
            var i = 0
            while (true) {
                send(i++)
            }
        }

        /**
         * ```
         * windowed[0]: [0]
         * windowed[1]: [0, 1]
         * windowed[2]: [0, 1, 2]
         * windowed[3]: [0, 1, 2, 3]
         * windowed[4]: [0, 1, 2, 3, 4]
         * windowed[5]: [0, 1, 2, 3, 4, 5]
         * windowed[6]: [0, 1, 2, 3, 4, 5, 6]
         * windowed[7]: [0, 1, 2, 3, 4, 5, 6, 7]
         * windowed[8]: [0, 1, 2, 3, 4, 5, 6, 7, 8]
         * windowed[9]: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9]
         * windowed[10]: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
         * windowed[11]: [2, 3, 4, 5, 6, 7, 8, 9, 10, 11]
         * windowed[12]: [3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
         * windowed[13]: [4, 5, 6, 7, 8, 9, 10, 11, 12, 13]
         * windowed[14]: [5, 6, 7, 8, 9, 10, 11, 12, 13, 14]
         * ```
         */
        val windowed: Flow<List<Int>> = flow.bufferedSliding(10)

        // flow를 중복 사용할 시에 초기 설정을 유지하는지 확인하기 위해 사용해본다.
        windowed.take(1).single()

        var i = 0
        val avgs = windowed
            .take(15)
            .map {
                log.debug { "windowed[$i]: $it" }
                i++
                it.average()
            }
            .toList()

        avgs.forEachIndexed { index, avg ->
            log.debug { "avgs[$index]=$avg" }
        }
        avgs[0] shouldBeEqualTo 0.0
        avgs[9] shouldBeEqualTo 4.5
        avgs[14] shouldBeEqualTo 9.5
    }
}
