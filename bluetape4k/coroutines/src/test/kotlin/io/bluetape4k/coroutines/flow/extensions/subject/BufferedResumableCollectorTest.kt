package io.bluetape4k.coroutines.flow.extensions.subject

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.coroutines.support.log
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.junit5.coroutines.withSingleThread
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Duration.Companion.seconds

class BufferedResumableCollectorTest {

    companion object: KLoggingChannel()

    @Test
    fun `capacity 만큼 버퍼링을 합니다`() = runTest {
        val bc = BufferedResumableCollector<Int>(32)
        val n = 10_000
        val counter = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                repeat(n) {
                    bc.next(it)
                }
                bc.complete()
            }.log("job")

            yield()

            val collector = FlowCollector<Int> {
                counter.incrementAndGet()
            }

            bc.drain(collector)
            job.join()
        }
        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `basic long operation with one capacity`() = runTest {
        val bc = BufferedResumableCollector<Int>(1)
        val n = 10_000
        val counter = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                repeat(n) {
                    bc.next(it)
                }
                bc.complete()
            }.log("job")
            yield()

            val collector = FlowCollector<Int> {
                counter.incrementAndGet()
            }

            bc.drain(collector)
            job.join()
        }
        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `basic long operations with 64 capacity`() = runTest {
        val bc = BufferedResumableCollector<Int>(64)
        val n = 100_000
        val counter = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                repeat(n) {
                    bc.next(it)
                }
                bc.complete()
            }.log("job")
            yield()

            val collector = FlowCollector<Int> { counter.incrementAndGet() }
            bc.drain(collector)

            job.join()
        }
        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `basic long operations with 256 capacity`() = runTest {
        val bc = BufferedResumableCollector<Int>(256)
        val n = 100_000
        val counter = AtomicInteger(0)

        withSingleThread { dispatcher ->
            val job = launch(dispatcher) {
                repeat(n) {
                    bc.next(it)
                }
                bc.complete()
            }.log("job")
            yield()

            val collector = FlowCollector<Int> { counter.incrementAndGet() }
            bc.drain(collector)
            job.join()
        }
        counter.get() shouldBeEqualTo n
    }

    @Test
    fun `suspended concurrent producers drain all values with small capacity`() =
        runSuspendDefault(timeout = 10.seconds) {
            val bc = BufferedResumableCollector<Int>(1)
            val producerWorkers = 8
            val rounds = 512
            val expectedValues = (1..rounds).toList()
            val expectedCount = expectedValues.size
            val produced = AtomicInteger(0)
            val received = mutableListOf<Int>()

            val producerJob = launch {
                SuspendedJobTester()
                    .workers(producerWorkers)
                    .rounds(rounds)
                    .add {
                        bc.next(produced.incrementAndGet())
                    }
                    .run()
                bc.complete()
            }.log("producerJob")

            yield()

            val collector = FlowCollector<Int> {
                received += it
            }
            bc.drain(collector)
            producerJob.join()

            produced.get() shouldBeEqualTo expectedCount
            received.sorted() shouldBeEqualTo expectedValues
        }

    @Test
    fun `suspended concurrent producers drain all values with buffered capacity`() =
        runSuspendDefault(timeout = 10.seconds) {
            val bc = BufferedResumableCollector<Int>(64)
            val producerWorkers = 8
            val rounds = 1024
            val expectedValues = (1..rounds).toList()
            val expectedCount = expectedValues.size
            val produced = AtomicInteger(0)
            val received = mutableListOf<Int>()

            val producerJob = launch {
                SuspendedJobTester()
                    .workers(producerWorkers)
                    .rounds(rounds)
                    .add {
                        bc.next(produced.incrementAndGet())
                    }
                    .run()
                bc.complete()
            }.log("producerJob")

            yield()

            val collector = FlowCollector<Int> {
                received += it
            }
            bc.drain(collector)
            producerJob.join()

            produced.get() shouldBeEqualTo expectedCount
            received.sorted() shouldBeEqualTo expectedValues
        }

    @Test
    fun `complete는 이미 적재된 값을 drain한 뒤 suspend producer를 거부한다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val bc = BufferedResumableCollector<Int>(1)
            bc.next(1)

            val producerResult = async(start = CoroutineStart.UNDISPATCHED) {
                runCatching { bc.next(2) }
            }

            bc.complete()
            val producerError = producerResult.await().exceptionOrNull()

            val received = mutableListOf<Int>()
            bc.drain(FlowCollector { received += it })

            received shouldBeEqualTo listOf(1)
            producerError.shouldBeInstanceOf<IllegalStateException>()
            runCatching { bc.next(3) }.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
        }

    @Test
    fun `terminal이 offer commit보다 먼저면 값을 enqueue하지 않는다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val beforeOffer = CompletableDeferred<Unit>()
            val releaseOffer = CompletableDeferred<Unit>()
            val bc = BufferedResumableCollector.forTest<Int>(
                capacity = 1,
                beforeOfferCommit = {
                    beforeOffer.complete(Unit)
                    releaseOffer.await()
                },
            )

            val producerResult = async(start = CoroutineStart.UNDISPATCHED) {
                runCatching { bc.next(1) }
            }
            beforeOffer.await()
            bc.complete()
            releaseOffer.complete(Unit)

            producerResult.await().exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
            val received = mutableListOf<Int>()
            bc.drain(FlowCollector { received += it })
            received shouldBeEqualTo emptyList()
        }

    @Test
    fun `error가 offer commit보다 먼저면 값을 거부하고 같은 error를 전파한다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val expectedError = IllegalStateException("terminal before offer commit")
            val beforeOffer = CompletableDeferred<Unit>()
            val releaseOffer = CompletableDeferred<Unit>()
            val bc = BufferedResumableCollector.forTest<Int>(
                capacity = 1,
                beforeOfferCommit = {
                    beforeOffer.complete(Unit)
                    releaseOffer.await()
                },
            )

            val producerResult = async(start = CoroutineStart.UNDISPATCHED) {
                runCatching { bc.next(1) }
            }
            beforeOffer.await()
            bc.error(expectedError)
            releaseOffer.complete(Unit)

            producerResult.await().exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
            val drainResult = runCatching { bc.drain(FlowCollector {}) }
            drainResult.exceptionOrNull() shouldBeSameInstanceAs expectedError
        }

    @Test
    fun `실제 offer가 error보다 먼저면 수락한 값을 drain하고 같은 error를 전파한다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val expectedError = IllegalStateException("terminal after offer")
            lateinit var bc: BufferedResumableCollector<Int>
            bc = BufferedResumableCollector.forTest(
                capacity = 1,
                afterOffer = {
                    bc.error(expectedError)
                    bc.complete()
                },
            )

            val producerResult = runCatching { bc.next(1) }

            producerResult.exceptionOrNull() shouldBeEqualTo null
            val received = mutableListOf<Int>()
            val drainResult = runCatching { bc.drain(FlowCollector { received += it }) }
            received shouldBeEqualTo listOf(1)
            drainResult.exceptionOrNull() shouldBeSameInstanceAs expectedError
        }

    @Test
    fun `committed offer의 pending terminal은 새 admission을 즉시 거부한다`() =
        runSuspendDefault(timeout = 5.seconds) {
            lateinit var bc: BufferedResumableCollector<Int>
            lateinit var rejectedProducer: kotlinx.coroutines.Deferred<Result<Unit>>
            bc = BufferedResumableCollector.forTest(
                capacity = 1,
                afterOffer = {
                    bc.complete()
                    rejectedProducer = async(start = CoroutineStart.UNDISPATCHED) {
                        runCatching { bc.next(2) }
                    }
                },
            )

            bc.next(1)

            rejectedProducer.await().exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
            val received = mutableListOf<Int>()
            bc.drain(FlowCollector { received += it })
            received shouldBeEqualTo listOf(1)
        }

    @Test
    fun `첫 error만 기록하고 buffered 값을 drain한 뒤 전파한다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val bc = BufferedResumableCollector<Int>(1)
            val expectedError = IllegalStateException("first terminal error")
            bc.next(1)

            val producerResult = async(start = CoroutineStart.UNDISPATCHED) {
                runCatching { bc.next(2) }
            }

            bc.error(expectedError)
            bc.complete()
            bc.error(IllegalArgumentException("ignored terminal error"))
            val producerError = producerResult.await().exceptionOrNull()

            val received = mutableListOf<Int>()
            val drainResult = runCatching {
                bc.drain(FlowCollector { received += it })
            }

            received shouldBeEqualTo listOf(1)
            drainResult.exceptionOrNull() shouldBeSameInstanceAs expectedError
            producerError.shouldBeInstanceOf<IllegalStateException>()
        }

    @Test
    fun `complete와 null error도 첫 terminal 상태를 유지한다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val ignoredError = IllegalStateException("ignored terminal error")

            lateinit var completed: BufferedResumableCollector<Int>
            completed = BufferedResumableCollector.forTest(
                capacity = 1,
                afterOffer = {
                    completed.complete()
                    completed.error(null)
                    completed.error(ignoredError)
                },
            )
            completed.next(1)
            val completedValues = mutableListOf<Int>()
            val completeResult = runCatching {
                completed.drain(FlowCollector { completedValues += it })
            }

            completedValues shouldBeEqualTo listOf(1)
            completeResult.exceptionOrNull() shouldBeEqualTo null

            lateinit var nullError: BufferedResumableCollector<Int>
            nullError = BufferedResumableCollector.forTest(
                capacity = 1,
                afterOffer = {
                    nullError.error(null)
                    nullError.complete()
                    nullError.error(ignoredError)
                },
            )
            nullError.next(2)
            val nullErrorValues = mutableListOf<Int>()
            val nullErrorResult = runCatching {
                nullError.drain(FlowCollector { nullErrorValues += it })
            }

            nullErrorValues shouldBeEqualTo listOf(2)
            nullErrorResult.exceptionOrNull() shouldBeEqualTo null
        }

    @Test
    fun `작은 버퍼의 다수 producer와 complete 경합에서 성공한 값만 모두 drain한다`() =
        runSuspendDefault(timeout = 10.seconds) {
            repeat(64) { round ->
                val bc = BufferedResumableCollector<Int>(1)
                val successful = Collections.synchronizedList(mutableListOf<Int>())
                val received = mutableListOf<Int>()

                val producers = (0 until 8).map { producer ->
                    async(start = CoroutineStart.UNDISPATCHED) {
                        val value = round * 8 + producer
                        runCatching { bc.next(value) }
                            .onSuccess { successful += value }
                    }
                }

                bc.complete()
                bc.drain(FlowCollector { received += it })
                val producerResults = producers.awaitAll()

                received.sorted() shouldBeEqualTo successful.sorted()
                producerResults
                    .filter { it.isFailure }
                    .forEach { result ->
                        result.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
                    }
            }
        }

    @Test
    fun `drain 취소는 suspend producer와 이후 next를 CancellationException으로 종료한다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val bc = BufferedResumableCollector<Int>(1)
            val emitStarted = CompletableDeferred<Unit>()
            bc.next(1)

            val drainJob = launch {
                bc.drain(FlowCollector {
                    emitStarted.complete(Unit)
                    awaitCancellation()
                })
            }
            emitStarted.await()

            bc.next(2)
            val producerResult = async(start = CoroutineStart.UNDISPATCHED) {
                runCatching { bc.next(3) }
            }

            drainJob.cancelAndJoin()

            producerResult.await().exceptionOrNull().shouldBeInstanceOf<CancellationException>()
            runCatching { bc.next(4) }.exceptionOrNull().shouldBeInstanceOf<CancellationException>()
        }

    @Test
    fun `값을 기다리는 drain 취소도 이후 producer에 전파한다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val bc = BufferedResumableCollector<Int>(1)
            val drainJob = launch(start = CoroutineStart.UNDISPATCHED) {
                bc.drain(FlowCollector {})
            }

            drainJob.cancelAndJoin()

            runCatching { bc.next(1) }.exceptionOrNull().shouldBeInstanceOf<CancellationException>()
        }

    @Test
    fun `terminal과 buffered 값이 있어도 취소된 drain은 값을 emit하지 않는다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val bc = BufferedResumableCollector<Int>(1)
            val emitted = AtomicInteger(0)
            val drainError = AtomicReference<Throwable?>()
            bc.next(1)
            bc.complete()

            val drainJob = launch(start = CoroutineStart.UNDISPATCHED) {
                cancel(CancellationException("cancel before drain"))
                drainError.set(
                    runCatching {
                        bc.drain(FlowCollector { emitted.incrementAndGet() })
                    }.exceptionOrNull(),
                )
            }
            drainJob.join()

            emitted.get() shouldBeEqualTo 0
            drainError.get().shouldBeInstanceOf<CancellationException>()
            runCatching { bc.next(2) }.exceptionOrNull().shouldBeInstanceOf<IllegalStateException>()
        }

    @Test
    fun `collector 실패는 같은 원인을 전파하고 suspend producer를 cause 보존 취소한다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val bc = BufferedResumableCollector<Int>(1)
            val expectedError = IllegalStateException("collector failed")
            bc.next(1)

            val producerResult = async(start = CoroutineStart.UNDISPATCHED) {
                runCatching { bc.next(2) }
            }
            val drainResult = runCatching {
                bc.drain(FlowCollector { throw expectedError })
            }

            drainResult.exceptionOrNull() shouldBeSameInstanceAs expectedError
            val producerError = producerResult.await().exceptionOrNull()
                .shouldBeInstanceOf<CancellationException>()
            producerError.cause shouldBeSameInstanceAs expectedError
        }

    @Test
    fun `capacity 대기 중 producer 취소는 값을 enqueue하지 않는다`() =
        runSuspendDefault(timeout = 5.seconds) {
            val bc = BufferedResumableCollector<Int>(1)
            bc.next(1)

            val producerJob = launch(start = CoroutineStart.UNDISPATCHED) {
                bc.next(2)
            }
            producerJob.cancelAndJoin()
            bc.complete()

            val received = mutableListOf<Int>()
            bc.drain(FlowCollector { received += it })

            received shouldBeEqualTo listOf(1)
        }
}
