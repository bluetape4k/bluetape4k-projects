package io.bluetape4k.redis.lettuce.coordination.internal

import kotlin.time.Duration

internal fun interface MonotonicTicker {
    fun readNanos(): Long

    companion object {
        val SYSTEM: MonotonicTicker = MonotonicTicker(System::nanoTime)
    }
}

internal class CoordinationDeadline private constructor(
    val expiresAtNanos: Long,
    private val ticker: MonotonicTicker,
) {
    fun remainingNanos(): Long {
        val now = ticker.readNanos()
        if (now >= expiresAtNanos) {
            return 0L
        }
        val remaining = expiresAtNanos - now
        return if (remaining < 0L) Long.MAX_VALUE else remaining
    }

    fun remainingMillisCeil(): Long {
        val remaining = remainingNanos()
        if (remaining == 0L) {
            return 0L
        }
        return remaining / NANOS_PER_MILLI + if (remaining % NANOS_PER_MILLI == 0L) 0L else 1L
    }

    fun isExpired(): Boolean = remainingNanos() == 0L

    companion object {
        fun after(
            timeout: Duration,
            ticker: MonotonicTicker = MonotonicTicker.SYSTEM,
        ): CoordinationDeadline {
            require(timeout.isFinite() && timeout.isPositive()) {
                "timeout must be positive and finite"
            }
            val timeoutNanos = timeout.inWholeNanoseconds
            require(timeoutNanos > 0L) { "timeout must be at least one nanosecond" }
            val now = ticker.readNanos()
            val expiresAt = if (now > Long.MAX_VALUE - timeoutNanos) Long.MAX_VALUE else now + timeoutNanos
            return CoordinationDeadline(expiresAt, ticker)
        }
    }
}

internal fun Duration.toPositiveMillisCeil(): Long {
    require(isFinite() && isPositive()) { "duration must be positive and finite" }
    val nanos = inWholeNanoseconds
    require(nanos > 0L) { "duration must be at least one nanosecond" }
    return nanos / NANOS_PER_MILLI + if (nanos % NANOS_PER_MILLI == 0L) 0L else 1L
}

private const val NANOS_PER_MILLI = 1_000_000L
