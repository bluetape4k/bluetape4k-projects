package io.bluetape4k.benchmark.webframework

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class WebFrameworkBenchmarkLifecycleTest {

    @Test
    fun `partial startup closes the first resource and preserves cleanup failure`() {
        val first = FakeCloseable(closeFailure = IllegalStateException("first close"))
        val startupFailure = IllegalArgumentException("second startup")

        val thrown = runCatching {
            BenchmarkServerLifecycle.start(
                firstFactory = { first },
                secondFactory = { throw startupFailure },
                combine = { _, _ -> Unit },
            )
        }.exceptionOrNull()

        assertSame(startupFailure, thrown)
        assertEquals(1, startupFailure.suppressed.size)
        assertSame(first.closeFailure, startupFailure.suppressed.single())
        assertEquals(1, first.closeCalls)
    }

    @Test
    fun `dual close attempts both resources and reports the first failure with the second suppressed`() {
        val firstFailure = IllegalStateException("first close")
        val secondFailure = IllegalArgumentException("second close")
        val first = FakeCloseable(closeFailure = firstFailure)
        val second = FakeCloseable(closeFailure = secondFailure)

        val thrown = runCatching {
            BenchmarkServerLifecycle.closeAll(first, second)
        }.exceptionOrNull()

        assertSame(firstFailure, thrown)
        assertEquals(1, first.closeCalls)
        assertEquals(1, second.closeCalls)
        assertEquals(1, firstFailure.suppressed.size)
        assertSame(secondFailure, firstFailure.suppressed.single())
    }

    private class FakeCloseable(
        val closeFailure: Throwable? = null,
    ): AutoCloseable {
        var closeCalls: Int = 0

        override fun close() {
            closeCalls++
            closeFailure?.let { throw it }
        }
    }
}
