package io.bluetape4k.redis.lettuce.lock

/** Lock operation emitted through the bounded observation contract. */
enum class LockOperation {
    ACQUIRE,
    INSPECT,
    RECONCILE,
    RENEW,
    RELEASE,
    DOWNGRADE,
    CLEANUP,
    CLOSE,
}

/** Lock outcome emitted through the bounded observation contract. */
enum class LockOutcome {
    SUCCEEDED,
    CONTENDED,
    TIMED_OUT,
    CANCELLED,
    AMBIGUOUS,
    OWNERSHIP_LOST,
    CAPACITY_REJECTED,
    BACKEND_FAILED,
    INTEGRITY_FAILED,
    CLOSED,
}

/** Lease-policy dimension emitted without TTL or identity data. */
enum class LockLeasePolicyKind {
    FIXED,
    WATCHDOG,
    NONE,
}

/** Failure dimension restricted to stable backend and integrity categories. */
enum class LockFailureMetricKind {
    CONNECTION,
    TIMEOUT,
    COMMAND,
    MALFORMED_REPLY,
    INVALID_STATE,
    INVALID_GENERATION,
    PARTIAL_MULTI_LOCK,
    COUNTER_REGRESSION,
}

/** Counter instruments supported by the lock runtime. */
enum class LockCounterName {
    OPERATION_TOTAL,
    RECONCILE_TOTAL,
    STALE_CLEANUP_TOTAL,
    OWNERSHIP_LOSS_TOTAL,
    WATCHDOG_LATE_TOTAL,
    WATCHDOG_MISSED_TOTAL,
    NOSCRIPT_FALLBACK_TOTAL,
    INTEGRITY_FAILURE_TOTAL,
    CAPACITY_REJECTION_TOTAL,
    CLEANUP_PENDING_TOTAL,
}

/** Gauge instruments supported by the lock runtime. */
enum class LockGaugeName {
    ACTIVE_WATCHDOGS,
    WATCHDOG_DUE_BACKLOG,
    SCHEDULED_TASKS,
    QUEUED_WAITERS,
    COORDINATION_OBJECTS,
    ACTIVE_REQUEST_HOLDS,
}

/** Histogram instruments with units encoded in their stable names. */
enum class LockHistogramName {
    REDIS_COMMAND_LATENCY_MILLIS,
    CALLER_WAIT_LATENCY_MILLIS,
    RETRY_COUNT,
    CLEANUP_BATCH_SIZE,
}

/** Identity-free structured event emitted by a lock object. */
data class LockEvent(
    val objectKind: LockKind,
    val operation: LockOperation,
    val outcome: LockOutcome,
    val failureKind: LockFailureMetricKind?,
    val leasePolicy: LockLeasePolicyKind,
)

/** Allowlisted low-cardinality dimensions shared by lock observations. */
data class LockDimensions(
    val objectKind: LockKind,
    val operation: LockOperation,
    val outcome: LockOutcome,
    val failureKind: LockFailureMetricKind?,
    val leasePolicy: LockLeasePolicyKind,
)

/** A bounded metric or event observation that carries no caller-controlled labels. */
sealed interface LockObservation {

    /** Adds a positive monotonic counter delta. */
    data class Counter(
        val name: LockCounterName,
        val delta: Long,
        val dimensions: LockDimensions,
    ): LockObservation {
        init {
            require(delta > 0L) { "Lock counter delta must be positive." }
        }
    }

    /** Reports a non-negative current runtime value. */
    data class Gauge(
        val name: LockGaugeName,
        val value: Long,
        val dimensions: LockDimensions,
    ): LockObservation {
        init {
            require(value >= 0L) { "Lock gauge value must not be negative." }
        }
    }

    /** Reports a finite non-negative sample. */
    data class Histogram(
        val name: LockHistogramName,
        val value: Double,
        val dimensions: LockDimensions,
    ): LockObservation {
        init {
            require(value.isFinite() && value >= 0.0) {
                "Lock histogram value must be finite and non-negative."
            }
        }
    }

    /** Emits one bounded structured lifecycle event. */
    data class Event(
        val event: LockEvent,
    ): LockObservation
}

/** Receives bounded lock observations without owning operation behavior. */
fun interface LockObservationSink {
    /** Records one observation. Implementations must not retain caller secrets outside this bounded model. */
    fun record(observation: LockObservation)

    companion object {
        /** No-op observation sink used when callers do not install telemetry. */
        @JvmField
        val NOOP: LockObservationSink = LockObservationSink {}
    }
}

internal fun LockObservationSink.recordSafely(observation: LockObservation) {
    try {
        record(observation)
    } catch (_: Exception) {
        // Observation must never alter the lock operation or watchdog lifecycle.
    }
}
