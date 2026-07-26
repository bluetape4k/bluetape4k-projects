package io.bluetape4k.redis.lettuce.lock.internal

import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationObservation
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationObservationName
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationObserver
import io.bluetape4k.redis.lettuce.lock.LeasePolicy
import io.bluetape4k.redis.lettuce.lock.LockCounterName
import io.bluetape4k.redis.lettuce.lock.LockDimensions
import io.bluetape4k.redis.lettuce.lock.LockEvent
import io.bluetape4k.redis.lettuce.lock.LockFailureMetricKind
import io.bluetape4k.redis.lettuce.lock.LockGaugeName
import io.bluetape4k.redis.lettuce.lock.LockHistogramName
import io.bluetape4k.redis.lettuce.lock.LockKind
import io.bluetape4k.redis.lettuce.lock.LockLeasePolicyKind
import io.bluetape4k.redis.lettuce.lock.LockObservation
import io.bluetape4k.redis.lettuce.lock.LockObservationSink
import io.bluetape4k.redis.lettuce.lock.LockOperation
import io.bluetape4k.redis.lettuce.lock.LockOutcome
import io.bluetape4k.redis.lettuce.lock.recordSafely
import io.bluetape4k.redis.lettuce.script.RedisScript
import io.bluetape4k.redis.lettuce.script.RedisScriptExecutionObserver
import io.bluetape4k.redis.lettuce.script.RedisScriptRunner
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.async.RedisScriptingAsyncCommands
import io.lettuce.core.api.sync.RedisScriptingCommands
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal val REQUIRED_COORDINATION_SIGNALS: Set<CoordinationObservationName> = setOf(
    CoordinationObservationName.OPERATION_OUTCOME,
    CoordinationObservationName.RECONCILIATION,
    CoordinationObservationName.STALE_CLEANUP,
    CoordinationObservationName.CLEANUP_PENDING,
    CoordinationObservationName.OWNERSHIP_LOSS,
    CoordinationObservationName.WATCHDOG_LATE,
    CoordinationObservationName.WATCHDOG_MISSED,
    CoordinationObservationName.NOSCRIPT_FALLBACK,
    CoordinationObservationName.INTEGRITY_REJECTION,
    CoordinationObservationName.CAPACITY_REJECTION,
    CoordinationObservationName.ACTIVE_WATCHDOGS,
    CoordinationObservationName.DUE_BACKLOG,
    CoordinationObservationName.ACTIVE_TASKS,
    CoordinationObservationName.WAITERS,
    CoordinationObservationName.OBJECTS,
    CoordinationObservationName.REQUEST_HOLDS,
    CoordinationObservationName.REDIS_LATENCY,
    CoordinationObservationName.WAIT_LATENCY,
    CoordinationObservationName.RETRIES,
    CoordinationObservationName.CLEANUP_BATCH_SIZE,
)

internal class LockObservationRecorder(
    private val objectKind: LockKind,
    private val sink: LockObservationSink,
) {
    fun asCoordinationObserver(): CoordinationObserver =
        CoordinationObserver { observation -> recordCoordination(observation) }

    fun <T> runScript(
        commands: RedisScriptingCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        operation: LockOperation,
        vararg args: String,
    ): T {
        return RedisScriptRunner.runObserved(
            commands,
            script,
            outputType,
            keys,
            scriptObserver(operation),
            *args,
        )
    }

    fun <T> runScriptAsync(
        commands: RedisScriptingAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        operation: LockOperation,
        vararg args: String,
    ): CompletableFuture<T> =
        RedisScriptRunner.runAsyncObserved(
            commands,
            script,
            outputType,
            keys,
            scriptObserver(operation),
            *args,
        )

    suspend fun <T> runScriptSuspending(
        commands: RedisScriptingAsyncCommands<String, String>,
        script: RedisScript,
        outputType: ScriptOutputType,
        keys: Array<String>,
        operation: LockOperation,
        vararg args: String,
    ): T =
        RedisScriptRunner.runSuspendingObserved(
            commands,
            script,
            outputType,
            keys,
            scriptObserver(operation),
            *args,
        )

    fun recordCounter(
        name: LockCounterName,
        operation: LockOperation,
        outcome: LockOutcome = name.defaultOutcome(),
        leasePolicy: LockLeasePolicyKind = LockLeasePolicyKind.NONE,
        failureKind: LockFailureMetricKind? = name.defaultFailureKind(),
    ) {
        val dimensions = dimensions(operation, outcome, leasePolicy, failureKind)
        sink.recordSafely(LockObservation.Counter(name, 1L, dimensions))
        recordEvent(dimensions)
    }

    fun recordCounter(
        name: LockCounterName,
        operation: LockOperation,
        outcome: LockOutcome,
        leasePolicy: LeasePolicy,
        failureKind: LockFailureMetricKind? = name.defaultFailureKind(),
    ) {
        recordCounter(name, operation, outcome, leasePolicy.toMetricKind(), failureKind)
    }

    fun recordWaitGauge(name: LockGaugeName, value: Int) {
        recordGauge(
            name = name,
            value = value.toDouble(),
            operation = LockOperation.ACQUIRE,
            outcome = if (value > 0) LockOutcome.CONTENDED else LockOutcome.SUCCEEDED,
            leasePolicy = LockLeasePolicyKind.NONE,
            failureKind = null,
        )
    }

    fun recordWaitCompletion(elapsedNanos: Long, retries: Int, outcome: LockOutcome) {
        recordHistogram(
            name = LockHistogramName.CALLER_WAIT_LATENCY_MILLIS,
            value = elapsedNanos.coerceAtLeast(0L) / 1_000_000.0,
            operation = LockOperation.ACQUIRE,
            outcome = outcome,
            leasePolicy = LockLeasePolicyKind.NONE,
            failureKind = null,
        )
        recordHistogram(
            name = LockHistogramName.RETRY_COUNT,
            value = retries.coerceAtLeast(0).toDouble(),
            operation = LockOperation.ACQUIRE,
            outcome = outcome,
            leasePolicy = LockLeasePolicyKind.NONE,
            failureKind = null,
        )
    }

    private fun recordCoordination(observation: CoordinationObservation) {
        val dimensions = observation.dimensions.asMap()
        val operation = dimensions["operation"].toOperation() ?: observation.name.defaultOperation()
        val outcome = dimensions["outcome"].toOutcome() ?: observation.name.defaultOutcome()
        val failure = dimensions["failure_kind"].toFailureKind() ?: observation.name.defaultFailureKind()
        val leasePolicy = dimensions["lease_policy"].toLeasePolicyKind()

        when (observation.name) {
            CoordinationObservationName.OPERATION_OUTCOME -> {
                val lockDimensions = dimensions(operation, outcome, leasePolicy, failure)
                sink.recordSafely(LockObservation.Counter(LockCounterName.OPERATION_TOTAL, 1L, lockDimensions))
                recordEvent(lockDimensions)
            }
            CoordinationObservationName.RECONCILIATION ->
                recordCounter(LockCounterName.RECONCILE_TOTAL, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.STALE_CLEANUP ->
                recordCounter(LockCounterName.STALE_CLEANUP_TOTAL, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.CLEANUP_PENDING ->
                recordCounter(LockCounterName.CLEANUP_PENDING_TOTAL, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.OWNERSHIP_LOSS ->
                recordCounter(LockCounterName.OWNERSHIP_LOSS_TOTAL, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.WATCHDOG_LATE ->
                recordCounter(LockCounterName.WATCHDOG_LATE_TOTAL, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.WATCHDOG_MISSED ->
                recordCounter(LockCounterName.WATCHDOG_MISSED_TOTAL, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.NOSCRIPT_FALLBACK ->
                recordCounter(LockCounterName.NOSCRIPT_FALLBACK_TOTAL, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.INTEGRITY_REJECTION ->
                recordCounter(LockCounterName.INTEGRITY_FAILURE_TOTAL, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.CAPACITY_REJECTION ->
                recordCounter(LockCounterName.CAPACITY_REJECTION_TOTAL, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.ACTIVE_WATCHDOGS ->
                recordGauge(LockGaugeName.ACTIVE_WATCHDOGS, observation.value, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.DUE_BACKLOG ->
                recordGauge(LockGaugeName.WATCHDOG_DUE_BACKLOG, observation.value, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.ACTIVE_TASKS ->
                recordGauge(LockGaugeName.SCHEDULED_TASKS, observation.value, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.WAITERS ->
                recordGauge(LockGaugeName.QUEUED_WAITERS, observation.value, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.OBJECTS ->
                recordGauge(LockGaugeName.COORDINATION_OBJECTS, observation.value, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.REQUEST_HOLDS ->
                recordGauge(LockGaugeName.ACTIVE_REQUEST_HOLDS, observation.value, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.REDIS_LATENCY ->
                recordHistogram(LockHistogramName.REDIS_COMMAND_LATENCY_MILLIS, observation.value, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.WAIT_LATENCY ->
                recordHistogram(LockHistogramName.CALLER_WAIT_LATENCY_MILLIS, observation.value, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.RETRIES ->
                recordHistogram(LockHistogramName.RETRY_COUNT, observation.value, operation, outcome, leasePolicy, failure)
            CoordinationObservationName.CLEANUP_BATCH_SIZE ->
                recordHistogram(LockHistogramName.CLEANUP_BATCH_SIZE, observation.value, operation, outcome, leasePolicy, failure)
        }
    }

    private fun recordGauge(
        name: LockGaugeName,
        value: Double,
        operation: LockOperation,
        outcome: LockOutcome,
        leasePolicy: LockLeasePolicyKind,
        failureKind: LockFailureMetricKind?,
    ) {
        sink.recordSafely(
            LockObservation.Gauge(
                name,
                value.coerceAtLeast(0.0).toLong(),
                dimensions(operation, outcome, leasePolicy, failureKind),
            ),
        )
    }

    private fun recordHistogram(
        name: LockHistogramName,
        value: Double,
        operation: LockOperation,
        outcome: LockOutcome,
        leasePolicy: LockLeasePolicyKind,
        failureKind: LockFailureMetricKind?,
    ) {
        sink.recordSafely(
            LockObservation.Histogram(
                name,
                value.coerceAtLeast(0.0),
                dimensions(operation, outcome, leasePolicy, failureKind),
            ),
        )
    }

    private fun scriptObserver(operation: LockOperation): RedisScriptExecutionObserver =
        RedisScriptExecutionObserver { observation ->
            if (observation.noScriptFallback) {
                recordCounter(LockCounterName.NOSCRIPT_FALLBACK_TOTAL, operation)
            }
            recordRedisLatency(operation, observation.elapsedNanos)
        }

    private fun recordRedisLatency(operation: LockOperation, elapsedNanos: Long) {
        val elapsedMillis = elapsedNanos.coerceAtLeast(0L) / 1_000_000.0
        recordHistogram(
            LockHistogramName.REDIS_COMMAND_LATENCY_MILLIS,
            elapsedMillis,
            operation,
            LockOutcome.SUCCEEDED,
            LockLeasePolicyKind.NONE,
            null,
        )
    }

    private fun recordEvent(dimensions: LockDimensions) {
        sink.recordSafely(
            LockObservation.Event(
                LockEvent(
                    objectKind = dimensions.objectKind,
                    operation = dimensions.operation,
                    outcome = dimensions.outcome,
                    failureKind = dimensions.failureKind,
                    leasePolicy = dimensions.leasePolicy,
                ),
            ),
        )
    }

    private fun dimensions(
        operation: LockOperation,
        outcome: LockOutcome,
        leasePolicy: LockLeasePolicyKind,
        failureKind: LockFailureMetricKind?,
    ): LockDimensions =
        LockDimensions(
            objectKind = objectKind,
            operation = operation,
            outcome = outcome,
            failureKind = failureKind,
            leasePolicy = leasePolicy,
        )
}

internal class LockWaitObservation(
    private val recorder: LockObservationRecorder,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private val activeRequests = AtomicInteger()
    private val queuedWaiters = AtomicInteger()

    fun begin(): Session {
        recorder.recordWaitGauge(LockGaugeName.ACTIVE_REQUEST_HOLDS, activeRequests.incrementAndGet())
        return Session(nanoTime())
    }

    inner class Session internal constructor(
        private val startedAtNanos: Long,
    ) {
        private val completed = AtomicBoolean()
        private val queued = AtomicBoolean()
        private val retries = AtomicInteger()

        fun onContended() {
            retries.incrementAndGet()
            if (queued.compareAndSet(false, true)) {
                recorder.recordWaitGauge(LockGaugeName.QUEUED_WAITERS, queuedWaiters.incrementAndGet())
            }
        }

        fun complete(outcome: LockOutcome) {
            if (!completed.compareAndSet(false, true)) return
            if (queued.compareAndSet(true, false)) {
                recorder.recordWaitGauge(LockGaugeName.QUEUED_WAITERS, queuedWaiters.decrementAndGet())
            }
            recorder.recordWaitGauge(LockGaugeName.ACTIVE_REQUEST_HOLDS, activeRequests.decrementAndGet())
            recorder.recordWaitCompletion(nanoTime() - startedAtNanos, retries.get(), outcome)
        }
    }
}

internal fun LockObservationSink.withObjectKind(objectKind: LockKind): LockObservationSink =
    LockObservationSink { observation ->
        val remapped = when (observation) {
            is LockObservation.Counter ->
                observation.copy(dimensions = observation.dimensions.copy(objectKind = objectKind))
            is LockObservation.Gauge ->
                observation.copy(dimensions = observation.dimensions.copy(objectKind = objectKind))
            is LockObservation.Histogram ->
                observation.copy(dimensions = observation.dimensions.copy(objectKind = objectKind))
            is LockObservation.Event ->
                observation.copy(event = observation.event.copy(objectKind = objectKind))
        }
        recordSafely(remapped)
    }

internal fun coordinationObserver(vararg recorders: LockObservationRecorder): CoordinationObserver {
    val observers = recorders.map(LockObservationRecorder::asCoordinationObserver)
    return CoordinationObserver { observation ->
        observers.forEach { observer ->
            observer.emit(observation.name, observation.dimensions, observation.value)
        }
    }
}

private fun CoordinationObservationName.defaultOperation(): LockOperation =
    when (this) {
        CoordinationObservationName.RECONCILIATION -> LockOperation.RECONCILE
        CoordinationObservationName.STALE_CLEANUP,
        CoordinationObservationName.CLEANUP_PENDING,
        CoordinationObservationName.CLEANUP_BATCH_SIZE,
        -> LockOperation.CLEANUP
        CoordinationObservationName.WATCHDOG_LATE,
        CoordinationObservationName.WATCHDOG_MISSED,
        CoordinationObservationName.OWNERSHIP_LOSS,
        -> LockOperation.RENEW
        else -> LockOperation.ACQUIRE
    }

private fun CoordinationObservationName.defaultOutcome(): LockOutcome =
    when (this) {
        CoordinationObservationName.CLEANUP_PENDING -> LockOutcome.CONTENDED
        CoordinationObservationName.OWNERSHIP_LOSS,
        CoordinationObservationName.WATCHDOG_MISSED,
        -> LockOutcome.OWNERSHIP_LOST
        CoordinationObservationName.CAPACITY_REJECTION -> LockOutcome.CAPACITY_REJECTED
        CoordinationObservationName.INTEGRITY_REJECTION -> LockOutcome.INTEGRITY_FAILED
        CoordinationObservationName.WAITERS -> LockOutcome.CONTENDED
        else -> LockOutcome.SUCCEEDED
    }

private fun CoordinationObservationName.defaultFailureKind(): LockFailureMetricKind? =
    when (this) {
        CoordinationObservationName.NOSCRIPT_FALLBACK -> LockFailureMetricKind.COMMAND
        CoordinationObservationName.INTEGRITY_REJECTION -> LockFailureMetricKind.INVALID_STATE
        else -> null
    }

private fun LockCounterName.defaultOutcome(): LockOutcome =
    when (this) {
        LockCounterName.CAPACITY_REJECTION_TOTAL -> LockOutcome.CAPACITY_REJECTED
        LockCounterName.INTEGRITY_FAILURE_TOTAL -> LockOutcome.INTEGRITY_FAILED
        LockCounterName.OWNERSHIP_LOSS_TOTAL,
        LockCounterName.WATCHDOG_MISSED_TOTAL,
        -> LockOutcome.OWNERSHIP_LOST
        LockCounterName.CLEANUP_PENDING_TOTAL -> LockOutcome.CONTENDED
        else -> LockOutcome.SUCCEEDED
    }

private fun LockCounterName.defaultFailureKind(): LockFailureMetricKind? =
    when (this) {
        LockCounterName.NOSCRIPT_FALLBACK_TOTAL -> LockFailureMetricKind.COMMAND
        LockCounterName.INTEGRITY_FAILURE_TOTAL -> LockFailureMetricKind.INVALID_STATE
        else -> null
    }

private fun LeasePolicy.toMetricKind(): LockLeasePolicyKind =
    when (this) {
        is LeasePolicy.Fixed -> LockLeasePolicyKind.FIXED
        is LeasePolicy.Watchdog -> LockLeasePolicyKind.WATCHDOG
    }

private fun String?.toOperation(): LockOperation? =
    this?.uppercase()?.let { runCatching { LockOperation.valueOf(it) }.getOrNull() }

private fun String?.toOutcome(): LockOutcome? =
    this?.uppercase()?.let { runCatching { LockOutcome.valueOf(it) }.getOrNull() }

private fun String?.toFailureKind(): LockFailureMetricKind? =
    this?.uppercase()?.let { runCatching { LockFailureMetricKind.valueOf(it) }.getOrNull() }

private fun String?.toLeasePolicyKind(): LockLeasePolicyKind =
    this?.uppercase()?.let { runCatching { LockLeasePolicyKind.valueOf(it) }.getOrNull() }
        ?: LockLeasePolicyKind.NONE
