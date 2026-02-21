package io.bluetape4k.concurrent.virtualthread.jdk25

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.ExecutionException
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
        assertFailsWith<ExecutionException> {
            provider.withAll { scope ->
                scope.fork { 1 }
                scope.fork<Int> { throw IllegalArgumentException("boom") }
                scope.join().throwIfFailed()
                0
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
