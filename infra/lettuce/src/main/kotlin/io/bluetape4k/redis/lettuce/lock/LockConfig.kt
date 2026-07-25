package io.bluetape4k.redis.lettuce.lock

import java.io.Serializable
import java.nio.charset.StandardCharsets
import java.time.Duration

/** Common validation and namespace policy shared by every lock object. */
data class LockConfig(
    val namespace: String = "bt4k:coord:v1",
    val hashTag: String? = null,
    val maxReentrantHolds: Int = 10_000,
): Serializable {
    init {
        validateNamespace(namespace)
        hashTag?.let { validateComponent(it, "Lock hash tag", MAX_RESOURCE_COMPONENT_CHARS) }
        require(maxReentrantHolds in 1..MAX_REENTRANT_HOLDS) {
            "Maximum reentrant holds must be between 1 and $MAX_REENTRANT_HOLDS."
        }
    }

    internal fun validateResourceName(name: String): String =
        name.also { validateComponent(it, "Lock resource name", MAX_RESOURCE_COMPONENT_CHARS) }

    internal fun validateDerivedKey(encodedKey: String): String =
        encodedKey.also {
            val byteCount = it.toByteArray(StandardCharsets.UTF_8).size
            require(it.isNotEmpty() && byteCount <= MAX_DERIVED_KEY_BYTES) {
                "Derived Redis key must contain at most $MAX_DERIVED_KEY_BYTES UTF-8 bytes."
            }
        }

    override fun toString(): String =
        "LockConfig(namespace=<redacted>, hashTag=<redacted>, maxReentrantHolds=$maxReentrantHolds)"

    private fun readResolve(): Any = restoreLockSerializedValue("LockConfig") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** FIFO queue bounds for [LettuceFairLock]. */
data class FairLockConfig(
    val lock: LockConfig = LockConfig(),
    val cleanupBatchSize: Int = 64,
    val maxQueueSize: Int = 10_000,
): Serializable {
    init {
        validateQueueBounds(cleanupBatchSize, maxQueueSize)
    }

    private fun readResolve(): Any = restoreLockSerializedValue("FairLockConfig") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Fencing epoch policy for [LettuceFencedLock]. */
data class FencedLockConfig(
    val lock: LockConfig = LockConfig(),
    val epoch: Long,
): Serializable {
    init {
        validateLuaExactPositive(epoch, "Fenced lock epoch")
    }

    override fun toString(): String = "FencedLockConfig(lock=$lock, epoch=<redacted>)"

    private fun readResolve(): Any = restoreLockSerializedValue("FencedLockConfig") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Writer-preference queue and cleanup bounds for [LettuceReadWriteLock]. */
data class ReadWriteLockConfig(
    val lock: LockConfig = LockConfig(),
    val cleanupBatchSize: Int = 64,
    val maxQueueSize: Int = 10_000,
): Serializable {
    init {
        validateQueueBounds(cleanupBatchSize, maxQueueSize)
    }

    private fun readResolve(): Any = restoreLockSerializedValue("ReadWriteLockConfig") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Bounded exponential retry policy for [LettuceSpinLock]. */
data class SpinLockConfig(
    val lock: LockConfig = LockConfig(),
    val initialDelay: Duration = Duration.ofMillis(10),
    val multiplier: Double = 2.0,
    val maxDelay: Duration = Duration.ofSeconds(1),
    val jitterRatio: Double = 0.25,
    val maxAttemptsPerSecond: Int = 100,
): Serializable {
    init {
        requirePositiveDuration(initialDelay, "Spin initial delay")
        requirePositiveDuration(maxDelay, "Spin maximum delay")
        require(initialDelay <= maxDelay) { "Spin initial delay must not exceed maximum delay." }
        require(multiplier.isFinite() && multiplier >= 1.0) {
            "Spin multiplier must be finite and at least 1.0."
        }
        require(jitterRatio.isFinite() && jitterRatio in 0.0..MAX_JITTER_RATIO) {
            "Spin jitter ratio must be finite and between 0.0 and $MAX_JITTER_RATIO."
        }
        require(maxAttemptsPerSecond in 1..MAX_SPIN_ATTEMPTS_PER_SECOND) {
            "Spin attempt rate must be between 1 and $MAX_SPIN_ATTEMPTS_PER_SECOND."
        }
    }

    private fun readResolve(): Any = restoreLockSerializedValue("SpinLockConfig") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

/** Key-count bound for one all-or-nothing [LettuceMultiLock] acquisition. */
data class MultiLockConfig(
    val lock: LockConfig = LockConfig(),
    val maxKeys: Int = MAX_MULTI_LOCK_KEYS,
): Serializable {
    init {
        require(maxKeys in 1..MAX_MULTI_LOCK_KEYS) {
            "Multi-lock maximum keys must be between 1 and $MAX_MULTI_LOCK_KEYS."
        }
    }

    internal fun validateInputKeyCount(keyCount: Int): Int =
        keyCount.also {
            require(it in 1..maxKeys) {
                "Multi-lock input must contain between 1 and $maxKeys keys."
            }
        }

    private fun readResolve(): Any = restoreLockSerializedValue("MultiLockConfig") { copy() }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}

internal fun Duration.toRedisMillisCeil(): Long {
    requirePositiveDuration(this, "Duration")
    return try {
        val wholeMillis = Math.multiplyExact(seconds, MILLIS_PER_SECOND)
        val partialMillis = if (nano == 0) 0L else (nano.toLong() + NANOS_PER_MILLI - 1L) / NANOS_PER_MILLI
        Math.addExact(wholeMillis, partialMillis)
    } catch (_: ArithmeticException) {
        throw IllegalArgumentException("Duration exceeds Redis millisecond range.")
    }
}

internal fun validateFixedLease(leaseTime: Duration) {
    requireDurationInRange(leaseTime, MIN_FIXED_LEASE, MAX_LEASE, "Fixed lease")
    leaseTime.toRedisMillisCeil()
}

internal fun validateWatchdogLease(
    ttl: Duration,
    renewalInterval: Duration,
    maxLifetime: Duration,
) {
    requireDurationInRange(ttl, MIN_WATCHDOG_TTL, MAX_LEASE, "Watchdog TTL")
    requireDurationInRange(renewalInterval, MIN_WATCHDOG_INTERVAL, ttl.dividedBy(3), "Watchdog renewal interval")
    requireDurationInRange(maxLifetime, Duration.ofNanos(1), MAX_WATCHDOG_LIFETIME, "Watchdog maximum lifetime")
    ttl.toRedisMillisCeil()
    renewalInterval.toRedisMillisCeil()
    maxLifetime.toRedisMillisCeil()
}

private fun validateNamespace(namespace: String) {
    val byteCount = namespace.toByteArray(StandardCharsets.UTF_8).size
    val segments = namespace.split(':')
    require(byteCount in 1..MAX_NAMESPACE_BYTES && segments.size in 1..MAX_NAMESPACE_SEGMENTS) {
        "Lock namespace must contain 1..$MAX_NAMESPACE_SEGMENTS segments and at most $MAX_NAMESPACE_BYTES UTF-8 bytes."
    }
    segments.forEach { validateComponent(it, "Lock namespace segment", MAX_NAMESPACE_SEGMENT_CHARS) }
}

private fun validateComponent(value: String, label: String, maxChars: Int) {
    require(value.length in 1..maxChars && SAFE_COMPONENT.matches(value)) {
        "$label must contain only ASCII letters, digits, dots, underscores, or hyphens."
    }
}

private fun validateQueueBounds(cleanupBatchSize: Int, maxQueueSize: Int) {
    require(cleanupBatchSize in 1..MAX_CLEANUP_BATCH) {
        "Cleanup batch size must be between 1 and $MAX_CLEANUP_BATCH."
    }
    require(maxQueueSize in 1..MAX_QUEUE_SIZE) {
        "Maximum queue size must be between 1 and $MAX_QUEUE_SIZE."
    }
}

private fun requireDurationInRange(
    value: Duration,
    minimum: Duration,
    maximum: Duration,
    label: String,
) {
    requirePositiveDuration(value, label)
    require(value >= minimum && value <= maximum) {
        "$label is outside its supported bounds."
    }
}

private fun requirePositiveDuration(value: Duration, label: String) {
    require(!value.isZero && !value.isNegative) { "$label must be positive." }
}

private val SAFE_COMPONENT = Regex("[A-Za-z0-9._-]+")
private val MIN_FIXED_LEASE: Duration = Duration.ofMillis(100)
private val MIN_WATCHDOG_TTL: Duration = Duration.ofSeconds(3)
private val MIN_WATCHDOG_INTERVAL: Duration = Duration.ofMillis(100)
private val MAX_LEASE: Duration = Duration.ofHours(24)
private val MAX_WATCHDOG_LIFETIME: Duration = Duration.ofDays(7)

private const val MAX_NAMESPACE_BYTES = 128
private const val MAX_NAMESPACE_SEGMENTS = 8
private const val MAX_NAMESPACE_SEGMENT_CHARS = 32
private const val MAX_RESOURCE_COMPONENT_CHARS = 128
private const val MAX_DERIVED_KEY_BYTES = 512
private const val MAX_REENTRANT_HOLDS = 10_000
private const val MAX_CLEANUP_BATCH = 256
private const val MAX_QUEUE_SIZE = 10_000
private const val MAX_SPIN_ATTEMPTS_PER_SECOND = 100
private const val MAX_JITTER_RATIO = 0.25
private const val MILLIS_PER_SECOND = 1_000L
private const val NANOS_PER_MILLI = 1_000_000L
