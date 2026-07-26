package io.bluetape4k.redis.lettuce.coordination.internal

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.nanoseconds

class CoordinationDeadlineTest {

    @Test
    fun `deadline uses injected monotonic time and rounds remaining milliseconds up`() = runTest {
        val ticker = MutableTicker(1_000_000L)
        val deadline = CoordinationDeadline.after(1_500.microseconds, ticker)

        deadline.remainingMillisCeil() shouldBeEqualTo 2L
        deadline.isExpired().shouldBeFalse()

        ticker.advance(1_499_999L)
        deadline.remainingMillisCeil() shouldBeEqualTo 1L
        deadline.isExpired().shouldBeFalse()

        ticker.advance(1L)
        deadline.remainingMillisCeil() shouldBeEqualTo 0L
        deadline.isExpired().shouldBeTrue()
    }

    @Test
    fun `deadline saturates addition instead of overflowing`() = runTest {
        val ticker = MutableTicker(Long.MAX_VALUE - 5L)
        val deadline = CoordinationDeadline.after(10.nanoseconds, ticker)

        deadline.expiresAtNanos shouldBeEqualTo Long.MAX_VALUE
        deadline.remainingNanos() shouldBeEqualTo 5L
    }

    @Test
    fun `deadline rejects non-positive and infinite durations`() {
        val ticker = MutableTicker()

        assertFailsWith<IllegalArgumentException> { CoordinationDeadline.after(Duration.ZERO, ticker) }
        assertFailsWith<IllegalArgumentException> { CoordinationDeadline.after((-1).nanoseconds, ticker) }
        assertFailsWith<IllegalArgumentException> { CoordinationDeadline.after(Duration.INFINITE, ticker) }
    }

    @Test
    fun `duration conversion rounds up without addition overflow`() {
        1.nanoseconds.toPositiveMillisCeil() shouldBeEqualTo 1L
        1_000_000.nanoseconds.toPositiveMillisCeil() shouldBeEqualTo 1L
        1_000_001.nanoseconds.toPositiveMillisCeil() shouldBeEqualTo 2L
        Long.MAX_VALUE.nanoseconds.toPositiveMillisCeil() shouldBeEqualTo 9_223_372_036_854L
    }

    private class MutableTicker(private var nowNanos: Long = 0L): MonotonicTicker {
        override fun readNanos(): Long = nowNanos

        fun advance(deltaNanos: Long) {
            nowNanos += deltaNanos
        }
    }
}
