package io.bluetape4k.concurrent.virtualthread.jdk25

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.time.Instant
import java.util.concurrent.TimeoutException
import kotlin.test.assertFailsWith

@EnabledOnJre(JRE.JAVA_25)
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
}
