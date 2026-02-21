package io.bluetape4k.concurrent.virtualthread.jdk25

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class Jdk25StructuredTaskScopeProviderTest {

    private val provider = Jdk25StructuredTaskScopeProvider()

    @Test
    fun `withAll success`() {
        val result = provider.withAll(factory = Thread.ofVirtual().factory()) { scope ->
            val a = scope.fork { 10 }
            val b = scope.fork { 20 }
            scope.join().throwIfFailed()
            a.get() + b.get()
        }

        assertEquals(30, result)
    }

    @Test
    fun `withAll failure should throw`() {
        assertThrows(ExecutionException::class.java) {
            provider.withAll(factory = Thread.ofVirtual().factory()) { scope ->
                scope.fork { 1 }
                scope.fork<Int> { throw IllegalArgumentException("boom") }
                scope.join().throwIfFailed()
                0
            }
        }
    }

    @Test
    fun `withAny should return first success`() {
        val result = provider.withAny<String>(factory = Thread.ofVirtual().factory()) { scope ->
            scope.fork {
                Thread.sleep(80)
                "slow"
            }
            scope.fork {
                Thread.sleep(10)
                "fast"
            }
            scope.join().result { IllegalStateException(it) }
        }

        assertEquals("fast", result)
    }
}
