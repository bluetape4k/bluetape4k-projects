package io.bluetape4k.concurrent.virtualthread.jdk25

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledForJreRange
import org.junit.jupiter.api.condition.JRE
import java.time.Instant
import java.util.concurrent.TimeoutException
import io.bluetape4k.assertions.assertFailsWith

@EnabledForJreRange(min = JRE.JAVA_25)
class Jdk25StructuredTaskScopeProviderTest {

    companion object: KLoggingChannel()

    private val provider = Jdk25StructuredTaskScopeProvider()

    @Test
    fun `withAll success`() {
        val result = provider.withAll { scope ->
            val subTask1 = scope.fork {
                log.debug { "Subtask 1 실행" }
                10
            }
            val subTask2 = scope.fork {
                log.debug { "Subtask 2 실행" }
                20
            }
            scope.join().throwIfFailed()

            subTask1.get() + subTask2.get()
        }

        result shouldBeEqualTo 30
    }

    @Test
    fun `withAll failure should throw`() {
        assertFailsWith<IllegalStateException> {
            provider.withAll { scope ->
                scope.fork { 1 }
                scope.fork<Int> { error("boom") }
                scope.join().throwIfFailed()
                0
            }
        }
    }

    @Test
    fun `withSupervised 일부 성공 일부 실패 시 결과를 분리해야 한다`() {
        val (successes, failures) = provider.withSupervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
            scope.fork { 1 }
            scope.fork { throw RuntimeException("fail") }
            scope.fork { 3 }
            scope.join()
            scope.successfulResults() to scope.failedExceptions()
        }
        successes.sorted() shouldBeEqualTo listOf(1, 3)
        failures.size shouldBeEqualTo 1
        failures[0].shouldBeInstanceOf<RuntimeException>()
    }

    @Test
    fun `withSupervised 모두 성공 시 successfulResults 에 전부 포함되어야 한다`() {
        val (successes, failures) = provider.withSupervised<Int, Pair<List<Int>, List<Throwable>>> { scope ->
            scope.fork { 10 }
            scope.fork { 20 }
            scope.join()
            scope.successfulResults() to scope.failedExceptions()
        }
        successes.sorted() shouldBeEqualTo listOf(10, 20)
        failures.size shouldBeEqualTo 0
    }

    @Test
    fun `withSupervised joinUntil 데드라인 초과 시 TimeoutException 이 발생해야 한다`() {
        assertFailsWith<TimeoutException> {
            provider.withSupervised<Int, Unit> { scope ->
                scope.fork { Thread.sleep(10_000); 42 }
                scope.joinUntil(Instant.now().plusMillis(100))
            }
        }
    }

    @Test
    fun `withSupervised results 일부 성공 일부 실패 시 Result 리스트를 반환해야 한다`() {
        val allResults = provider.withSupervised<Int, List<Result<Int>>> { scope ->
            scope.fork { 1 }
            scope.fork { throw RuntimeException("fail") }
            scope.fork { 3 }
            scope.join()
            scope.results()
        }
        allResults.size shouldBeEqualTo 3
        allResults.filter { it.isSuccess }.map { it.getOrThrow() }.sorted() shouldBeEqualTo listOf(1, 3)
        allResults.mapNotNull { it.exceptionOrNull() }.size shouldBeEqualTo 1
    }

    @Test
    fun `withSupervised results nullable T null 성공도 Result success 로 포함되어야 한다`() {
        val allResults = provider.withSupervised<Int?, List<Result<Int?>>> { scope ->
            scope.fork { 1 }
            scope.fork { null }
            scope.fork { 3 }
            scope.join()
            scope.results()
        }
        allResults.size shouldBeEqualTo 3
        allResults.all { it.isSuccess }.shouldBeTrue()
    }

    @Test
    fun `withAll joinUntil 데드라인 초과 시 TimeoutException 이 발생해야 한다`() {
        assertFailsWith<TimeoutException> {
            provider.withAll { scope ->
                scope.fork { Thread.sleep(10_000); 42 }
                scope.joinUntil(Instant.now().plusMillis(100))
                scope.throwIfFailed()
            }
        }
    }

    @Test
    fun `withAll joinUntil 내 데드라인 이전에 완료되면 정상 결과를 반환해야 한다`() {
        val result = provider.withAll { scope ->
            val task = scope.fork { 42 }
            scope.joinUntil(Instant.now().plusSeconds(5)).throwIfFailed()
            task.get()
        }
        result shouldBeEqualTo 42
    }

    @Test
    fun `withAny should return first success`() {
        val result = provider.withAny { scope ->
            scope.fork {
                Thread.sleep(80)
                log.debug { "Slow subtask starting..." }
                "slow"
            }
            scope.fork {
                Thread.sleep(10)
                log.debug { "Fast subtask starting..." }
                "fast"
            }
            scope.join().result { IllegalStateException(it) }
        }

        result shouldBeEqualTo "fast"
    }

    @Test
    fun `withAny joinUntil 데드라인 초과 시 TimeoutException 이 발생해야 한다`() {
        assertFailsWith<TimeoutException> {
            provider.withAny<String> { scope ->
                scope.fork { Thread.sleep(10_000); "slow" }
                scope.joinUntil(Instant.now().plusMillis(100)).result { RuntimeException(it) }
            }
        }
    }

    @Test
    fun `withAny joinUntil 내 데드라인 이전에 완료되면 정상 결과를 반환해야 한다`() {
        val result = provider.withAny<String> { scope ->
            scope.fork { "fast" }
            scope.joinUntil(Instant.now().plusSeconds(5)).result { RuntimeException(it) }
        }
        result shouldBeEqualTo "fast"
    }
}
