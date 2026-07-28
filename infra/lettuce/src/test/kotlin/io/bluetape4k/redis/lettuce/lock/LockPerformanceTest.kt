package io.bluetape4k.redis.lettuce.lock

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRenewalOutcome
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntime
import io.bluetape4k.redis.lettuce.coordination.internal.CoordinationRuntimeLimits
import io.bluetape4k.redis.lettuce.lock.internal.deriveDistributedLockKeys
import io.bluetape4k.redis.lettuce.lock.internal.deriveFairLockKeys
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.ScoredValue
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * coordination lock command, scheduler, latency, retained-state 한계를 특성화합니다.
 *
 * 절대 시간은 host마다 달라집니다. 따라서 이 suite는 정확한 command budget, hard cap, cleanup invariant,
 * and normalized within-run relationships. It is intentionally isolated from the default test task.
 */
@Tag("coordination-lock-performance")
internal class LockPerformanceTest {

    @Test
    fun `characterize bounded lock coordination`() = runSuspendIO {
        Files.deleteIfExists(reportPath())
        val schedulerResult = characterizeSchedulerCapacity()
        var redisResult: RedisCharacterization? = null
        var cleanupResult = CleanupState(
            redisStateEntries = -1L,
            runtimeTasks = -1,
            watchdogs = -1,
            executorsTerminated = false,
            connectionsClosed = false,
        )
        var redisVersion = "unknown"

        RedisServer().use { server ->
            server.start()
            val client = LettuceClients.clientOf(server.host, server.port)
            val connections = mutableListOf<StatefulRedisConnection<String, String>>()
            val workloadExecutor = Executors.newFixedThreadPool(WORKERS)
            val probeExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
            var bodyFailure: Throwable? = null
            try {
                repeat(WORKERS + 1) {
                    connections += client.connect(StringCodec.UTF8)
                }
                val workloadConnections = connections.take(WORKERS)
                val probeConnection = connections.last()
                val commands = workloadConnections.first().sync()
                redisVersion = commands.info("server")
                    .lineSequence()
                    .firstOrNull { it.startsWith("redis_version:") }
                    ?.substringAfter(':')
                    ?.trim()
                    ?: "unknown"
                redisResult = characterizeRedis(
                    connections = workloadConnections,
                    probeConnection = probeConnection,
                    workloadExecutor = workloadExecutor,
                    probeExecutor = probeExecutor,
                )
            } catch (failure: Throwable) {
                bodyFailure = failure
                throw failure
            } finally {
                val cleanupFailures = mutableListOf<Throwable>()
                fun cleanup(block: () -> Unit) {
                    try {
                        block()
                    } catch (failure: Throwable) {
                        cleanupFailures += failure
                    }
                }

                cleanup {
                    probeExecutor.shutdownNow()
                    probeExecutor.awaitTermination(CLEANUP_TIMEOUT_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
                }
                cleanup {
                    workloadExecutor.shutdown()
                    if (!workloadExecutor.awaitTermination(CLEANUP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        workloadExecutor.shutdownNow()
                        workloadExecutor.awaitTermination(CLEANUP_TIMEOUT_SECONDS, TimeUnit.SECONDS).shouldBeTrue()
                    }
                }
                val remainingEntries = connections.firstOrNull()
                    ?.takeIf { it.isOpen }
                    ?.sync()
                    ?.dbsize()
                    ?: 0L
                connections.asReversed().forEach { connection ->
                    cleanup { connection.close() }
                }
                cleanup { client.shutdown() }
                cleanupResult = CleanupState(
                    redisStateEntries = remainingEntries,
                    runtimeTasks = redisResult?.retainedState?.final?.runtimeTasks ?: -1,
                    watchdogs = redisResult?.retainedState?.final?.watchdogs ?: -1,
                    executorsTerminated = workloadExecutor.isTerminated && probeExecutor.isTerminated,
                    connectionsClosed = connections.all { !it.isOpen },
                )
                cleanupFailures.firstOrNull()?.let { first ->
                    cleanupFailures.drop(1).forEach(first::addSuppressed)
                    bodyFailure?.addSuppressed(first) ?: throw first
                }
            }
        }

        val measured = checkNotNull(redisResult)
        cleanupResult.executorsTerminated.shouldBeTrue()
        cleanupResult.connectionsClosed.shouldBeTrue()
        cleanupResult.runtimeTasks.shouldBeZero()
        cleanupResult.watchdogs.shouldBeZero()
        measured.errors.shouldBeZero()
        measured.timeouts.shouldBeZero()
        measured.commandBudget.cold shouldBeEqualTo COLD_COMMAND_BUDGET
        measured.commandBudget.warm shouldBeEqualTo WARM_COMMAND_BUDGET
        measured.fairCleanup.observedBatch shouldBeEqualTo FAIR_CLEANUP_BATCH
        measured.spin.observedAttemptsPerSecond shouldBeLessOrEqualTo
            measured.spin.configuredMaxAttemptsPerSecond.toDouble()
        measured.spin.busyLoopAttempts.shouldBeZero()
        measured.retainedState.final shouldBeEqualTo measured.retainedState.baseline
        assertWithinCaps(measured.retainedState.peak, measured.retainedState.caps)
        measured.retainedState.windows.size shouldBeEqualTo MEASUREMENT_WINDOWS
        measured.retainedState.hasMonotonicGrowth.shouldBeEqualTo(false)
        measured.latency.hotLockWaitP95Millis shouldBeGreaterOrEqualTo measured.latency.hotLockWaitP50Millis
        measured.latency.redisCommandP95Millis shouldBeGreaterOrEqualTo measured.latency.redisCommandP50Millis
        measured.responsiveness.pingP95Millis shouldBeGreaterOrEqualTo measured.responsiveness.pingP50Millis
        measured.responsiveness.sampleCount shouldBeGreaterThan 0
        writeReport(schedulerResult, measured, cleanupResult, redisVersion)
    }

    private fun characterizeSchedulerCapacity(): SchedulerCharacterization {
        val ticker = TestTicker()
        val scheduler = ManualCoordinationScheduler()
        val limits = CoordinationRuntimeLimits(
            maxRegistrations = REGISTRATION_CAP,
            maxWatchdogsPerTick = WATCHDOGS_PER_TICK,
            backlogCadence = BACKLOG_CADENCE_MILLIS.milliseconds,
        )
        val runtime = CoordinationRuntime(ticker = ticker, scheduler = scheduler, limits = limits)
        val registration = runtime.registerObject("performance-runtime")
        val tasks = List(REGISTRATION_CAP) {
            registration.registerTask(1.seconds) {}
        }
        val peakTasks = runtime.activeTasks
        peakTasks shouldBeEqualTo REGISTRATION_CAP

        ticker.advance(1.seconds)
        val tickDispatches = mutableListOf<Int>()
        var backlog = Int.MAX_VALUE
        while (backlog != 0) {
            val drain = runtime.drainDue()
            tickDispatches += drain.dispatched
            drain.dispatched shouldBeLessOrEqualTo WATCHDOGS_PER_TICK
            backlog = drain.dueBacklog
            if (backlog > 0) {
                drain.nextDrainDelay shouldBeEqualTo BACKLOG_CADENCE_MILLIS.milliseconds
                ticker.advance(BACKLOG_CADENCE_MILLIS.milliseconds)
            }
        }
        val snapshot = runtime.snapshot()
        snapshot.due shouldBeEqualTo REGISTRATION_CAP.toLong()
        snapshot.dispatched shouldBeEqualTo REGISTRATION_CAP.toLong()
        snapshot.late.shouldBeZero()
        snapshot.missed.shouldBeZero()
        runtime.activeTasks.shouldBeZero()
        tasks.forEach(AutoCloseable::close)
        registration.close()
        runtime.isClosed.shouldBeTrue()

        val calculatedDrainTicks = ceil(REGISTRATION_CAP.toDouble() / WATCHDOGS_PER_TICK).toInt()
        tickDispatches.size shouldBeEqualTo calculatedDrainTicks
        val watchdog = characterizeWatchdogTick()
        return SchedulerCharacterization(
            registrationCount = REGISTRATION_CAP,
            maxPerTick = WATCHDOGS_PER_TICK,
            backlogCadenceMillis = BACKLOG_CADENCE_MILLIS,
            tickDispatches = tickDispatches,
            due = snapshot.due,
            dispatched = snapshot.dispatched,
            late = snapshot.late,
            missed = snapshot.missed,
            maximumBacklog = snapshot.maximumBacklog,
            calculatedDrainTicks = calculatedDrainTicks,
            calculatedDrainMillis = (calculatedDrainTicks - 1) * BACKLOG_CADENCE_MILLIS,
            redisCompletionSafetyMarginMillis =
                WATCHDOG_TTL_MILLIS - WATCHDOG_RENEWAL_MILLIS - REQUIRED_REDIS_MARGIN_MILLIS,
            watchdogTickDispatch = watchdog.dispatched,
            watchdogDue = watchdog.due,
            watchdogDispatched = watchdog.dispatched,
            watchdogLate = watchdog.late,
            watchdogMissed = watchdog.missed,
            finalTasks = runtime.activeTasks,
            finalWatchdogs = watchdog.finalWatchdogs,
        )
    }

    private fun characterizeWatchdogTick(): WatchdogCharacterization {
        val ticker = TestTicker()
        val scheduler = ManualCoordinationScheduler()
        val runtime = CoordinationRuntime(
            ticker = ticker,
            scheduler = scheduler,
            limits = CoordinationRuntimeLimits(maxRegistrations = WATCHDOGS_PER_TICK),
            watchdogDelay = { it },
        )
        val registration = runtime.registerObject("performance-watchdogs")
        val watchdogs = List(WATCHDOGS_PER_TICK) { generation ->
            registration.registerWatchdog(
                ttl = 3.seconds,
                renewalInterval = 1.seconds,
                generation = generation.toLong() + 1L,
                maxLifetime = 10.seconds,
            ) {
                CompletableFuture.completedFuture(CoordinationRenewalOutcome.RENEWED)
            }
        }
        ticker.advance(1.seconds)
        runtime.drainDue().dispatched shouldBeEqualTo WATCHDOGS_PER_TICK
        val snapshot = runtime.snapshot()
        snapshot.due shouldBeEqualTo WATCHDOGS_PER_TICK.toLong()
        snapshot.dispatched shouldBeEqualTo WATCHDOGS_PER_TICK.toLong()
        snapshot.late.shouldBeZero()
        snapshot.missed.shouldBeZero()
        watchdogs.forEach(AutoCloseable::close)
        registration.close()
        return WatchdogCharacterization(
            due = snapshot.due,
            dispatched = snapshot.dispatched,
            late = snapshot.late,
            missed = snapshot.missed,
            finalWatchdogs = runtime.activeWatchdogs,
        )
    }

    private fun characterizeRedis(
        connections: List<StatefulRedisConnection<String, String>>,
        probeConnection: StatefulRedisConnection<String, String>,
        workloadExecutor: java.util.concurrent.ExecutorService,
        probeExecutor: ScheduledExecutorService,
    ): RedisCharacterization {
        val commands = connections.first().sync()
        val runId = Base58.randomString(12)
        val commandBudget = characterizeCommandBudget(connections.first(), runId)
        val fairCleanup = characterizeFairCleanup(connections.first(), runId)
        val spin = characterizeSpinRate(connections.first(), runId)
        val workloadName = "perf-$runId-hot"
        val workloadConfig = LockConfig(namespace = "bt4k:coord:perf")
        val workloadKeys = deriveDistributedLockKeys(workloadName, workloadConfig, StringCodec.UTF8)
        commands.del(*workloadKeys.all)
        val runtimes = connections.map(CoordinationRuntime::forConnection)
        val locks = connections.map { connection ->
            LettuceDistributedLock.create(connection, workloadName, workloadConfig)
        }
        val baseline = retainedCounts(runtimes, commands, workloadKeys)
        val holder = locks.first().tryAcquire(
            LockOwnerId.from("holder"),
            LockRequestId.from("holder-request"),
            LEASE,
        ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
        var peak = retainedCounts(runtimes, commands, workloadKeys)
        val errors = AtomicInteger()
        val timeouts = AtomicInteger()
        val probeSamples = Collections.synchronizedList(mutableListOf<Long>())
        val probeErrors = AtomicInteger()
        val probeTask = probeExecutor.scheduleAtFixedRate(
            {
                val startedAt = System.nanoTime()
                try {
                    probeConnection.sync().ping()
                    probeSamples += System.nanoTime() - startedAt
                } catch (_: Throwable) {
                    probeErrors.incrementAndGet()
                }
            },
            0L,
            PING_INTERVAL_MILLIS,
            TimeUnit.MILLISECONDS,
        )

        val measuredSamples = mutableListOf<Long>()
        val windows = mutableListOf<RetainedCounts>()
        val commandCountBeforeWorkload: Long
        val workloadCommands: Long
        try {
            runAttempts(
                attempts = WARMUP_ATTEMPTS,
                locks = locks,
                executor = workloadExecutor,
                prefix = "warmup",
                collectSamples = false,
                errors = errors,
                timeouts = timeouts,
            )
            commandCountBeforeWorkload = scriptCalls(commands)
            repeat(MEASUREMENT_WINDOWS) { window ->
                measuredSamples += runAttempts(
                    attempts = MEASURED_ATTEMPTS / MEASUREMENT_WINDOWS,
                    locks = locks,
                    executor = workloadExecutor,
                    prefix = "measured-$window",
                    collectSamples = true,
                    errors = errors,
                    timeouts = timeouts,
                )
                val retained = retainedCounts(runtimes, commands, workloadKeys)
                windows += retained
                peak = peak.max(retained)
            }
            workloadCommands = scriptCalls(commands) - commandCountBeforeWorkload
        } finally {
            probeTask.cancel(true)
            locks.first().release(holder) shouldBeEqualTo LockMutationResult.Released(0)
            locks.asReversed().forEach(AutoCloseable::close)
            commands.del(*workloadKeys.all)
        }
        val final = retainedCounts(runtimes, commands, workloadKeys)
        val probeCopy = synchronized(probeSamples) { probeSamples.toList() }
        probeErrors.get().shouldBeZero()
        workloadCommands shouldBeEqualTo MEASURED_ATTEMPTS.toLong()
        measuredSamples.size shouldBeEqualTo MEASURED_ATTEMPTS
        val retainedState = RetainedState(
            baseline = baseline,
            peak = peak,
            final = final,
            caps = RetainedCounts(
                runtimeTasks = REGISTRATION_CAP,
                watchdogs = REGISTRATION_CAP,
                waiters = QUEUE_CAP,
                queueEntries = QUEUE_CAP,
                requestHolds = REQUEST_HOLD_CAP,
            ),
            windows = windows,
            hasMonotonicGrowth = windows.hasStrictMonotonicGrowth(),
        )

        return RedisCharacterization(
            runId = runId,
            commandBudget = commandBudget.copy(workload = workloadCommands),
            fairCleanup = fairCleanup,
            spin = spin,
            latency = LatencyCharacterization(
                hotLockWaitP50Millis = measuredSamples.percentileMillis(50.0),
                hotLockWaitP95Millis = measuredSamples.percentileMillis(95.0),
                redisCommandP50Millis = measuredSamples.percentileMillis(50.0),
                redisCommandP95Millis = measuredSamples.percentileMillis(95.0),
            ),
            responsiveness = ResponsivenessCharacterization(
                pingP50Millis = probeCopy.percentileMillis(50.0),
                pingP95Millis = probeCopy.percentileMillis(95.0),
                sampleCount = probeCopy.size,
                errors = probeErrors.get(),
            ),
            retainedState = retainedState,
            errors = errors.get(),
            timeouts = timeouts.get(),
        )
    }

    private fun characterizeCommandBudget(
        connection: StatefulRedisConnection<String, String>,
        runId: String,
    ): CommandBudget {
        val commands = connection.sync()
        val name = "perf-$runId-budget"
        val keys = deriveDistributedLockKeys(name, LockConfig(), StringCodec.UTF8)
        val lock = LettuceDistributedLock.create(connection, name)
        commands.del(*keys.all)
        return try {
            commands.scriptFlush()
            val coldBefore = scriptCalls(commands)
            val cold = lock.tryAcquire(
                LockOwnerId.from("cold-owner"),
                LockRequestId.from("cold-request"),
                LEASE,
            ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
            val coldCommands = scriptCalls(commands) - coldBefore
            lock.release(cold)

            val warmBefore = scriptCalls(commands)
            val warm = lock.tryAcquire(
                LockOwnerId.from("warm-owner"),
                LockRequestId.from("warm-request"),
                LEASE,
            ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
            val warmCommands = scriptCalls(commands) - warmBefore
            lock.release(warm)
            CommandBudget(cold = coldCommands, warm = warmCommands, workload = 0L)
        } finally {
            lock.close()
            commands.del(*keys.all)
        }
    }

    private fun characterizeFairCleanup(
        connection: StatefulRedisConnection<String, String>,
        runId: String,
    ): FairCleanupCharacterization {
        val commands = connection.sync()
        val config = FairLockConfig(cleanupBatchSize = FAIR_CLEANUP_BATCH)
        val name = "perf-$runId-fair"
        val keys = deriveFairLockKeys(name, config, StringCodec.UTF8)
        val lock = LettuceFairLock.create(connection, name, config)
        commands.del(*keys.all)
        return try {
            val staleCount = FAIR_CLEANUP_BATCH + 1
            commands.hset(
                keys.waiters,
                (1..staleCount).associate { index -> "stale-$index" to "$index|1|1" },
            )
            commands.zadd(
                keys.queue,
                *(1..staleCount)
                    .map { index -> ScoredValue.just(index.toDouble(), "stale-$index") }
                    .toTypedArray(),
            )
            commands.set(keys.sequence, staleCount.toString())
            val before = commands.zcard(keys.queue)
            lock.tryAcquire(
                LockOwnerId.from("fair-owner"),
                LockRequestId.from("fair-request"),
                LEASE,
            ) shouldBeEqualTo LockAcquireResult.CleanupPending
            val after = commands.zcard(keys.queue)
            FairCleanupCharacterization(
                configuredBatchCap = FAIR_CLEANUP_BATCH,
                observedBatch = (before - after).toInt(),
                remainingAfterFirstPass = after.toInt(),
            )
        } finally {
            lock.close()
            commands.del(*keys.all)
        }
    }

    private fun characterizeSpinRate(
        connection: StatefulRedisConnection<String, String>,
        runId: String,
    ): SpinCharacterization {
        val commands = connection.sync()
        val name = "perf-$runId-spin"
        val config = SpinLockConfig(maxAttemptsPerSecond = SPIN_CONFIGURED_ATTEMPTS_PER_SECOND)
        val keys = deriveDistributedLockKeys(name, config.lock, StringCodec.UTF8)
        val lock = LettuceSpinLock.create(connection, name, config)
        commands.del(*keys.all)
        return try {
            val holder = lock.tryAcquire(
                LockOwnerId.from("spin-holder"),
                LockRequestId.from("spin-holder-request"),
                LEASE,
            ).shouldBeInstanceOf<LockAcquireResult.Acquired<LockHandle>>().handle
            val callsBefore = scriptCalls(commands)
            val startedAt = System.nanoTime()
            lock.acquire(
                LockOwnerId.from("spin-contender"),
                LockRequestId.from("spin-contender-request"),
                SPIN_WAIT,
                LEASE,
            ) shouldBeEqualTo LockAcquireResult.TimedOut
            val elapsedNanos = System.nanoTime() - startedAt
            val attempts = scriptCalls(commands) - callsBefore
            lock.release(holder)
            val attemptsPerSecond = attempts * NANOS_PER_SECOND.toDouble() / elapsedNanos
            SpinCharacterization(
                configuredMaxAttemptsPerSecond = SPIN_CONFIGURED_ATTEMPTS_PER_SECOND,
                observedAttempts = attempts,
                elapsedMillis = elapsedNanos / NANOS_PER_MILLISECOND.toDouble(),
                observedAttemptsPerSecond = attemptsPerSecond,
                busyLoopAttempts = if (attemptsPerSecond > SPIN_CONFIGURED_ATTEMPTS_PER_SECOND) {
                    attempts.toInt()
                } else {
                    0
                },
            )
        } finally {
            lock.close()
            commands.del(*keys.all)
        }
    }

    private fun runAttempts(
        attempts: Int,
        locks: List<LettuceDistributedLock>,
        executor: java.util.concurrent.ExecutorService,
        prefix: String,
        collectSamples: Boolean,
        errors: AtomicInteger,
        timeouts: AtomicInteger,
    ): List<Long> {
        val base = attempts / WORKERS
        val remainder = attempts % WORKERS
        return locks.mapIndexed { worker, lock ->
            executor.submit<List<Long>> {
                val count = base + if (worker < remainder) 1 else 0
                val samples = if (collectSamples) ArrayList<Long>(count) else emptyList<Long>()
                repeat(count) { index ->
                    val startedAt = System.nanoTime()
                    val result = try {
                        lock.tryAcquire(
                            LockOwnerId.from("contender-$worker"),
                            LockRequestId.from("$prefix-$worker-$index"),
                            LEASE,
                        )
                    } catch (_: Throwable) {
                        null
                    }
                    val elapsed = System.nanoTime() - startedAt
                    if (collectSamples) {
                        (samples as ArrayList<Long>) += elapsed
                    }
                    when (result) {
                        is LockAcquireResult.Contended -> Unit
                        LockAcquireResult.TimedOut -> timeouts.incrementAndGet()
                        else -> errors.incrementAndGet()
                    }
                }
                samples
            }
        }.flatMap { future ->
            future.get(WORKLOAD_TIMEOUT_MINUTES, TimeUnit.MINUTES)
        }
    }

    private fun retainedCounts(
        runtimes: List<CoordinationRuntime>,
        commands: RedisCommands<String, String>,
        keys: io.bluetape4k.redis.lettuce.lock.internal.DistributedLockKeys,
    ): RetainedCounts =
        RetainedCounts(
            runtimeTasks = runtimes.sumOf(CoordinationRuntime::activeTasks),
            watchdogs = runtimes.sumOf(CoordinationRuntime::activeWatchdogs),
            waiters = 0,
            queueEntries = 0,
            requestHolds = commands.hlen(keys.holds).toInt(),
        )

    private fun scriptCalls(commands: RedisCommands<String, String>): Long =
        COMMAND_CALL_PATTERN.findAll(commands.info("commandstats"))
            .sumOf { match -> match.groupValues[2].toLong() }

    private fun assertWithinCaps(actual: RetainedCounts, caps: RetainedCounts) {
        actual.runtimeTasks shouldBeLessOrEqualTo caps.runtimeTasks
        actual.watchdogs shouldBeLessOrEqualTo caps.watchdogs
        actual.waiters shouldBeLessOrEqualTo caps.waiters
        actual.queueEntries shouldBeLessOrEqualTo caps.queueEntries
        actual.requestHolds shouldBeLessOrEqualTo caps.requestHolds
    }

    private fun List<RetainedCounts>.hasStrictMonotonicGrowth(): Boolean {
        fun increasing(selector: (RetainedCounts) -> Int): Boolean =
            zipWithNext().all { (left, right) -> selector(right) > selector(left) }
        return increasing(RetainedCounts::runtimeTasks) ||
            increasing(RetainedCounts::watchdogs) ||
            increasing(RetainedCounts::waiters) ||
            increasing(RetainedCounts::queueEntries) ||
            increasing(RetainedCounts::requestHolds)
    }

    private fun RetainedCounts.max(other: RetainedCounts): RetainedCounts =
        RetainedCounts(
            runtimeTasks = maxOf(runtimeTasks, other.runtimeTasks),
            watchdogs = maxOf(watchdogs, other.watchdogs),
            waiters = maxOf(waiters, other.waiters),
            queueEntries = maxOf(queueEntries, other.queueEntries),
            requestHolds = maxOf(requestHolds, other.requestHolds),
        )

    private fun List<Long>.percentileMillis(percentile: Double): Double {
        isNotEmpty().shouldBeTrue()
        val sorted = sorted()
        val index = (ceil(percentile / 100.0 * sorted.size).toInt() - 1).coerceIn(sorted.indices)
        return sorted[index] / NANOS_PER_MILLISECOND.toDouble()
    }

    private fun writeReport(
        scheduler: SchedulerCharacterization,
        redis: RedisCharacterization,
        cleanup: CleanupState,
        redisVersion: String,
    ) {
        val report = buildString {
            appendLine("{")
            appendLine("  \"schemaVersion\": $REPORT_SCHEMA_VERSION,")
            appendLine("  \"status\": \"passed\",")
            appendLine("  \"tag\": \"coordination-lock-performance\",")
            appendLine("  \"publication\": \"sibling-temp-fsync-atomic-move\",")
            appendLine("  \"runId\": ${redis.runId.jsonString()},")
            appendLine("  \"generatedAt\": ${Instant.now().toString().jsonString()},")
            appendLine("  \"metadata\": {")
            appendLine("    \"redisImage\": ${"${RedisServer.IMAGE}:${RedisServer.TAG}".jsonString()},")
            appendLine("    \"redisVersion\": ${redisVersion.jsonString()},")
            appendLine("    \"javaVersion\": ${System.getProperty("java.version").jsonString()},")
            appendLine("    \"kotlinVersion\": ${KotlinVersion.CURRENT.toString().jsonString()},")
            appendLine("    \"lettuceVersion\": ${(RedisClient::class.java.`package`.implementationVersion ?: "unknown").jsonString()},")
            appendLine("    \"osName\": ${System.getProperty("os.name").jsonString()},")
            appendLine("    \"osVersion\": ${System.getProperty("os.version").jsonString()},")
            appendLine("    \"osArch\": ${System.getProperty("os.arch").jsonString()},")
            appendLine("    \"cpuCount\": ${Runtime.getRuntime().availableProcessors()}")
            appendLine("  },")
            appendLine("  \"protocol\": {")
            appendLine("    \"usesDedicatedRedis\": true,")
            appendLine("    \"usesDedicatedConnections\": true,")
            appendLine("    \"usesDedicatedExecutors\": true,")
            appendLine("    \"usesSeparatePingConnection\": true,")
            appendLine("    \"usesSeparatePingExecutor\": true,")
            appendLine("    \"explicitCleanup\": true")
            appendLine("  },")
            appendLine("  \"workload\": {")
            appendLine("    \"warmupAttempts\": $WARMUP_ATTEMPTS,")
            appendLine("    \"measuredAttempts\": $MEASURED_ATTEMPTS,")
            appendLine("    \"workers\": $WORKERS,")
            appendLine("    \"measurementWindows\": $MEASUREMENT_WINDOWS")
            appendLine("  },")
            appendLine("  \"commandBudget\": ${redis.commandBudget.toJson()},")
            appendLine("  \"fairCleanup\": ${redis.fairCleanup.toJson()},")
            appendLine("  \"spin\": ${redis.spin.toJson()},")
            appendLine("  \"latency\": ${redis.latency.toJson()},")
            appendLine("  \"responsiveness\": ${redis.responsiveness.toJson()},")
            appendLine("  \"retainedState\": ${redis.retainedState.toJson()},")
            appendLine("  \"scheduler\": ${scheduler.toJson()},")
            appendLine("  \"errors\": ${redis.errors},")
            appendLine("  \"timeouts\": ${redis.timeouts},")
            appendLine("  \"cleanup\": ${cleanup.toJson()}")
            appendLine("}")
        }
        val target = reportPath()
        Files.createDirectories(target.parent)
        val temporary = Files.createTempFile(target.parent, "results-", ".json.tmp")
        try {
            FileChannel.open(
                temporary,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING,
            ).use { channel ->
                val bytes = ByteBuffer.wrap(report.toByteArray(StandardCharsets.UTF_8))
                while (bytes.hasRemaining()) {
                    channel.write(bytes)
                }
                channel.force(true)
            }
            Files.move(
                temporary,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun reportPath(): Path {
        val workingDirectory = Path.of(System.getProperty("user.dir"))
        return if (workingDirectory.endsWith(Path.of("infra", "lettuce"))) {
            workingDirectory.resolve("build/reports/coordination-lock-performance/results.json")
        } else {
            workingDirectory.resolve("infra/lettuce/build/reports/coordination-lock-performance/results.json")
        }
    }

    private fun String.jsonString(): String =
        buildString {
            append('"')
            this@jsonString.forEach { char ->
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
            append('"')
        }

    private fun Double.jsonNumber(): String = String.format(Locale.ROOT, "%.6f", this)

    private fun CommandBudget.toJson(): String =
        "{\"cold\":$cold,\"warm\":$warm,\"workload\":$workload}"

    private fun FairCleanupCharacterization.toJson(): String =
        "{\"configuredBatchCap\":$configuredBatchCap,\"observedBatch\":$observedBatch," +
            "\"remainingAfterFirstPass\":$remainingAfterFirstPass}"

    private fun SpinCharacterization.toJson(): String =
        "{\"configuredMaxAttemptsPerSecond\":$configuredMaxAttemptsPerSecond," +
            "\"observedAttempts\":$observedAttempts,\"elapsedMillis\":${elapsedMillis.jsonNumber()}," +
            "\"observedAttemptsPerSecond\":${observedAttemptsPerSecond.jsonNumber()}," +
            "\"busyLoopAttempts\":$busyLoopAttempts}"

    private fun LatencyCharacterization.toJson(): String =
        "{\"hotLockWaitP50Millis\":${hotLockWaitP50Millis.jsonNumber()}," +
            "\"hotLockWaitP95Millis\":${hotLockWaitP95Millis.jsonNumber()}," +
            "\"redisCommandP50Millis\":${redisCommandP50Millis.jsonNumber()}," +
            "\"redisCommandP95Millis\":${redisCommandP95Millis.jsonNumber()}}"

    private fun ResponsivenessCharacterization.toJson(): String =
        "{\"pingP50Millis\":${pingP50Millis.jsonNumber()}," +
            "\"pingP95Millis\":${pingP95Millis.jsonNumber()}," +
            "\"sampleCount\":$sampleCount,\"errors\":$errors}"

    private fun RetainedState.toJson(): String =
        "{\"baseline\":${baseline.toJson()},\"peak\":${peak.toJson()},\"final\":${final.toJson()}," +
            "\"caps\":${caps.toJson()},\"windows\":[${windows.joinToString(",") { it.toJson() }}]," +
            "\"hasMonotonicGrowth\":$hasMonotonicGrowth}"

    private fun RetainedCounts.toJson(): String =
        "{\"runtimeTasks\":$runtimeTasks,\"watchdogs\":$watchdogs,\"waiters\":$waiters," +
            "\"queueEntries\":$queueEntries,\"requestHolds\":$requestHolds}"

    private fun SchedulerCharacterization.toJson(): String =
        "{\"registrationCount\":$registrationCount,\"maxPerTick\":$maxPerTick," +
            "\"backlogCadenceMillis\":$backlogCadenceMillis," +
            "\"tickDispatches\":[${tickDispatches.joinToString(",")}],\"due\":$due," +
            "\"dispatched\":$dispatched,\"late\":$late,\"missed\":$missed," +
            "\"maximumBacklog\":$maximumBacklog,\"calculatedDrainTicks\":$calculatedDrainTicks," +
            "\"calculatedDrainMillis\":$calculatedDrainMillis," +
            "\"redisCompletionSafetyMarginMillis\":$redisCompletionSafetyMarginMillis," +
            "\"watchdogTickDispatch\":$watchdogTickDispatch,\"watchdogDue\":$watchdogDue," +
            "\"watchdogDispatched\":$watchdogDispatched,\"watchdogLate\":$watchdogLate," +
            "\"watchdogMissed\":$watchdogMissed," +
            "\"finalTasks\":$finalTasks,\"finalWatchdogs\":$finalWatchdogs}"

    private fun CleanupState.toJson(): String =
        "{\"redisStateEntries\":$redisStateEntries,\"runtimeTasks\":$runtimeTasks," +
            "\"watchdogs\":$watchdogs,\"executorsTerminated\":$executorsTerminated," +
            "\"connectionsClosed\":$connectionsClosed}"

    private data class CommandBudget(
        val cold: Long,
        val warm: Long,
        val workload: Long,
    )

    private data class FairCleanupCharacterization(
        val configuredBatchCap: Int,
        val observedBatch: Int,
        val remainingAfterFirstPass: Int,
    )

    private data class SpinCharacterization(
        val configuredMaxAttemptsPerSecond: Int,
        val observedAttempts: Long,
        val elapsedMillis: Double,
        val observedAttemptsPerSecond: Double,
        val busyLoopAttempts: Int,
    )

    private data class LatencyCharacterization(
        val hotLockWaitP50Millis: Double,
        val hotLockWaitP95Millis: Double,
        val redisCommandP50Millis: Double,
        val redisCommandP95Millis: Double,
    )

    private data class ResponsivenessCharacterization(
        val pingP50Millis: Double,
        val pingP95Millis: Double,
        val sampleCount: Int,
        val errors: Int,
    )

    private data class RetainedCounts(
        val runtimeTasks: Int,
        val watchdogs: Int,
        val waiters: Int,
        val queueEntries: Int,
        val requestHolds: Int,
    )

    private data class RetainedState(
        val baseline: RetainedCounts,
        val peak: RetainedCounts,
        val final: RetainedCounts,
        val caps: RetainedCounts,
        val windows: List<RetainedCounts>,
        val hasMonotonicGrowth: Boolean,
    )

    private data class SchedulerCharacterization(
        val registrationCount: Int,
        val maxPerTick: Int,
        val backlogCadenceMillis: Long,
        val tickDispatches: List<Int>,
        val due: Long,
        val dispatched: Long,
        val late: Long,
        val missed: Long,
        val maximumBacklog: Int,
        val calculatedDrainTicks: Int,
        val calculatedDrainMillis: Long,
        val redisCompletionSafetyMarginMillis: Long,
        val watchdogTickDispatch: Long,
        val watchdogDue: Long,
        val watchdogDispatched: Long,
        val watchdogLate: Long,
        val watchdogMissed: Long,
        val finalTasks: Int,
        val finalWatchdogs: Int,
    )

    private data class WatchdogCharacterization(
        val due: Long,
        val dispatched: Long,
        val late: Long,
        val missed: Long,
        val finalWatchdogs: Int,
    )

    private data class RedisCharacterization(
        val runId: String,
        val commandBudget: CommandBudget,
        val fairCleanup: FairCleanupCharacterization,
        val spin: SpinCharacterization,
        val latency: LatencyCharacterization,
        val responsiveness: ResponsivenessCharacterization,
        val retainedState: RetainedState,
        val errors: Int,
        val timeouts: Int,
    )

    private data class CleanupState(
        val redisStateEntries: Long,
        val runtimeTasks: Int,
        val watchdogs: Int,
        val executorsTerminated: Boolean,
        val connectionsClosed: Boolean,
    )

    private companion object {
        val LEASE = LeasePolicy.Fixed(Duration.ofMinutes(5))
        val SPIN_WAIT: Duration = Duration.ofSeconds(1)
        val COMMAND_CALL_PATTERN = Regex("cmdstat_(evalsha|eval):calls=(\\d+)")
        const val REPORT_SCHEMA_VERSION = 1
        const val WORKERS = 8
        const val WARMUP_ATTEMPTS = 10_000
        const val MEASURED_ATTEMPTS = 50_000
        const val MEASUREMENT_WINDOWS = 5
        const val COLD_COMMAND_BUDGET = 2L
        const val WARM_COMMAND_BUDGET = 1L
        const val FAIR_CLEANUP_BATCH = 64
        const val QUEUE_CAP = 10_000
        const val REQUEST_HOLD_CAP = 10_000
        const val REGISTRATION_CAP = 10_000
        const val WATCHDOGS_PER_TICK = 256
        const val BACKLOG_CADENCE_MILLIS = 25L
        const val WATCHDOG_TTL_MILLIS = 3_000L
        const val WATCHDOG_RENEWAL_MILLIS = 1_000L
        const val REQUIRED_REDIS_MARGIN_MILLIS = 1_000L
        const val SPIN_CONFIGURED_ATTEMPTS_PER_SECOND = 20
        const val PING_INTERVAL_MILLIS = 10L
        const val CLEANUP_TIMEOUT_SECONDS = 10L
        const val WORKLOAD_TIMEOUT_MINUTES = 3L
        const val NANOS_PER_MILLISECOND = 1_000_000L
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
