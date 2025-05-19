package io.bluetape4k.coroutines.flow.extensions

import app.cash.turbine.test
import io.bluetape4k.coroutines.tests.assertEmpty
import io.bluetape4k.coroutines.tests.assertResult
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Kotlinx Coroutines 에서 공식적으로 제공하는
 *
 * [kotlinx.coroutines.flow.debounce], [kotlinx.coroutines.flow.sample]과 유사한 기능을 제공하는 것이
 * [throttleLeading], [throttleTrailing] 이다.
 *
 * [kotlinx.coroutines.flow.sample]은 [ThrottleBehavior.TRAILING] 과 유사하게 Timer 발생 시 가장 최근에 방출된 요소를 방출한다.
 */
class ThrottleTest: AbstractFlowTest() {

    companion object: KLoggingChannel()

    @Test
    fun `compare throttling methods`() = runTest {
        //-----1-----2-----3-----4-----5-----6-----7-----8-----9-----10
        //--------------|--------------|----------------|--------------|
        flowRangeOf(1, 10)
            .onEach { delay(200) }.log("source")
            .debounce(501L).log("debounce")
            .assertResult(10)                       // debounce 는 timeout 안에 emit 되는 값이 있다면 overwrite 한다

        flowRangeOf(1, 10)
            .onEach { delay(200) }.log("source")
            .sample(501L).log("sample")
            .assertResult(2, 5, 7)

        flowRangeOf(1, 10)
            .onEach { delay(200) }.log("source")
            .throttleLeading(501L).log("leading")
            .assertResult(1, 4, 7, 10)

        flowRangeOf(1, 10)
            .onEach { delay(200) }.log("source")
            .throttleTrailing(501L).log("trailing")
            .assertResult(3, 6, 9, 10)
    }

    @Nested
    inner class Debounce {

        @Test
        fun `debounce flow items`() = runTest {
            flow {
                emit(1)
                delay(90)
                emit(2)
                delay(90)
                emit(3)             // emit
                delay(1010)
                emit(4)             // emit
                delay(1010)
                emit(5)             // emit
            }
                .log("source")
                .debounce(1000).log("debounce")
                .assertResult(3, 4, 5)

            flow {
                emit(1)
                delay(90)
                emit(2)
                delay(90)
                emit(3)             // emit
                delay(1010)
                emit(4)             // emit
                delay(1010)
                emit(5)             // not emit !!! in sample
            }
                .log("source")
                .sample(1000).log("sample")
                .assertResult(3, 4)
        }

        @Test
        fun `debounce flow item by timer`() = runTest {
            //-----1-----2-----3-----4-----5
            //---------|---------|---------|
            flow {
                repeat(5) {
                    emit(it + 1)
                    delay(it * 300L)
                }
            }
                .log("source")
                .debounce(501).log("debounce")          // debounce 는 timeout 이 호출되면, 기존 값은 모두 버린다.
                .assertResult(3, 4, 5)

            //-----1-----2-----3-----4-----5
            //---------|---------|---------|
            flow {
                repeat(5) {
                    emit(it + 1)
                    delay(300)
                }
            }.log("source")
                .sample(501).log("sample")
                .assertResult(2, 4)
        }
    }

    @Nested
    inner class Sample {

        @Test
        fun `sampling flow item by timer`() = runTest {
            //-----1-----2-----3-----4-----5-----6-----7-----8-----9-----10
            //--------------|--------------|----------------|--------------|
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .sample(501L).log("sample")
                .assertResult(2, 5, 7)
        }

        @Test
        fun `sample flow item`() = runTest {
            flow {
                emit(1)             // skip
                emit(2)             // deliver
                delay(501)     // 501

                emit(3)             // skip
                delay(99)      // 600

                emit(4)             // skip
                delay(100)     // 700

                emit(5)             // skip
                emit(6)             // deliver
                delay(301)     // 1001

                emit(7)             // deliver
                delay(500)     // 1501
            }
                .log("source")
                .sample(500).log("sample")
                .assertResult(2, 6, 7)
        }
    }

    @Nested
    inner class ThrottleLeading {

        @Test
        fun `throttle flow item`() = runTest {
            flow {
                emit(1)             // deliver
                emit(2)             // skip
                delay(501)     // 501

                emit(3)             // deliver
                delay(99)      // 600

                emit(4)             // skip
                delay(100)     // 700

                emit(5)             // skip
                emit(6)             // skip
                delay(301)     // 1001

                emit(7)             // deliver
                delay(500)     // 1501
            }
                .log("source")
                .throttleLeading(500).log("leading")
                .assertResult(1, 3, 7)
        }

        @Test
        fun `throttle leading with completed flow`() = runTest {
            //-----1-----2-----3-----4-----5-----6-----7-----8-----9-----10
            //--------------|--------------|----------------|-------------|

            // 1 - deliver (200)
            // 2 - skip    (400)
            // ---------------------------- 501
            // 3 - skip    (600)
            // 4 - deliver (800)
            // ---------------------------- 1002
            // 5 - skip    (1000)
            // 6 - skip    (1200)
            // 7 - deliver (1400)
            // ---------------------------- 1503
            // 8 - skip    (1600)
            // 9 - skip    (1800)
            // 10 - deliver (2000)
            // ---------------------------- 2004
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleLeading(501).log("leading")
                .assertResult(1, 4, 7, 10)
        }

        @Test
        fun `throttle with complete and no delay`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleLeading(0).log("leading")
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        @Test
        fun `throttle with complete and no duration`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleLeading { Duration.ZERO }.log("leading")
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        @Test
        fun `throttle with complete and no time`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleLeading(0L).log("leading")
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        @Test
        fun `throttle with null value`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .map { it.takeIf { it % 2 == 0 } }.log("leading")
                .throttleLeading(500L)
                .assertResult(null, 4, null, 10)  // 1, 4, 7, 10
        }

        @Test
        fun `throttle single flow`() = runTest {
            flowOf(1).log("source")
                .throttleLeading(100).log("leading")
                .assertResult(1)
        }

        @Test
        fun `throttle empty flow`() = runTest {
            emptyFlow<Int>().log("source")
                .throttleLeading(100).log("leading")
                .assertEmpty()
        }

        @Test
        fun `throttle never flow`() = runTest {
            var hasValue = false

            val job = neverFlow().log("source")
                .throttleLeading(100).log("leading")
                .onEach { hasValue = true }
                .launchIn(this)

            advanceTimeBy(1000)
            job.cancel()
            hasValue.shouldBeFalse()
        }

        @Test
        fun `throttle with failure upstream`() = runTest {
            flow {
                emit(1)
                throw RuntimeException("Boom!")
            }
                .log("source")
                .throttleLeading(100).log("leading")
                .test {
                    awaitError()
                }

            flow {
                emit(1)
                delay(200)
                emit(2)
                delay(200)
                throw RuntimeException("Boom!")
            }
                .log("source")
                .throttleLeading(100).log("leading")
                .test {
                    awaitItem() shouldBeEqualTo 1
                    awaitItem() shouldBeEqualTo 2
                    awaitError()
                }

            flow {
                emit(1)             // Should be published since it is first
                delay(100)
                emit(2)              // Should be skipped since error will arrive before the timeout expires
                delay(100)
                throw RuntimeException("Boom!")
            }
                .log("source")
                .throttleLeading(500).log("leading")
                .test {
                    awaitItem() shouldBeEqualTo 1
                    awaitError()
                }
        }

        @Test
        fun `throttle with timer which raise exception`() = runTest {
            flowRangeOf(1, 10)
                .log("source")
                .throttleLeading { throw RuntimeException("Boom!") }.log("leading")
                .test {
                    awaitItem() shouldBeEqualTo 1
                    awaitError()
                }

            flow {
                emit(1)
                delay(100)
                emit(2)
                delay(400)
                emit(3)
            }.log("source")
                .throttleLeading {
                    when (it) {
                        1    -> 400.milliseconds
                        3    -> throw RuntimeException("first")
                        else -> throw RuntimeException("second")
                    }
                }.log("leading")
                .test {
                    awaitItem() shouldBeEqualTo 1
                    awaitItem() shouldBeEqualTo 3
                    awaitError().message shouldBeEqualTo "first"
                }
        }

        @Test
        fun `throttle take`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleLeading(500).log("leading")
                .take(1)
                .assertResult(1)

            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleLeading { throw RuntimeException("Boom!") }.log("leading")
                .take(1)
                .assertResult(1)
        }

        @Test
        fun `throttle cancellation`() = runTest {
            var count = 0

            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleLeading {
                    if (count++ % 2 == 0) {
                        throw kotlinx.coroutines.CancellationException("$it")
                    } else {
                        500.milliseconds
                    }
                }.log("leading")
                .materialize()
                .test {
                    awaitItem() shouldBeEqualTo FlowEvent.Value(1)
                    awaitItem().errorOrThrow() shouldBeInstanceOf kotlinx.coroutines.CancellationException::class
                    awaitComplete()
                }
        }
    }

    @Nested
    inner class ThrottleTrailing {
        @Test
        fun `throttle flow item A`() = runTest {
            // -1---2----3-
            // -@-----!--@-----!
            // -------2--------3
            flow {
                delay(100)
                emit(1)
                delay(300)      // 400
                emit(2)
                delay(400)      // 800
                emit(3)
                delay(100)      // 900
            }.log("source")
                .throttleTrailing(500).log("trailing")
                .assertResult(2, 3)
        }

        @Test
        fun `throttle flow item B`() = runTest {
            // -1---2----3----4
            // -@-----!--@-----!
            // -------2--------4
            flow {
                delay(100)
                emit(1)
                delay(300)          // 400
                emit(2)
                delay(400)          // 800
                emit(3)
                delay(450)          // 1250
                emit(4)
            }.log("source")
                .throttleTrailing(500).log("trailing")
                .assertResult(2, 4)
        }

        @Test
        fun `throttle flow item C`() = runTest {
            // -1---2----3------4|
            // -@-----!--@-----!
            // -------2--------3 4
            flow {
                delay(100)
                emit(1)
                delay(300)          // 400
                emit(2)
                delay(400)          // 800
                emit(3)
                delay(550)          // 1350
                emit(4)
            }.log("source")
                .throttleTrailing(500).log("trailing")
                .assertResult(2, 3, 4)
        }

        @Test
        fun `throttle with complete and no delay A`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleTrailing(0).log("trailing")
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        @Test
        fun `throttle with complete and no delay B`() = runTest {
            flowRangeOf(1, 10)
                .throttleTrailing(0)
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        @Test
        fun `throttle with complete and no delay C`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(100) }.log("source")
                .throttleTrailing(Duration.ZERO).log("trailing")
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        @Test
        fun `throttle with complete and no delay D`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(100) }.log("source")
                .throttleTrailing { Duration.ZERO }.log("trailing")
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        @Test
        fun `throttle with complete and no delay E`() = runTest {
            flowRangeOf(1, 10).log("source")
                .throttleTrailing { _ -> Duration.ZERO }.log("trailing")
                .assertResult(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        }

        @Test
        fun `throttle with null value`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleTrailing(500L).log("trailing")
                .assertResult(3, 6, 9, 10)

            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .map { it.takeIf { it % 2 == 0 } }.log("even")
                .throttleTrailing(500L).log("trailing")
                .assertResult(null, 6, null, 10)  // 3, 6, 9, 10
        }

        @Test
        fun `throttle single flow`() = runTest {
            flowOf(1).log("source")
                .throttleTrailing(100).log("trailing")
                .assertResult(1)
        }

        @Test
        fun `throttle empty flow`() = runTest {
            emptyFlow<Int>().log("source")
                .throttleTrailing(100).log("trailing")
                .assertEmpty()
        }

        @Test
        fun `throttle never flow`() = runTest {
            var hasValue = false

            val job = neverFlow().log("source")
                .throttleTrailing(100).log("trailing")
                .onEach { hasValue = true }
                .launchIn(this)

            advanceTimeBy(1000)
            job.cancel()
            hasValue.shouldBeFalse()
        }

        @Test
        fun `throttle with failure upstream`() = runTest {
            flow {
                emit(1)
                throw RuntimeException("Boom!")
            }.log("source")
                .throttleTrailing(100).log("trailing")
                .test {
                    awaitError()
                }

            // 1-----2----X
            //  --1   --2
            flow {
                emit(1)
                delay(200)
                emit(2)
                delay(200)
                throw RuntimeException("Boom!")
            }.log("source")
                .throttleTrailing(100).log("trailing")
                .test {
                    awaitItem() shouldBeEqualTo 1
                    awaitItem() shouldBeEqualTo 2
                    awaitError()
                }

            // 1-2-X
            //  ----X
            flow {
                emit(1)
                delay(100)
                emit(2)
                delay(100)
                throw RuntimeException("Boom!")
            }.log("source")
                .throttleTrailing(400).log("trailing")
                .test {
                    awaitError()
                }
        }

        @Test
        fun `throttle with timer which raise exception`() = runTest {
            flowRangeOf(1, 10)
                .throttleTrailing { throw RuntimeException("Boom!") }
                .test {
                    awaitError()
                }

            // 1-2----3
            //  ----2 X
            flow {
                emit(1)
                delay(100)
                emit(2)
                delay(400)
                emit(3)
            }.log("source")
                .throttleTrailing {
                    when (it) {
                        1    -> 400.milliseconds
                        3    -> throw RuntimeException("first")
                        else -> throw RuntimeException("second")
                    }
                }.log("trailing")
                .test {
                    awaitItem() shouldBeEqualTo 2
                    awaitError().message shouldBeEqualTo "first"
                }

            // 1-2----3------4
            //  ----2  -----X
            flow {
                emit(1)
                delay(100)
                emit(2)
                delay(400)
                emit(3)
                delay(600)
                emit(4)
            }.log("source")
                .throttleTrailing {
                    when (it) {
                        1    -> 400.milliseconds
                        3    -> throw RuntimeException("first")
                        else -> throw RuntimeException("second")
                    }
                }.log("trailing")
                .test {
                    awaitItem() shouldBeEqualTo 2
                    awaitError().message shouldBeEqualTo "first"
                }
        }

        @Test
        fun `throttle take`() = runTest {
            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleTrailing(500).log("trailing")
                .take(1)
                .assertResult(3)

            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .concatWith(flow<Int> { throw RuntimeException("Boom!") }.log("concat"))
                .throttleTrailing(500).log("trailing")
                .take(1)
                .assertResult(3)
        }

        @Test
        fun `throttle cancellation`() = runTest {
            // --1--2--3--4--5--6--7--8--9--10
            var count = 1

            flowRangeOf(1, 10)
                .onEach { delay(200) }.log("source")
                .throttleTrailing {
                    if (count++ % 2 == 0) {
                        throw kotlinx.coroutines.CancellationException("$it")
                    } else {
                        500.milliseconds
                    }
                }.log("trailing")
                .test {
                    awaitItem() shouldBeEqualTo 3
                    awaitError() shouldBeInstanceOf kotlinx.coroutines.CancellationException::class
                }
        }
    }
}
