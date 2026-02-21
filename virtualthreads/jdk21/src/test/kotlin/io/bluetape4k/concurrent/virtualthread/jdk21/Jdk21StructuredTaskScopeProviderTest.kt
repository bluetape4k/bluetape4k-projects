package io.bluetape4k.concurrent.virtualthread.jdk21

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException

class Jdk21StructuredTaskScopeProviderTest {

    private val provider = Jdk21StructuredTaskScopeProvider()

    @Test
    fun `withAll success`() {
        val result = provider.withAll(factory = Thread.ofVirtual().factory()) { scope ->
            val a = scope.fork { 1 }
            val b = scope.fork { 2 }
            scope.join().throwIfFailed()
            a.get() + b.get()
        }

        assertEquals(3, result)
    }

    @Test
    fun `withAll failure should throw`() {
        assertThrows(ExecutionException::class.java) {
            provider.withAll(factory = Thread.ofVirtual().factory()) { scope ->
                scope.fork { 1 }
                scope.fork<Int> { throw IllegalStateException("boom") }
                scope.join().throwIfFailed()
                0
            }
        }
    }

    @Test
    fun `withAny should return first success`() {
        val result = provider.withAny<String>(factory = Thread.ofVirtual().factory()) { scope ->
            scope.fork {
                Thread.sleep(50)
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
