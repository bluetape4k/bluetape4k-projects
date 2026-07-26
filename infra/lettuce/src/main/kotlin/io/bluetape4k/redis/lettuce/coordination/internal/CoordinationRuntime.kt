package io.bluetape4k.redis.lettuce.coordination.internal

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import java.lang.ref.WeakReference
import java.util.PriorityQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

internal data class CoordinationRuntimeLimits(
    val maxRegistrations: Int = 10_000,
    val maxWatchdogsPerTick: Int = 256,
    val backlogCadence: Duration = 25.milliseconds,
) {
    init {
        require(maxRegistrations > 0) { "maxRegistrations must be positive" }
        require(maxWatchdogsPerTick > 0) { "maxWatchdogsPerTick must be positive" }
        require(backlogCadence.isPositive() && backlogCadence.isFinite()) {
            "backlogCadence must be positive and finite"
        }
    }
}

internal class CoordinationCapacityException(message: String): IllegalStateException(message)

internal enum class CoordinationRenewalOutcome {
    RENEWED,
    OWNERSHIP_LOST,
}

internal fun interface CoordinationScheduledHandle {
    fun cancel(): Boolean
}

internal interface CoordinationScheduler {
    val isShutdown: Boolean

    fun schedule(delay: Duration, task: () -> Unit): CoordinationScheduledHandle

    fun shutdown()
}

internal data class CoordinationDrainResult(
    val dispatched: Int,
    val dueBacklog: Int,
    val nextDrainDelay: Duration?,
)

internal data class CoordinationRuntimeSnapshot(
    val due: Long,
    val dispatched: Long,
    val late: Long,
    val missed: Long,
    val maximumBacklog: Int,
    val rejectedLateCompletions: Long,
)

internal class CoordinationRuntime(
    private val ticker: MonotonicTicker = MonotonicTicker.SYSTEM,
    scheduler: CoordinationScheduler? = null,
    val limits: CoordinationRuntimeLimits = CoordinationRuntimeLimits(),
    private val observer: CoordinationObserver = CoordinationObserver(),
    private val watchdogDelay: (Duration) -> Duration = ::earlyJitteredWatchdogDelay,
) {
    private val lock = ReentrantLock()
    private val ownsScheduler = scheduler == null
    private val scheduler = scheduler ?: ExecutorCoordinationScheduler()
    private val objectSequence = AtomicLong()
    private val taskSequence = AtomicLong()
    private val objects = LinkedHashMap<Long, ObjectState>()
    private val tasks = LinkedHashMap<Long, TaskState>()
    private val dueQueue = PriorityQueue<TaskState>(compareBy<TaskState> { it.dueAtNanos }.thenBy { it.id })
    private var scheduledDrain: CoordinationScheduledHandle? = null
    private var scheduledDrainAtNanos: Long? = null
    private var closed = false
    private var dueCount = 0L
    private var dispatchedCount = 0L
    private var lateCount = 0L
    private var missedCount = 0L
    private var maximumBacklog = 0
    private var rejectedLateCompletions = 0L

    val isClosed: Boolean get() = lock.withLock { closed }
    val schedulerShutdown: Boolean get() = scheduler.isShutdown
    val activeObjects: Int get() = lock.withLock { objects.size }
    val activeTasks: Int get() = lock.withLock { tasks.values.count { it.kind == TaskKind.SCHEDULED } }
    val activeWatchdogs: Int get() = lock.withLock { tasks.values.count { it.kind == TaskKind.WATCHDOG } }
    val dueBacklog: Int get() = lock.withLock { dueBacklogLocked(ticker.readNanos()) }

    fun registerObject(fingerprint: String): CoordinationObjectRegistration {
        require(fingerprint.matches(FINGERPRINT_PATTERN)) { "fingerprint has an invalid format" }
        val (registration, objectCount) = lock.withLock {
            check(!closed) { "coordination runtime is closed" }
            val id = objectSequence.incrementAndGet()
            objects[id] = ObjectState(id, fingerprint)
            CoordinationObjectRegistration(id) to objects.size.toDouble()
        }
        observer.emit(CoordinationObservationName.OBJECTS, value = objectCount)
        return registration
    }

    fun drainDue(): CoordinationDrainResult {
        val dispatches = mutableListOf<TaskState>()
        val observations = mutableListOf<PendingObservation>()
        val ownershipLossActions = mutableListOf<() -> Unit>()
        var activitySnapshot: ActivitySnapshot? = null
        val result = lock.withLock {
            if (closed) {
                return CoordinationDrainResult(0, 0, null)
            }
            scheduledDrain?.cancel()
            scheduledDrain = null
            scheduledDrainAtNanos = null

            val now = ticker.readNanos()
            while (dispatches.size < limits.maxWatchdogsPerTick) {
                val next = dueQueue.peek() ?: break
                if (next.dueAtNanos > now) {
                    break
                }
                dueQueue.remove()
                if (next.closed || tasks[next.id] !== next) {
                    continue
                }
                dueCount++
                if (next.kind == TaskKind.WATCHDOG && now >= next.lifetimeDeadlineNanos) {
                    observations += PendingObservation(CoordinationObservationName.OWNERSHIP_LOSS)
                    if (removeTaskLocked(next)) {
                        activitySnapshot = activitySnapshotLocked()
                        next.ownershipLossAction?.let(ownershipLossActions::add)
                    }
                    continue
                }
                if (next.inFlight) {
                    if (now >= next.ttlDeadlineNanos) {
                        missedCount++
                        observations += PendingObservation(CoordinationObservationName.WATCHDOG_MISSED)
                        observations += PendingObservation(CoordinationObservationName.OWNERSHIP_LOSS)
                        if (removeTaskLocked(next)) {
                            activitySnapshot = activitySnapshotLocked()
                            next.ownershipLossAction?.let(ownershipLossActions::add)
                        }
                    }
                    continue
                }
                when (next.kind) {
                    TaskKind.SCHEDULED -> {
                        tasks.remove(next.id)
                        next.closed = true
                        activitySnapshot = activitySnapshotLocked()
                    }
                    TaskKind.WATCHDOG -> {
                        next.inFlight = true
                        next.renewalDueAtNanos = next.dueAtNanos
                        next.dueAtNanos = minOf(next.ttlDeadlineNanos, next.lifetimeDeadlineNanos)
                        dueQueue.add(next)
                    }
                }
                dispatches += next
            }

            dispatchedCount += dispatches.size
            val backlog = dueBacklogLocked(now)
            maximumBacklog = maxOf(maximumBacklog, backlog)
            val nextDelay = when {
                backlog > 0 -> limits.backlogCadence
                else -> dueQueue.peek()?.let { next ->
                    positiveDelay(next.dueAtNanos, now)
                }
            }
            if (nextDelay != null) {
                scheduleDrainLocked(nextDelay, now)
            }
            observations += PendingObservation(CoordinationObservationName.DUE_BACKLOG, backlog.toDouble())
            CoordinationDrainResult(dispatches.size, backlog, nextDelay)
        }

        dispatches.forEach(::dispatch)
        runOwnershipLossActions(ownershipLossActions)
        activitySnapshot?.let(::emitActivityGauges)
        emitObservations(observations)
        return result
    }

    fun connectionClosed() {
        closeRuntime(removeFromRegistry = true)
    }

    fun snapshot(): CoordinationRuntimeSnapshot =
        lock.withLock {
            CoordinationRuntimeSnapshot(
                due = dueCount,
                dispatched = dispatchedCount,
                late = lateCount,
                missed = missedCount,
                maximumBacklog = maximumBacklog,
                rejectedLateCompletions = rejectedLateCompletions,
            )
        }

    private fun dispatch(task: TaskState) {
        when (task.kind) {
            TaskKind.SCHEDULED -> {
                try {
                    task.scheduledAction?.invoke()
                } catch (error: Exception) {
                    log.warn(error) {
                        "Coordination scheduled task failed (fingerprint=${task.objectFingerprint})"
                    }
                }
            }
            TaskKind.WATCHDOG -> dispatchWatchdog(task)
        }
    }

    private fun dispatchWatchdog(task: TaskState) {
        val expectedGeneration = lock.withLock { task.generation }
        val future = try {
            task.renewAction?.invoke()
                ?: CompletableFuture.completedFuture(CoordinationRenewalOutcome.OWNERSHIP_LOST)
        } catch (error: Exception) {
            CompletableFuture.failedFuture(error)
        }
        future.whenComplete { outcome, error ->
            completeWatchdog(task, expectedGeneration, outcome, error)
        }
    }

    private fun completeWatchdog(
        task: TaskState,
        expectedGeneration: Long,
        outcome: CoordinationRenewalOutcome?,
        error: Throwable?,
    ) {
        val observations = mutableListOf<PendingObservation>()
        var activitySnapshot: ActivitySnapshot? = null
        var ownershipLossAction: (() -> Unit)? = null
        lock.withLock {
            val current = tasks[task.id]
            if (
                closed ||
                current !== task ||
                task.closed ||
                task.objectFingerprint != current.objectFingerprint ||
                task.generation != expectedGeneration
            ) {
                if (current === task) {
                    if (removeTaskLocked(task)) {
                        activitySnapshot = activitySnapshotLocked()
                    }
                }
                rejectedLateCompletions++
                return@withLock
            }

            task.inFlight = false
            dueQueue.remove(task)
            val now = ticker.readNanos()
            if (now >= task.ttlDeadlineNanos || now >= task.lifetimeDeadlineNanos) {
                if (now >= task.ttlDeadlineNanos) {
                    missedCount++
                    observations += PendingObservation(CoordinationObservationName.WATCHDOG_MISSED)
                }
                observations += PendingObservation(CoordinationObservationName.OWNERSHIP_LOSS)
                if (removeTaskLocked(task)) {
                    activitySnapshot = activitySnapshotLocked()
                    ownershipLossAction = task.ownershipLossAction
                }
                return@withLock
            }
            if (error != null || outcome == CoordinationRenewalOutcome.OWNERSHIP_LOST) {
                observations += PendingObservation(CoordinationObservationName.OWNERSHIP_LOSS)
                if (removeTaskLocked(task)) {
                    activitySnapshot = activitySnapshotLocked()
                    ownershipLossAction = task.ownershipLossAction
                }
                return@withLock
            }

            val completionDelay = now - task.renewalDueAtNanos
            if (completionDelay > 0L) {
                lateCount++
                observations += PendingObservation(CoordinationObservationName.WATCHDOG_LATE)
            }
            task.ttlDeadlineNanos = saturatingAdd(now, task.ttlNanos)
            task.dueAtNanos = minOf(
                saturatingAdd(now, nextWatchdogDelayNanos(task.renewalIntervalNanos)),
                task.lifetimeDeadlineNanos,
            )
            dueQueue.add(task)
            scheduleNextLocked(now)
        }
        ownershipLossAction?.let(::runOwnershipLossAction)
        activitySnapshot?.let(::emitActivityGauges)
        emitObservations(observations)
    }

    private fun closeObject(objectId: Long) {
        var closedRuntime = false
        var activitySnapshot: ActivitySnapshot? = null
        var objectCount: Double? = null
        var closeActions: List<() -> Unit> = emptyList()
        lock.withLock {
            val objectState = objects.remove(objectId) ?: return
            objectState.closed = true
            closeActions = objectState.closeActions.toList()
            objectState.closeActions.clear()
            tasks.values.filter { it.objectId == objectId }.forEach { task ->
                task.closed = true
                tasks.remove(task.id)
            }
            dueQueue.removeIf { it.objectId == objectId }
            if (objects.isEmpty()) {
                closedRuntime = closeRuntimeLocked()
            }
            activitySnapshot = activitySnapshotLocked()
            objectCount = objects.size.toDouble()
        }
        runCloseActions(closeActions)
        if (closedRuntime) {
            finishRuntimeClose(removeFromRegistry = true)
        }
        activitySnapshot?.let(::emitActivityGauges)
        objectCount?.let { observer.emit(CoordinationObservationName.OBJECTS, value = it) }
    }

    private fun closeRuntime(removeFromRegistry: Boolean) {
        var closeActions: List<() -> Unit> = emptyList()
        val activitySnapshot = lock.withLock {
            closeActions = objects.values.flatMap { it.closeActions }
            if (!closeRuntimeLocked()) {
                return
            }
            activitySnapshotLocked()
        }
        runCloseActions(closeActions)
        finishRuntimeClose(removeFromRegistry)
        emitActivityGauges(activitySnapshot)
    }

    private fun closeRuntimeLocked(): Boolean {
        if (closed) {
            return false
        }
        closed = true
        scheduledDrain?.cancel()
        scheduledDrain = null
        scheduledDrainAtNanos = null
        tasks.values.forEach { it.closed = true }
        tasks.clear()
        dueQueue.clear()
        objects.values.forEach { it.closed = true }
        objects.clear()
        return true
    }

    private fun finishRuntimeClose(removeFromRegistry: Boolean) {
        if (ownsScheduler) {
            scheduler.shutdown()
        }
        if (removeFromRegistry) {
            registryLock.withLock {
                registry.removeIf { entry ->
                    val connection = entry.connection.get()
                    connection == null || entry.runtime === this
                }
            }
        }
    }

    private fun registerTask(
        objectId: Long,
        delay: Duration,
        scheduledAction: (() -> Unit)?,
        ttl: Duration?,
        renewalInterval: Duration?,
        maxLifetime: Duration?,
        generation: Long,
        ownershipLossAction: (() -> Unit)?,
        renewAction: (() -> CompletableFuture<CoordinationRenewalOutcome>)?,
    ): CoordinationTaskRegistration {
        require(delay.isFinite() && !delay.isNegative()) { "delay must be non-negative and finite" }
        try {
            val (registration, activitySnapshot) = lock.withLock {
                check(!closed) { "coordination runtime is closed" }
                val objectState = objects[objectId]
                check(objectState != null && !objectState.closed) { "coordination object is closed" }
                if (tasks.size >= limits.maxRegistrations) {
                    throw CoordinationCapacityException("coordination registration capacity is exhausted")
                }

                val now = ticker.readNanos()
                val kind = if (renewAction == null) TaskKind.SCHEDULED else TaskKind.WATCHDOG
                val ttlNanos = ttl?.inWholeNanoseconds ?: 0L
                val renewalNanos = renewalInterval?.inWholeNanoseconds ?: 0L
                val lifetimeNanos = maxLifetime?.inWholeNanoseconds ?: Long.MAX_VALUE
                if (kind == TaskKind.WATCHDOG) {
                    require(lifetimeNanos > 0L) { "watchdog max lifetime must be positive" }
                    validateWatchdogCapacity(ttlNanos, renewalNanos)
                }
                val initialDelayNanos = if (kind == TaskKind.WATCHDOG) {
                    nextWatchdogDelayNanos(delay.inWholeNanoseconds)
                } else {
                    delay.inWholeNanoseconds
                }
                val lifetimeDeadlineNanos = saturatingAdd(now, lifetimeNanos)

                val task = TaskState(
                    id = taskSequence.incrementAndGet(),
                    objectId = objectId,
                    objectFingerprint = objectState.fingerprint,
                    kind = kind,
                    dueAtNanos = minOf(
                        saturatingAdd(now, initialDelayNanos),
                        lifetimeDeadlineNanos,
                    ),
                    ttlNanos = ttlNanos,
                    ttlDeadlineNanos = saturatingAdd(now, ttlNanos),
                    lifetimeDeadlineNanos = lifetimeDeadlineNanos,
                    renewalIntervalNanos = renewalNanos,
                    generation = generation,
                    scheduledAction = scheduledAction,
                    ownershipLossAction = ownershipLossAction,
                    renewAction = renewAction,
                )
                tasks[task.id] = task
                dueQueue.add(task)
                scheduleNextLocked(now)
                CoordinationTaskRegistration(task) to activitySnapshotLocked()
            }
            emitActivityGauges(activitySnapshot)
            return registration
        } catch (error: CoordinationCapacityException) {
            observer.emit(CoordinationObservationName.CAPACITY_REJECTION)
            throw error
        }
    }

    private fun validateWatchdogCapacity(ttlNanos: Long, renewalNanos: Long) {
        require(ttlNanos >= MIN_WATCHDOG_TTL.inWholeNanoseconds) {
            "watchdog ttl must be at least $MIN_WATCHDOG_TTL"
        }
        require(renewalNanos > 0L && renewalNanos <= ttlNanos / 3L) {
            "watchdog renewal interval must be positive and at most one third of ttl"
        }
        val prospectiveWatchdogs = tasks.values.count { it.kind == TaskKind.WATCHDOG } + 1
        val drainBatches = ceil(prospectiveWatchdogs.toDouble() / limits.maxWatchdogsPerTick).toLong()
        val requiredDrainNanos = saturatingMultiply(
            drainBatches,
            limits.backlogCadence.inWholeNanoseconds,
        )
        val newCompletionMargin = ttlNanos - renewalNanos - REQUIRED_REDIS_MARGIN.inWholeNanoseconds
        val minimumCompletionMargin = tasks.values
            .asSequence()
            .filter { it.kind == TaskKind.WATCHDOG }
            .map { it.ttlNanos - it.renewalIntervalNanos - REQUIRED_REDIS_MARGIN.inWholeNanoseconds }
            .plus(newCompletionMargin)
            .min()
        if (requiredDrainNanos > minimumCompletionMargin) {
            throw CoordinationCapacityException("watchdog service capacity would be exceeded")
        }
    }

    private fun nextWatchdogDelayNanos(renewalIntervalNanos: Long): Long {
        val renewalInterval = renewalIntervalNanos.nanoseconds
        val delay = watchdogDelay(renewalInterval)
        val minimum = (renewalInterval * MIN_WATCHDOG_DELAY_FACTOR).inWholeNanoseconds
        require(
            delay.isFinite() &&
                delay.inWholeNanoseconds in minimum..renewalIntervalNanos
        ) {
            "watchdog delay must be finite and between 90% and 100% of the renewal interval"
        }
        return delay.inWholeNanoseconds
    }

    private fun scheduleNextLocked(now: Long) {
        val next = dueQueue.peek() ?: return
        scheduleDrainLocked(positiveDelay(next.dueAtNanos, now), now)
    }

    private fun scheduleDrainLocked(delay: Duration, now: Long) {
        val target = saturatingAdd(now, delay.inWholeNanoseconds)
        val currentTarget = scheduledDrainAtNanos
        if (currentTarget != null && currentTarget <= target) {
            return
        }
        scheduledDrain?.cancel()
        scheduledDrainAtNanos = target
        scheduledDrain = scheduler.schedule(delay) { drainDue() }
    }

    private fun dueBacklogLocked(now: Long): Int =
        tasks.values.count { !it.closed && !it.inFlight && it.dueAtNanos <= now }

    private fun removeTaskLocked(task: TaskState): Boolean =
        if (tasks.remove(task.id) === task) {
            task.closed = true
            dueQueue.remove(task)
            true
        } else {
            false
        }

    private fun activitySnapshotLocked(): ActivitySnapshot =
        ActivitySnapshot(
            activeTasks = tasks.values.count { it.kind == TaskKind.SCHEDULED }.toDouble(),
            activeWatchdogs = tasks.values.count { it.kind == TaskKind.WATCHDOG }.toDouble(),
        )

    private fun emitActivityGauges(snapshot: ActivitySnapshot) {
        observer.emit(
            CoordinationObservationName.ACTIVE_TASKS,
            value = snapshot.activeTasks,
        )
        observer.emit(
            CoordinationObservationName.ACTIVE_WATCHDOGS,
            value = snapshot.activeWatchdogs,
        )
    }

    private fun emitObservations(observations: Iterable<PendingObservation>) {
        observations.forEach { observation ->
            observer.emit(observation.name, value = observation.value)
        }
    }

    private fun runCloseActions(actions: Iterable<() -> Unit>) {
        actions.forEach { action ->
            try {
                action()
            } catch (error: Exception) {
                log.warn(error) { "Coordination close listener failed" }
            }
        }
    }

    private fun runOwnershipLossActions(actions: Iterable<() -> Unit>) {
        actions.forEach(::runOwnershipLossAction)
    }

    private fun runOwnershipLossAction(action: () -> Unit) {
        try {
            action()
        } catch (error: Exception) {
            log.warn(error) { "Coordination ownership-loss listener failed" }
        }
    }

    inner class CoordinationObjectRegistration internal constructor(
        private val objectId: Long,
    ): AutoCloseable {
        val isClosed: Boolean
            get() = lock.withLock { objects[objectId]?.closed != false }

        fun registerTask(
            delay: Duration,
            task: () -> Unit,
        ): CoordinationTaskRegistration =
            registerTask(
                objectId = objectId,
                delay = delay,
                scheduledAction = task,
                ttl = null,
                renewalInterval = null,
                maxLifetime = null,
                generation = 0L,
                ownershipLossAction = null,
                renewAction = null,
            )

        fun registerWatchdog(
            ttl: Duration,
            renewalInterval: Duration,
            generation: Long,
            maxLifetime: Duration = Duration.INFINITE,
            onOwnershipLost: () -> Unit = {},
            renew: () -> CompletableFuture<CoordinationRenewalOutcome>,
        ): CoordinationTaskRegistration =
            registerTask(
                objectId = objectId,
                delay = renewalInterval,
                scheduledAction = null,
                ttl = ttl,
                renewalInterval = renewalInterval,
                maxLifetime = maxLifetime,
                generation = generation,
                ownershipLossAction = onOwnershipLost,
                renewAction = renew,
            )

        fun onClose(action: () -> Unit) {
            val invokeNow = lock.withLock {
                val objectState = objects[objectId]
                if (objectState == null || objectState.closed) {
                    true
                } else {
                    objectState.closeActions += action
                    false
                }
            }
            if (invokeNow) {
                action()
            }
        }

        override fun close() {
            closeObject(objectId)
        }
    }

    inner class CoordinationTaskRegistration internal constructor(
        private val task: TaskState,
    ): AutoCloseable {
        val isClosed: Boolean get() = lock.withLock { task.closed }

        fun updateGeneration(generation: Long) {
            lock.withLock {
                check(!task.closed && tasks[task.id] === task) { "coordination task is closed" }
                task.generation = generation
            }
        }

        override fun close() {
            val activitySnapshot = lock.withLock {
                if (removeTaskLocked(task)) activitySnapshotLocked() else null
            }
            activitySnapshot?.let(::emitActivityGauges)
        }
    }

    private data class ObjectState(
        val id: Long,
        val fingerprint: String,
        var closed: Boolean = false,
        val closeActions: MutableList<() -> Unit> = mutableListOf(),
    )

    private data class ActivitySnapshot(
        val activeTasks: Double,
        val activeWatchdogs: Double,
    )

    private data class PendingObservation(
        val name: CoordinationObservationName,
        val value: Double = 1.0,
    )

    internal data class TaskState(
        val id: Long,
        val objectId: Long,
        val objectFingerprint: String,
        val kind: TaskKind,
        var dueAtNanos: Long,
        var renewalDueAtNanos: Long = dueAtNanos,
        val ttlNanos: Long,
        var ttlDeadlineNanos: Long,
        val lifetimeDeadlineNanos: Long,
        val renewalIntervalNanos: Long,
        var generation: Long,
        val scheduledAction: (() -> Unit)?,
        val ownershipLossAction: (() -> Unit)?,
        val renewAction: (() -> CompletableFuture<CoordinationRenewalOutcome>)?,
        var inFlight: Boolean = false,
        var closed: Boolean = false,
    )

    internal enum class TaskKind {
        SCHEDULED,
        WATCHDOG,
    }

    companion object: KLogging() {
        private val FINGERPRINT_PATTERN = Regex("[A-Za-z0-9._-]{1,64}")
        private val MIN_WATCHDOG_TTL = 3.seconds
        private val REQUIRED_REDIS_MARGIN = 1.seconds
        private val registryLock = ReentrantLock()
        private val registry = mutableListOf<RuntimeRegistryEntry>()

        private fun earlyJitteredWatchdogDelay(interval: Duration): Duration {
            val factor = ThreadLocalRandom.current().nextDouble(
                MIN_WATCHDOG_DELAY_FACTOR,
                1.0,
            )
            return (interval * factor).coerceAtLeast(1.nanoseconds)
        }

        fun forConnection(
            connection: Any,
            ticker: MonotonicTicker = MonotonicTicker.SYSTEM,
            scheduler: CoordinationScheduler? = null,
            limits: CoordinationRuntimeLimits = CoordinationRuntimeLimits(),
            observer: CoordinationObserver = CoordinationObserver(),
        ): CoordinationRuntime =
            registryLock.withLock {
                registry.removeIf { it.connection.get() == null || it.runtime.isClosed }
                registry.firstOrNull { it.connection.get() === connection }
                    ?.runtime
                    ?: CoordinationRuntime(ticker, scheduler, limits, observer).also { runtime ->
                        registry += RuntimeRegistryEntry(WeakReference(connection), runtime)
                    }
            }

        private fun positiveDelay(targetNanos: Long, nowNanos: Long): Duration {
            if (targetNanos <= nowNanos) {
                return Duration.ZERO
            }
            val difference = targetNanos - nowNanos
            return (if (difference < 0L) Long.MAX_VALUE else difference).nanoseconds
        }

        private fun saturatingAdd(value: Long, positiveDelta: Long): Long =
            if (positiveDelta > 0L && value > Long.MAX_VALUE - positiveDelta) {
                Long.MAX_VALUE
            } else {
                value + positiveDelta
            }

        private fun saturatingMultiply(left: Long, right: Long): Long =
            if (left == 0L || right == 0L) {
                0L
            } else if (left > Long.MAX_VALUE / right) {
                Long.MAX_VALUE
            } else {
                left * right
            }
    }
}

private data class RuntimeRegistryEntry(
    val connection: WeakReference<Any>,
    val runtime: CoordinationRuntime,
)

private class ExecutorCoordinationScheduler: CoordinationScheduler {
    private val executor = ScheduledThreadPoolExecutor(
        1,
        ThreadFactory { task ->
            Thread(task, "bluetape4k-coordination-runtime").apply {
                isDaemon = true
            }
        },
    ).apply {
        removeOnCancelPolicy = true
    }

    override val isShutdown: Boolean get() = executor.isShutdown

    override fun schedule(
        delay: Duration,
        task: () -> Unit,
    ): CoordinationScheduledHandle {
        val future = executor.schedule(task, delay.inWholeNanoseconds, TimeUnit.NANOSECONDS)
        return CoordinationScheduledHandle { future.cancel(false) }
    }

    override fun shutdown() {
        executor.shutdownNow()
    }
}

private const val MIN_WATCHDOG_DELAY_FACTOR = 0.9
