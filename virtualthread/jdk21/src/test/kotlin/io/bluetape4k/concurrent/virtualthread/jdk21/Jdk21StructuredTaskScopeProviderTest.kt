package io.bluetape4k.concurrent.virtualthread.jdk21

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import kotlin.test.assertFailsWith

@EnabledOnJre(JRE.JAVA_21)
class Jdk21StructuredTaskScopeProviderTest {

    companion object: KLoggingChannel()

    private val provider = Jdk21StructuredTaskScopeProvider()

    @Test
    fun `withAll success`() {
        val result = provider.withAll { scope ->
            val a = scope.fork {
                log.debug { "Subtask 1 실행" }
                1
            }
            val b = scope.fork {
                log.debug { "Subtask 2 실행" }
                2
            }
            scope.join().throwIfFailed()
            a.get() + b.get()
        }

        result shouldBeEqualTo 3
    }

    @Test
    fun `withAll failure should throw`() {
        assertFailsWith<IllegalStateException> {
            provider.withAll { scope ->
                scope.fork { 1 }
                scope.fork<Int> { throw IllegalStateException("boom") }
                scope.join().throwIfFailed()
                0
            }
        }
    }

    @Test
    fun `withAny should return first success`() {
        val result = provider.withAny { scope ->
            scope.fork {
                Thread.sleep(50)
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
