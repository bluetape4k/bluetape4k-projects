package io.bluetape4k.redis.lettuce.coordination.internal

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong

internal enum class CoordinationObservationKind {
    COUNTER,
    GAUGE,
    HISTOGRAM,
    EVENT,
}

internal enum class CoordinationObservationName(
    val wireName: String,
    val kind: CoordinationObservationKind,
) {
    OPERATION_OUTCOME("coordination.operation.outcome", CoordinationObservationKind.COUNTER),
    RECONCILIATION("coordination.reconciliation", CoordinationObservationKind.COUNTER),
    STALE_CLEANUP("coordination.stale.cleanup", CoordinationObservationKind.COUNTER),
    CLEANUP_PENDING("coordination.cleanup.pending", CoordinationObservationKind.EVENT),
    OWNERSHIP_LOSS("coordination.ownership.loss", CoordinationObservationKind.COUNTER),
    WATCHDOG_LATE("coordination.watchdog.late", CoordinationObservationKind.COUNTER),
    WATCHDOG_MISSED("coordination.watchdog.missed", CoordinationObservationKind.COUNTER),
    NOSCRIPT_FALLBACK("coordination.noscript.fallback", CoordinationObservationKind.COUNTER),
    INTEGRITY_REJECTION("coordination.integrity.rejection", CoordinationObservationKind.COUNTER),
    CAPACITY_REJECTION("coordination.capacity.rejection", CoordinationObservationKind.COUNTER),
    ACTIVE_WATCHDOGS("coordination.watchdogs.active", CoordinationObservationKind.GAUGE),
    DUE_BACKLOG("coordination.backlog.due", CoordinationObservationKind.GAUGE),
    ACTIVE_TASKS("coordination.tasks.active", CoordinationObservationKind.GAUGE),
    WAITERS("coordination.waiters", CoordinationObservationKind.GAUGE),
    OBJECTS("coordination.objects", CoordinationObservationKind.GAUGE),
    REQUEST_HOLDS("coordination.request.holds", CoordinationObservationKind.GAUGE),
    REDIS_LATENCY("coordination.redis.latency", CoordinationObservationKind.HISTOGRAM),
    WAIT_LATENCY("coordination.wait.latency", CoordinationObservationKind.HISTOGRAM),
    RETRIES("coordination.retries", CoordinationObservationKind.COUNTER),
    CLEANUP_BATCH_SIZE("coordination.cleanup.batch.size", CoordinationObservationKind.HISTOGRAM),
}

internal enum class CoordinationRiskSignal(
    val wireName: String,
    val observations: Set<CoordinationObservationName>,
    val alertPredicate: String,
) {
    QUEUED_WAITERS_NOT_DECREASING(
        "queued-waiters-not-decreasing",
        setOf(CoordinationObservationName.WAITERS),
        "waiters remain non-decreasing during the evaluation window",
    ),
    POST_CLOSE_ACTIVITY(
        "post-close-activity",
        setOf(CoordinationObservationName.ACTIVE_TASKS, CoordinationObservationName.ACTIVE_WATCHDOGS),
        "tasks or watchdogs remain nonzero after close",
    ),
    OWNERSHIP_LOSS_INCREMENT(
        "ownership-loss-increment",
        setOf(CoordinationObservationName.OWNERSHIP_LOSS),
        "ownership loss counter increases",
    ),
    CLEANUP_PENDING_PERSISTENCE(
        "cleanup-pending-persistence",
        setOf(CoordinationObservationName.CLEANUP_PENDING),
        "cleanup pending remains active across consecutive windows",
    ),
    CAPACITY_REJECTION(
        "capacity-rejection",
        setOf(CoordinationObservationName.CAPACITY_REJECTION),
        "capacity rejection counter is nonzero",
    ),
    INTEGRITY_OR_NOSCRIPT_SPIKE(
        "integrity-or-noscript-spike",
        setOf(CoordinationObservationName.INTEGRITY_REJECTION, CoordinationObservationName.NOSCRIPT_FALLBACK),
        "integrity rejection or NOSCRIPT fallback rate exceeds baseline",
    ),
    ACTIVE_WAITERS_WITHOUT_PROGRESS(
        "active-waiters-without-progress",
        setOf(CoordinationObservationName.WAITERS, CoordinationObservationName.OPERATION_OUTCOME),
        "waiters are active while successful operation outcomes stop",
    ),
}

internal class CoordinationDimensions private constructor(
    private val values: Map<String, String>,
) {
    val size: Int get() = values.size

    fun asMap(): Map<String, String> = values

    companion object {
        val EMPTY: CoordinationDimensions = CoordinationDimensions(emptyMap())

        fun of(vararg dimensions: Pair<String, String>): CoordinationDimensions {
            val values = dimensions.toMap()
            values.forEach { (name, value) ->
                require(name in ALLOWED_NAMES) { "unsupported coordination dimension: $name" }
                require(value.matches(VALUE_PATTERN)) {
                    "coordination dimension value must be low-cardinality"
                }
            }
            return if (values.isEmpty()) EMPTY else CoordinationDimensions(values.toMap())
        }

        private val ALLOWED_NAMES = setOf(
            "object_kind",
            "operation",
            "outcome",
            "failure_kind",
            "lease_policy",
        )
        private val VALUE_PATTERN = Regex("[a-z0-9][a-z0-9_.-]{0,63}")
    }
}

internal data class CoordinationObservation(
    val name: CoordinationObservationName,
    val dimensions: CoordinationDimensions,
    val value: Double,
)

internal fun interface CoordinationObservationSink {
    fun record(observation: CoordinationObservation)
}

internal class CoordinationObserver(
    private val sink: CoordinationObservationSink = NOOP_SINK,
) {
    private val emitted = AtomicLong()
    private val dropped = AtomicLong()

    val emittedObservations: Long get() = emitted.get()
    val droppedObservations: Long get() = dropped.get()

    fun emit(
        name: CoordinationObservationName,
        dimensions: CoordinationDimensions = CoordinationDimensions.EMPTY,
        value: Double = 1.0,
    ) {
        emitted.incrementAndGet()
        try {
            sink.record(CoordinationObservation(name, dimensions, value))
        } catch (error: Exception) {
            dropped.incrementAndGet()
            log.warn(error) { "Coordination observation sink rejected an event (name=${name.wireName})" }
        }
    }

    fun <T> observe(
        name: CoordinationObservationName,
        dimensions: CoordinationDimensions,
        block: () -> T,
    ): T {
        try {
            return block()
        } finally {
            emit(name, dimensions)
        }
    }

    suspend fun <T> observeSuspending(
        name: CoordinationObservationName,
        dimensions: CoordinationDimensions,
        block: suspend () -> T,
    ): T {
        try {
            return block()
        } finally {
            emit(name, dimensions)
        }
    }

    fun <T> observeFuture(
        name: CoordinationObservationName,
        dimensions: CoordinationDimensions,
        future: CompletableFuture<T>,
    ): CompletableFuture<T> {
        future.whenComplete { _, _ -> emit(name, dimensions) }
        return future
    }

    private companion object: KLogging() {
        val NOOP_SINK = CoordinationObservationSink {}
    }
}
