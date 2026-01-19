package io.bluetape4k.resilience4j.circuitbreaker

import io.bluetape4k.junit5.coroutines.runSuspendTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.circuitbreaker.circuitBreaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Phaser
import kotlin.test.assertFailsWith

class FlowCircuitBreakerTest {

    companion object: KLoggingChannel()

    @Test
    fun `should collect successfully`() = runSuspendTest {
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val results = CopyOnWriteArrayList<Int>()

        // When
        flow {
            repeat(3) {
                emit(it)
            }
        }
            .circuitBreaker(circuitBreaker)
            .toList(results)

        repeat(3) {
            results[it] shouldBeEqualTo it
        }

        metrics.numberOfBufferedCalls shouldBeEqualTo 1
        metrics.numberOfFailedCalls shouldBeEqualTo 0
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 1
    }

    @Test
    fun `circuit breaker가 open 시에는 flow collect가 되지 않습니다`() = runSuspendTest {
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        circuitBreaker.transitionToOpenState()
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val results = CopyOnWriteArrayList<Int>()

        assertFailsWith<CallNotPermittedException> {
            flow {
                repeat(3) {
                    emit(it)
                }
            }
                .circuitBreaker(circuitBreaker)
                .toList(results)
        }

        results.shouldBeEmpty()

        metrics.numberOfBufferedCalls shouldBeEqualTo 0
        metrics.numberOfFailedCalls shouldBeEqualTo 0
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 0
        metrics.numberOfNotPermittedCalls shouldBeEqualTo 1
    }

    @Test
    fun `circuit breaker가 open 시에는 flow는 시작하지 않습니다`() = runSuspendTest {
        var wasStarted = false
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        circuitBreaker.transitionToOpenState()
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val results = CopyOnWriteArrayList<Int>()

        assertFailsWith<CallNotPermittedException> {
            flow {
                wasStarted = true
                repeat(3) {
                    emit(it)
                }
            }
                .circuitBreaker(circuitBreaker)
                .toList(results)
        }

        wasStarted.shouldBeFalse()
        results.shouldBeEmpty()

        metrics.numberOfBufferedCalls shouldBeEqualTo 0
        metrics.numberOfFailedCalls shouldBeEqualTo 0
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 0
        metrics.numberOfNotPermittedCalls shouldBeEqualTo 1
    }

    @Test
    fun `예외발생 시 기록에 남깁니다`() = runSuspendTest {
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val results = CopyOnWriteArrayList<Int>()

        assertFailsWith<IllegalStateException> {
            flow {
                repeat(6) {
                    if (it == 4) error("failed")
                    emit(it)
                }
            }
                .circuitBreaker(circuitBreaker)
                .toList(results)
        }

        results.size shouldBeEqualTo 4

        metrics.numberOfBufferedCalls shouldBeEqualTo 1
        metrics.numberOfFailedCalls shouldBeEqualTo 1
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 0
        metrics.numberOfNotPermittedCalls shouldBeEqualTo 0
    }

    @Test
    fun `Job 취소 시에는 fail로 기록하지 않습니다`() = runSuspendTest {
        val phaser = Phaser(1)
        var flowCompleted = false
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val job = launch(start = CoroutineStart.ATOMIC) {
            flow {
                phaser.arrive()
                delay(5000L)
                emit(1)
                flowCompleted = true
            }
                .circuitBreaker(circuitBreaker)
                .first()
        }

        phaser.awaitAdvance(1)
        job.cancelAndJoin()

        job.isCompleted.shouldBeTrue()
        job.isCancelled.shouldBeTrue()
        flowCompleted.shouldBeFalse()

        metrics.numberOfBufferedCalls shouldBeEqualTo 0
        metrics.numberOfFailedCalls shouldBeEqualTo 0
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 0
        metrics.numberOfNotPermittedCalls shouldBeEqualTo 0
    }

    @Test
    fun `Job 예외 취소 시에는 fail로 기록하지 않습니다`() = runSuspendTest {
        val parentJob = Job()
        val phaser = Phaser(1)
        var flowCompleted = false
        val circuitBreaker = CircuitBreaker.ofDefaults("testName")
        val metrics = circuitBreaker.metrics
        metrics.numberOfBufferedCalls shouldBeEqualTo 0

        val parentScope = CoroutineScope(parentJob)
        val job = parentScope.launch {
            launch(start = CoroutineStart.ATOMIC) {
                flow {
                    phaser.arrive()
                    delay(5000L)
                    emit(1)
                    flowCompleted = true
                }
                    .circuitBreaker(circuitBreaker)
                    .first()
            }
            error("exceptional cancellation")
        }

        phaser.awaitAdvance(1)
        parentJob.runCatching { join() }

        job.isCompleted.shouldBeTrue()
        job.isCancelled.shouldBeTrue()
        flowCompleted.shouldBeFalse()

        metrics.numberOfBufferedCalls shouldBeEqualTo 0
        metrics.numberOfFailedCalls shouldBeEqualTo 0
        metrics.numberOfSuccessfulCalls shouldBeEqualTo 0
        metrics.numberOfNotPermittedCalls shouldBeEqualTo 0
    }
}
