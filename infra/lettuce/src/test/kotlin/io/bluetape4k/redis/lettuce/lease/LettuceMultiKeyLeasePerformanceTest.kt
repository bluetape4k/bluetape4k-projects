package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceConst
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCommandTimeoutException
import io.lettuce.core.RedisException
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.StringCodec
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.time.Instant
import java.util.Collections
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil

/**
 * 기본 test task 밖에서 Redis-side multi-key lease 비용을 특성화합니다.
 *
 * Lua script 또는 기본 `maxKeys`를 변경한 뒤
 * `:bluetape4k-lettuce:multiKeyLeasePerformanceTest`를 다시 실행합니다.
 * 절대 latency는 환경 의존적이므로 regression assertion은 여러 run의 normalized p95
 * 중앙값을 비교합니다. 이 test는 전용 Redis server와 explicit executor를 의도적으로
 * 소유합니다. shared launcher는 시도별 latency sample, persistent connection,
 * independently scheduled PING probe를 보존할 수 없어 사용하지 않습니다. 각 실행은
 * 독립적인 measurement window를 여러 번 기록하고 run-level p95 중앙값을 비교하므로 한
 * 번의 잡음 섞인 측정만으로 task가 실패하지 않습니다.
 */
@Tag("performance")
internal class LettuceMultiKeyLeasePerformanceTest {

    @Test
    fun `characterize lease latency throughput and connection responsiveness`() = runSuspendIO {
        val report = reportPath()
        Files.deleteIfExists(report)
        var rawRuns = emptyList<MeasurementRun>()
        var aggregatedResults = emptyList<PerformanceResult>()
        var redisVersion = "unknown"
        var executorType = "unknown"
        var executorPoolSize = 0
        var probeExecutorType = "unknown"
        var probeExecutorPoolSize = 0
        var redisVersionFailure: Throwable? = null
        var probeErrorCounter: AtomicInteger? = null
        var probeFailureRef: AtomicReference<Throwable?>? = null
        var measurementFailures = emptyList<MeasurementFailure>()
        val bodyFailure = try {
            RedisServer().use { server ->
                server.start()
                val client = LettuceClients.clientOf(server.host, server.port)
                val workloadConnections = mutableListOf<StatefulRedisConnection<String, String>>()
                var probeConnection: StatefulRedisConnection<String, String>? = null
                val workloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENCY) as ThreadPoolExecutor
                executorType = workloadExecutor.javaClass.name
                executorPoolSize = MAX_CONCURRENCY
                val probeExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
                probeExecutorType = probeExecutor.javaClass.name
                probeExecutorPoolSize = 1
                var probeTask: ScheduledFuture<*>? = null
                val currentProbeErrors = AtomicInteger()
                val currentProbeFailure = AtomicReference<Throwable?>()
                val currentProbeStopping = AtomicBoolean()
                probeErrorCounter = currentProbeErrors
                probeFailureRef = currentProbeFailure
                val operationFailure = try {
                    repeat(MAX_CONCURRENCY) {
                        workloadConnections += client.connect(StringCodec.UTF8)
                    }
                    probeConnection = client.connect(StringCodec.UTF8)
                    mergeRedisVersion(
                        RedisVersionInfo(redisVersion, redisVersionFailure),
                        readRedisVersion(probeConnection),
                    ).also {
                        redisVersion = it.version
                        redisVersionFailure = it.failure
                    }
                    val probeSamples = Collections.synchronizedList(mutableListOf<Long>())
                    val probeCompletions = AtomicInteger()
                    val currentMeasurementFailures =
                        Collections.synchronizedList(mutableListOf<MeasurementFailure>())
                    measurementFailures = currentMeasurementFailures
                    probeTask = probeExecutor.scheduleAtFixedRate(
                        {
                            val startedAt = System.nanoTime()
                            try {
                                probeConnection.sync().ping()
                                probeSamples += System.nanoTime() - startedAt
                            } catch (failure: Throwable) {
                                if (!currentProbeStopping.get()) {
                                    if (!failure.isRecoverableMeasurementFailure()) {
                                        currentProbeFailure.compareAndSet(null, failure)
                                    } else {
                                        currentProbeErrors.incrementAndGet()
                                    }
                                }
                            } finally {
                                probeCompletions.incrementAndGet()
                            }
                        },
                        0L,
                        PROBE_INTERVAL_MILLIS,
                        TimeUnit.MILLISECONDS,
                    )
                    val runId = Base58.randomString(12)
                    val completedRuns = mutableListOf<MeasurementRun>()
                    repeat(MEASUREMENT_RUNS) { runIndex ->
                        val results = COMBINATIONS.map { (keyCount, concurrency) ->
                            runCombination(
                                "$runId-$runIndex",
                                runIndex + 1,
                                keyCount,
                                concurrency,
                                workloadConnections,
                                workloadExecutor,
                                probeSamples,
                                probeCompletions,
                                currentMeasurementFailures,
                            )
                        }
                        completedRuns += MeasurementRun(runIndex + 1, results)
                        rawRuns = completedRuns.toList()
                    }

                    mergeRedisVersion(
                        RedisVersionInfo(redisVersion, redisVersionFailure),
                        readRedisVersion(probeConnection),
                    ).also {
                        redisVersion = it.version
                        redisVersionFailure = it.failure
                    }
                    aggregatedResults = aggregateResults(rawRuns)
                    CONCURRENCY_LEVELS.forEach { concurrency ->
                        val resultAt8 = aggregatedResults.single {
                            it.keyCount == 8 && it.concurrency == concurrency
                        }
                        val resultAt32 = aggregatedResults.single {
                            it.keyCount == 32 && it.concurrency == concurrency
                        }
                        resultAt32.normalizedAcquireP95Millis shouldBeLessOrEqualTo
                            resultAt8.normalizedAcquireP95Millis * NORMALIZED_P95_RATIO_LIMIT
                        resultAt32.acquireP95Millis shouldBeLessOrEqualTo
                            resultAt8.acquireP95Millis * NORMALIZED_P95_RATIO_LIMIT
                    }
                    aggregatedResults.forEach { result ->
                        result.errors.shouldBeZero()
                        result.timeouts.shouldBeZero()
                        result.probeSampleCount shouldBeGreaterOrEqualTo MIN_PROBE_SAMPLES * MEASUREMENT_RUNS
                        result.probeP99Millis shouldBeLessThan COMMAND_TIMEOUT.toMillis().toDouble()
                    }
                    null
                } catch (failure: Throwable) {
                    failure
                }
                val cleanupFailures = mutableListOf<Throwable>()
                fun attemptCleanup(block: () -> Unit) {
                    try {
                        block()
                    } catch (failure: Throwable) {
                        cleanupFailures += failure
                    }
                }
                attemptCleanup {
                    currentProbeStopping.set(true)
                    probeTask?.cancel(true)
                }
                attemptCleanup {
                    probeExecutor.shutdownNow()
                    probeExecutor.awaitTermination(10, TimeUnit.SECONDS).shouldBeTrue()
                }
                attemptCleanup {
                    workloadExecutor.shutdown()
                    if (!workloadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                        workloadExecutor.shutdownNow()
                        workloadExecutor.awaitTermination(10, TimeUnit.SECONDS).shouldBeTrue()
                    }
                }
                attemptCleanup { probeConnection?.close() }
                workloadConnections.asReversed().forEach { connection ->
                    attemptCleanup { connection.close() }
                }
                attemptCleanup { client.shutdown() }
                attemptCleanup { workloadExecutor.activeCount.shouldBeZero() }
                attemptCleanup {
                    probeFailureAfterTermination(probeFailureRef, probeErrorCounter)?.let { throw it }
                }
                combineFailure(operationFailure, cleanupFailures)?.let { throw it }
            }
            null
        } catch (failure: Throwable) {
            failure
        }
        val reportFailure = try {
            writeReport(
                results = aggregatedResults,
                redisVersion = redisVersion,
                status = if (bodyFailure == null) "passed" else "failed",
                failure = bodyFailure,
                redisVersionFailure = redisVersionFailure,
                measurementRuns = rawRuns,
                executorType = executorType,
                executorPoolSize = executorPoolSize,
                probeExecutorType = probeExecutorType,
                probeExecutorPoolSize = probeExecutorPoolSize,
                probeErrorCount = probeErrorCounter?.get() ?: 0,
                probeFailure = probeFailureRef?.get(),
                measurementFailures = measurementFailures.toList(),
            )
            null
        } catch (failure: Throwable) {
            failure
        }
        if (bodyFailure != null) {
            reportFailure?.let(bodyFailure::addSuppressed)
            throw bodyFailure
        }
        reportFailure?.let { throw it }
    }

    @Test
    fun `measurement uses enough rounds to make p95 stable`() {
        MEASURED_ROUNDS shouldBeGreaterOrEqualTo 300
    }

    @Test
    fun `performance result report carries sample count and aggregation metadata`() {
        val json = sampleResult().toJson()

        json shouldContain "\"acquireSampleCount\":"
        json shouldContain "\"aggregationPolicy\":"
    }

    @Test
    fun `aggregation ignores one noisy run by taking the median`() {
        val aggregate = aggregateResults(
            listOf(
                syntheticRun(1, 1.0),
                syntheticRun(2, 1.2),
                syntheticRun(3, 100.0),
            ),
        )

        aggregate.single { it.keyCount == 8 && it.concurrency == 1 }.acquireP95Millis shouldBeEqualTo 1.2
    }

    @Test
    fun `failed performance report identifies failed status and reason`() {
        val previousReportPath = System.getProperty(REPORT_PATH_PROPERTY)
        val temporaryReportPath = Files.createTempFile("multi-key-lease-failure-", ".json")
        System.setProperty(REPORT_PATH_PROPERTY, temporaryReportPath.toString())
        try {
            writeReport(listOf(sampleResult(errors = 1)), "8.8.1")

            val report = Files.readString(temporaryReportPath)
            report shouldContain "\"status\": \"failed\""
            report shouldContain "\"failure\":"
            report shouldContain "\"executorPoolSize\": 0"
            report shouldContain "\"probeErrorCount\": 0"
            report shouldContain "\"probeFailure\": null"
            report shouldContain "\"redisVersionFailure\": null"
        } finally {
            Files.deleteIfExists(temporaryReportPath)
            if (previousReportPath == null) {
                System.clearProperty(REPORT_PATH_PROPERTY)
            } else {
                System.setProperty(REPORT_PATH_PROPERTY, previousReportPath)
            }
        }
    }

    @Test
    fun `measurement error policy preserves non recoverable failures`() {
        RedisException("redis failure").isRecoverableMeasurementFailure().shouldBeTrue()
        TimeoutException().isRecoverableMeasurementFailure().shouldBeTrue()
        (!AssertionError().isRecoverableMeasurementFailure()).shouldBeTrue()
        (!CancellationException().isRecoverableMeasurementFailure()).shouldBeTrue()
    }

    @Test
    fun `redis version fallback preserves a known value when a later lookup fails`() {
        val failure = IllegalStateException("late version lookup failure")

        val merged = mergeRedisVersion(
            RedisVersionInfo(version = "8.8.1", failure = null),
            RedisVersionInfo(version = "unknown", failure = failure),
        )

        merged.version shouldBeEqualTo "8.8.1"
        merged.failure.shouldBeSameInstanceAs(failure)
    }

    @Test
    fun `round worker termination timeout becomes a fatal failure`() {
        val failure = awaitWorkerTermination(CountDownLatch(1), Duration.ofMillis(1))

        failure?.message!! shouldContain "round workers did not terminate"
    }

    @Test
    fun `late probe failure is retained after termination recheck`() {
        val probeFailure = AtomicReference<Throwable?>()
        val probeErrors = AtomicInteger()

        probeFailureAfterTermination(probeFailure, probeErrors) shouldBeEqualTo null
        val lateFailure = IllegalStateException("late probe failure")
        probeFailure.set(lateFailure)

        probeFailureAfterTermination(probeFailure, probeErrors).shouldBeSameInstanceAs(lateFailure)
    }

    @Test
    fun `round cleanup preserves primary failure`() {
        val primaryFailure = AssertionError("primary round failure")
        val cleanupFailure = IllegalStateException("cleanup failure")

        combineFailure(primaryFailure, listOf(cleanupFailure)).shouldBeSameInstanceAs(primaryFailure)
        primaryFailure.suppressed.single().shouldBeSameInstanceAs(cleanupFailure)
    }

    @Test
    fun `failed report preserves primary failure and escapes control characters`() {
        val failure = IllegalStateException("primary\u0000\u0008\u000C\u001F")
        val previousReportPath = System.getProperty(REPORT_PATH_PROPERTY)
        val temporaryReportPath = Files.createTempFile("multi-key-lease-primary-failure-", ".json")
        System.setProperty(REPORT_PATH_PROPERTY, temporaryReportPath.toString())
        try {
            writeReport(
                results = emptyList(),
                redisVersion = "unknown",
                status = "failed",
                failure = failure,
            )
            val json = Files.readString(temporaryReportPath)

            json shouldContain "\"status\": \"failed\""
            json shouldContain "IllegalStateException"
            json shouldContain "primary\\u0000\\b\\f\\u001f"
        } finally {
            Files.deleteIfExists(temporaryReportPath)
            if (previousReportPath == null) {
                System.clearProperty(REPORT_PATH_PROPERTY)
            } else {
                System.setProperty(REPORT_PATH_PROPERTY, previousReportPath)
            }
        }
    }

    private fun sampleResult(errors: Int = 0): PerformanceResult = PerformanceResult(
        keyCount = 8,
        concurrency = 1,
        acquireP50Millis = 0.8,
        acquireP95Millis = 1.2,
        normalizedAcquireP95Millis = 0.15,
        releaseP50Millis = 0.4,
        releaseP95Millis = 0.7,
        scenarioThroughputPerSecond = 1_000.0,
        probeP95Millis = 1.0,
        probeP99Millis = 1.5,
        acquireSampleCount = 100,
        releaseSampleCount = 100,
        probeSampleCount = 40,
        acquiredCount = 100,
        conflictedCount = 0,
        timeouts = 0,
        errors = errors,
    )

    private fun syntheticRun(run: Int, acquireP95Millis: Double): MeasurementRun = MeasurementRun(
        run = run,
        results = COMBINATIONS.map { (keyCount, concurrency) ->
            sampleResult().copy(
                keyCount = keyCount,
                concurrency = concurrency,
                acquireP95Millis = acquireP95Millis,
                normalizedAcquireP95Millis = acquireP95Millis / keyCount,
            )
        },
    )

    private fun runCombination(
        runId: String,
        runNumber: Int,
        keyCount: Int,
        concurrency: Int,
        connections: List<StatefulRedisConnection<String, String>>,
        executor: ThreadPoolExecutor,
        probeSamples: MutableList<Long>,
        probeCompletions: AtomicInteger,
        measurementFailures: MutableList<MeasurementFailure>,
    ): PerformanceResult {
        val tag = "perf-$runId-$keyCount-$concurrency"
        val keys = List(keyCount) { index -> "lease:{$tag}:$index" }
        val leases = connections.take(concurrency).map(::LettuceMultiKeyLease)
        val commands = connections.first().sync()
        val timeouts = AtomicInteger()
        val errors = AtomicInteger()

        repeat(WARM_UP_ROUNDS) { round ->
            runRound(keys, leases, commands, executor, round, timeouts, errors)
        }

        val probeStart = synchronized(probeSamples) { probeSamples.size }
        val acquireSamples = ArrayList<Long>(MEASURED_ROUNDS * concurrency)
        val releaseSamples = ArrayList<Long>(MEASURED_ROUNDS)
        var acquiredCount = 0
        var conflictedCount = 0
        val startedAt = System.nanoTime()
        repeat(MEASURED_ROUNDS) { round ->
            val errorsBeforeRound = errors.get()
            val timeoutsBeforeRound = timeouts.get()
            val measurement = try {
                runRound(
                    keys,
                    leases,
                    commands,
                    executor,
                    WARM_UP_ROUNDS + round,
                    timeouts,
                    errors,
                )
            } catch (failure: Throwable) {
                if (!failure.isRecoverableMeasurementFailure()) {
                    throw failure
                }
                if (errors.get() == errorsBeforeRound) errors.incrementAndGet()
                if (failure is TimeoutException || failure is RedisCommandTimeoutException) {
                    if (timeouts.get() == timeoutsBeforeRound) timeouts.incrementAndGet()
                }
                measurementFailures += MeasurementFailure(
                    run = runNumber,
                    round = WARM_UP_ROUNDS + round + 1,
                    keyCount = keyCount,
                    concurrency = concurrency,
                    cause = failure,
                )
                return@repeat
            }
            acquireSamples += measurement.acquireNanos
            releaseSamples += measurement.releaseNanos
            acquiredCount += measurement.acquiredCount
            conflictedCount += measurement.conflictedCount
        }
        if (acquireSamples.isEmpty() || releaseSamples.isEmpty()) {
            measurementFailures
                .lastOrNull { it.keyCount == keyCount && it.concurrency == concurrency }
                ?.cause
                ?.let { throw it }
            throw PerformanceFailure("no successful measured rounds for keyCount=$keyCount concurrency=$concurrency")
        }
        val elapsedNanos = System.nanoTime() - startedAt

        await()
            .atMost(Duration.ofSeconds(5))
            .until { executor.activeCount == 0 }
        commands.exists(*keys.toTypedArray()).shouldBeZero()
        val completionsAtWorkloadEnd = probeCompletions.get()
        await()
            .atMost(COMMAND_TIMEOUT)
            .until { probeCompletions.get() > completionsAtWorkloadEnd }
        val combinationProbeSamples = synchronized(probeSamples) {
            probeSamples.drop(probeStart)
        }
        val expectedProbeSamples = maxOf(
            MIN_PROBE_SAMPLES,
            (elapsedNanos / PROBE_INTERVAL_NANOS / PROBE_COVERAGE_DIVISOR).toInt(),
        )
        combinationProbeSamples.size shouldBeGreaterOrEqualTo expectedProbeSamples
        val operationCount = acquireSamples.size + releaseSamples.size
        return PerformanceResult(
            keyCount = keyCount,
            concurrency = concurrency,
            acquireP50Millis = acquireSamples.percentileMillis(50.0),
            acquireP95Millis = acquireSamples.percentileMillis(95.0),
            normalizedAcquireP95Millis = acquireSamples.percentileMillis(95.0) / keyCount,
            releaseP50Millis = releaseSamples.percentileMillis(50.0),
            releaseP95Millis = releaseSamples.percentileMillis(95.0),
            scenarioThroughputPerSecond = operationCount * NANOS_PER_SECOND.toDouble() / elapsedNanos,
            probeP95Millis = combinationProbeSamples.percentileMillis(95.0),
            probeP99Millis = combinationProbeSamples.percentileMillis(99.0),
            acquireSampleCount = acquireSamples.size,
            releaseSampleCount = releaseSamples.size,
            probeSampleCount = combinationProbeSamples.size,
            acquiredCount = acquiredCount,
            conflictedCount = conflictedCount,
            timeouts = timeouts.get(),
            errors = errors.get(),
        )
    }

    private fun readRedisVersion(connection: StatefulRedisConnection<String, String>): RedisVersionInfo = try {
        val version = connection.sync().info("server")
            .lineSequence()
            .firstOrNull { it.startsWith("redis_version:") }
            ?.substringAfter(':')
            ?.trim()
            ?.takeIf(String::isNotEmpty)
        if (version == null) {
            RedisVersionInfo(
                version = "unknown",
                failure = PerformanceFailure("Redis INFO response did not include redis_version"),
            )
        } else {
            RedisVersionInfo(version = version, failure = null)
        }
    } catch (failure: RedisException) {
        RedisVersionInfo(version = "unknown", failure = failure)
    }

    private fun mergeRedisVersion(current: RedisVersionInfo, observed: RedisVersionInfo): RedisVersionInfo {
        val mergedFailure = when {
            current.failure == null -> observed.failure
            observed.failure == null -> current.failure
            else -> current.failure.also { it.addSuppressed(observed.failure) }
        }
        return if (observed.version == "unknown") {
            current.copy(failure = mergedFailure)
        } else {
            observed.copy(failure = mergedFailure)
        }
    }

    private fun awaitWorkerTermination(
        completion: CountDownLatch,
        timeout: Duration = WORKER_TERMINATION_TIMEOUT,
    ): Throwable? = try {
        if (completion.await(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
            null
        } else {
            PerformanceFailure("round workers did not terminate within ${timeout.toMillis()} ms")
        }
    } catch (failure: InterruptedException) {
        Thread.currentThread().interrupt()
        failure
    }

    private fun Throwable.isRecoverableMeasurementFailure(): Boolean =
        this is RedisException || this is TimeoutException

    private fun probeFailureAfterTermination(
        probeFailure: AtomicReference<Throwable?>?,
        probeErrors: AtomicInteger?,
    ): Throwable? {
        probeFailure?.get()?.let { return it }
        val errorCount = probeErrors?.get() ?: 0
        return if (errorCount == 0) {
            null
        } else {
            PerformanceFailure("$errorCount probe ping failures")
        }
    }

    private fun combineFailure(primaryFailure: Throwable?, cleanupFailures: List<Throwable>): Throwable? {
        val cleanupFailure = cleanupFailures.firstOrNull()?.also { first ->
            cleanupFailures.drop(1).forEach(first::addSuppressed)
        }
        if (primaryFailure != null) {
            cleanupFailure?.let(primaryFailure::addSuppressed)
            return primaryFailure
        }
        return cleanupFailure
    }

    private fun aggregateResults(runs: List<MeasurementRun>): List<PerformanceResult> {
        runs.shouldNotBeEmpty()
        return COMBINATIONS.map { (keyCount, concurrency) ->
            val samples = runs.map { run ->
                run.results.single { it.keyCount == keyCount && it.concurrency == concurrency }
            }
            PerformanceResult(
                keyCount = keyCount,
                concurrency = concurrency,
                acquireP50Millis = samples.map { it.acquireP50Millis }.median(),
                acquireP95Millis = samples.map { it.acquireP95Millis }.median(),
            normalizedAcquireP95Millis = samples.map { it.normalizedAcquireP95Millis }.median(),
                releaseP50Millis = samples.map { it.releaseP50Millis }.median(),
                releaseP95Millis = samples.map { it.releaseP95Millis }.median(),
                scenarioThroughputPerSecond = samples.map { it.scenarioThroughputPerSecond }.median(),
                probeP95Millis = samples.map { it.probeP95Millis }.median(),
                probeP99Millis = samples.map { it.probeP99Millis }.median(),
                acquireSampleCount = samples.sumOf { it.acquireSampleCount },
                releaseSampleCount = samples.sumOf { it.releaseSampleCount },
                probeSampleCount = samples.sumOf { it.probeSampleCount },
                acquiredCount = samples.sumOf { it.acquiredCount },
                conflictedCount = samples.sumOf { it.conflictedCount },
                timeouts = samples.sumOf { it.timeouts },
                errors = samples.sumOf { it.errors },
            )
        }
    }

    private fun runRound(
        keys: List<String>,
        leases: List<LettuceMultiKeyLease>,
        commands: io.lettuce.core.api.sync.RedisCommands<String, String>,
        executor: ThreadPoolExecutor,
        round: Int,
        timeouts: AtomicInteger,
        errors: AtomicInteger,
    ): RoundMeasurement {
        commands.del(*keys.toTypedArray())
        val barrier = CyclicBarrier(leases.size)
        val workerCompletion = CountDownLatch(leases.size)
        val futures = leases.mapIndexed { index, lease ->
            executor.submit<AcquireAttempt> {
                try {
                    barrier.await()
                    val token = "owner-$round-$index-${Base58.randomString(22)}"
                    val startedAt = System.nanoTime()
                    try {
                        AcquireAttempt(
                            index,
                            token,
                            lease.acquire(keys, token, LEASE_TIME),
                            System.nanoTime() - startedAt,
                        )
                    } catch (failure: Throwable) {
                        if (failure is RedisCommandTimeoutException) timeouts.incrementAndGet()
                        errors.incrementAndGet()
                        throw failure
                    }
                } finally {
                    workerCompletion.countDown()
                }
            }
        }
        var measurement: RoundMeasurement? = null
        var operationFailure: Throwable? = null
        try {
            val attempts = futures.map { future ->
                try {
                    future.get(ROUND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                } catch (failure: TimeoutException) {
                    timeouts.incrementAndGet()
                    errors.incrementAndGet()
                    future.cancel(true)
                    throw failure
                } catch (failure: ExecutionException) {
                    throw failure.cause ?: failure
                }
            }
            val winners = attempts.filter { it.result == MultiKeyAcquireResult.Acquired }
            val losers = attempts.filter { it.result is MultiKeyAcquireResult.Conflicted }
            winners shouldHaveSize 1
            losers shouldHaveSize leases.size - 1
            attempts shouldHaveSize leases.size

            val winner = winners.single()
            val releaseStartedAt = System.nanoTime()
            leases[winner.index].release(keys, winner.token) shouldBeEqualTo MultiKeyReleaseResult.Released
            val releaseNanos = System.nanoTime() - releaseStartedAt
            commands.exists(*keys.toTypedArray()).shouldBeZero()
            measurement = RoundMeasurement(
                acquireNanos = attempts.map { it.acquireNanos },
                releaseNanos = releaseNanos,
                acquiredCount = winners.size,
                conflictedCount = losers.size,
            )
        } catch (failure: Throwable) {
            operationFailure = failure
        }
        val cleanupFailures = mutableListOf<Throwable>()
        fun attemptCleanup(block: () -> Unit) {
            try {
                block()
            } catch (failure: Throwable) {
                cleanupFailures += failure
            }
        }
        attemptCleanup {
            futures.forEach { future ->
                if (!future.isDone) future.cancel(true)
            }
        }
        var workerTerminationFailure: Throwable? = null
        attemptCleanup {
            workerTerminationFailure = awaitWorkerTermination(workerCompletion)
        }
        if (workerTerminationFailure == null) {
            attemptCleanup { commands.del(*keys.toTypedArray()) }
        }
        val primaryFailure = workerTerminationFailure?.also { terminationFailure ->
            operationFailure?.let(terminationFailure::addSuppressed)
        } ?: operationFailure
        combineFailure(primaryFailure, cleanupFailures)?.let { throw it }
        return checkNotNull(measurement)
    }

    private fun writeReport(
        results: List<PerformanceResult>,
        redisVersion: String,
        status: String? = null,
        failure: Throwable? = null,
        redisVersionFailure: Throwable? = null,
        measurementRuns: List<MeasurementRun> = emptyList(),
        executorType: String = "unknown",
        executorPoolSize: Int = 0,
        probeExecutorType: String = "unknown",
        probeExecutorPoolSize: Int = 0,
        probeErrorCount: Int = 0,
        probeFailure: Throwable? = null,
        measurementFailures: List<MeasurementFailure> = emptyList(),
    ) {
        val effectiveStatus = status ?: if (results.any { it.errors > 0 || it.timeouts > 0 }) {
            "failed"
        } else {
            "passed"
        }
        val effectiveFailure = failure ?: if (effectiveStatus == "failed") {
            PerformanceFailure("one or more performance samples failed")
        } else {
            null
        }
        val report = buildString {
            appendLine("{")
            appendLine("  \"redisImage\": ${("${RedisServer.IMAGE}:${RedisServer.TAG}").jsonString()},")
            appendLine("  \"status\": ${effectiveStatus.jsonString()},")
            appendLine("  \"generatedAt\": ${Instant.now().toString().jsonString()},")
            appendLine("  \"redisVersion\": ${redisVersion.jsonString()},")
            appendLine("  \"redisVersionFailure\": ${redisVersionFailure?.toJson() ?: "null"},")
            appendLine("  \"javaVersion\": ${System.getProperty("java.version").jsonString()},")
            appendLine("  \"kotlinVersion\": ${KotlinVersion.CURRENT.toString().jsonString()},")
            appendLine(
                "  \"lettuceVersion\": ${(RedisClient::class.java.`package`.implementationVersion
                    ?: "unknown").jsonString()},",
            )
            appendLine("  \"cpuCount\": ${Runtime.getRuntime().availableProcessors()},")
            appendLine("  \"executorType\": ${executorType.jsonString()},")
            appendLine("  \"executorPoolSize\": $executorPoolSize,")
            appendLine("  \"probeExecutorType\": ${probeExecutorType.jsonString()},")
            appendLine("  \"probeExecutorPoolSize\": $probeExecutorPoolSize,")
            appendLine("  \"probeErrorCount\": $probeErrorCount,")
            appendLine("  \"probeFailure\": ${probeFailure?.toJson() ?: "null"},")
            appendLine("  \"warmUpRounds\": $WARM_UP_ROUNDS,")
            appendLine("  \"measuredRounds\": $MEASURED_ROUNDS,")
            appendLine("  \"measurementRuns\": ${measurementRuns.size},")
            appendLine("  \"aggregationPolicy\": ${AGGREGATION_POLICY.jsonString()},")
            appendLine("  \"normalizedP95RatioLimit\": ${NORMALIZED_P95_RATIO_LIMIT.jsonNumber()},")
            appendLine(
                "  \"metricDirection\": { \"latency\": \"lower is better\", " +
                    "\"throughput\": \"higher is better\" },",
            )
            appendLine("  \"results\": [")
            results.forEachIndexed { index, result ->
                append("    ${result.toJson()}")
                appendLine(if (index == results.lastIndex) "" else ",")
            }
            appendLine("  ],")
            appendLine("  \"rawRuns\": [")
            measurementRuns.forEachIndexed { index, run ->
                append("    ${run.toJson()}")
                appendLine(if (index == measurementRuns.lastIndex) "" else ",")
            }
            appendLine("  ],")
            appendLine("  \"measurementFailures\": [")
            measurementFailures.forEachIndexed { index, measurementFailure ->
                append("    ${measurementFailure.toJson()}")
                appendLine(if (index == measurementFailures.lastIndex) "" else ",")
            }
            appendLine("  ],")
            appendLine("  \"failure\": ${effectiveFailure?.toJson() ?: "null"}")
            appendLine("}")
        }
        val reportPath = reportPath()
        Files.createDirectories(reportPath.parent)
        val temporaryPath = Files.createTempFile(reportPath.parent, "results-", ".json.tmp")
        try {
            Files.writeString(temporaryPath, report)
            Files.move(
                temporaryPath,
                reportPath,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } finally {
            Files.deleteIfExists(temporaryPath)
        }
    }

    private fun reportPath(): Path {
        System.getProperty(REPORT_PATH_PROPERTY)?.let { configuredPath ->
            return Path.of(configuredPath)
        }
        val workingDirectory = Path.of(System.getProperty("user.dir"))
        return if (workingDirectory.endsWith(Path.of("infra", "lettuce"))) {
            workingDirectory.resolve("build/reports/multi-key-lease-performance/results.json")
        } else {
            workingDirectory.resolve("infra/lettuce/build/reports/multi-key-lease-performance/results.json")
        }
    }

    private fun List<Long>.percentileMillis(percentile: Double): Double {
        val sorted = shouldNotBeEmpty().sorted()
        val index = (ceil(percentile / 100.0 * sorted.size).toInt() - 1).coerceIn(sorted.indices)
        return sorted[index] / NANOS_PER_MILLISECOND.toDouble()
    }

    private fun List<Double>.median(): Double {
        val sorted = shouldNotBeEmpty().sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2.0
        } else {
            sorted[middle]
        }
    }

    private fun PerformanceResult.toJson(): String = buildString {
        append("{")
        append("\"keyCount\":$keyCount,")
        append("\"concurrency\":$concurrency,")
        append("\"acquireP50Millis\":${acquireP50Millis.jsonNumber()},")
        append("\"acquireP95Millis\":${acquireP95Millis.jsonNumber()},")
        append("\"acquireP95MillisPerKey\":${normalizedAcquireP95Millis.jsonNumber()},")
        append("\"releaseP50Millis\":${releaseP50Millis.jsonNumber()},")
        append("\"releaseP95Millis\":${releaseP95Millis.jsonNumber()},")
        append("\"scenarioThroughputPerSecond\":${scenarioThroughputPerSecond.jsonNumber()},")
        append("\"probeP95Millis\":${probeP95Millis.jsonNumber()},")
        append("\"probeP99Millis\":${probeP99Millis.jsonNumber()},")
        append("\"acquireSampleCount\":$acquireSampleCount,")
        append("\"releaseSampleCount\":$releaseSampleCount,")
        append("\"probeSampleCount\":$probeSampleCount,")
        append("\"acquiredCount\":$acquiredCount,")
        append("\"conflictedCount\":$conflictedCount,")
        append("\"timeouts\":$timeouts,")
        append("\"errors\":$errors,")
        append("\"aggregationPolicy\":${AGGREGATION_POLICY.jsonString()}")
        append("}")
    }

    private fun MeasurementRun.toJson(): String = buildString {
        append("{\"run\":$run,\"results\":[")
        results.forEachIndexed { index, result ->
            append(result.toJson())
            if (index != results.lastIndex) append(',')
        }
        append("]}")
    }

    private fun MeasurementFailure.toJson(): String = buildString {
        append("{\"run\":$run,")
        append("\"round\":$round,")
        append("\"keyCount\":$keyCount,")
        append("\"concurrency\":$concurrency,")
        append("\"failure\":${cause.toJson()}}")
    }

    private fun Throwable.toJson(): String = buildString {
        append("{\"type\":")
        append((this@toJson::class.qualifiedName ?: "Throwable").jsonString())
        append(",\"message\":")
        append((message ?: "").jsonString())
        append('}')
    }

    private fun String.jsonString(): String = buildString {
        append('"')
        for (character in this@jsonString) {
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                else -> {
                    if (character.code < 0x20) {
                        append("\\u")
                        append(character.code.toString(16).padStart(4, '0'))
                    } else {
                        append(character)
                    }
                }
            }
        }
        append('"')
    }

    private fun Double.jsonNumber(): String = String.format(Locale.ROOT, "%.6f", this)

    private data class AcquireAttempt(
        val index: Int,
        val token: String,
        val result: MultiKeyAcquireResult,
        val acquireNanos: Long,
    )

    private data class RoundMeasurement(
        val acquireNanos: List<Long>,
        val releaseNanos: Long,
        val acquiredCount: Int,
        val conflictedCount: Int,
    )

    private data class MeasurementRun(
        val run: Int,
        val results: List<PerformanceResult>,
    )

    private data class MeasurementFailure(
        val run: Int,
        val round: Int,
        val keyCount: Int,
        val concurrency: Int,
        val cause: Throwable,
    )

    private data class RedisVersionInfo(
        val version: String,
        val failure: Throwable?,
    )

    private class PerformanceFailure(message: String) : RuntimeException(message)

    private data class PerformanceResult(
        val keyCount: Int,
        val concurrency: Int,
        val acquireP50Millis: Double,
        val acquireP95Millis: Double,
        val normalizedAcquireP95Millis: Double,
        val releaseP50Millis: Double,
        val releaseP95Millis: Double,
        val scenarioThroughputPerSecond: Double,
        val probeP95Millis: Double,
        val probeP99Millis: Double,
        val acquireSampleCount: Int,
        val releaseSampleCount: Int,
        val probeSampleCount: Int,
        val acquiredCount: Int,
        val conflictedCount: Int,
        val timeouts: Int,
        val errors: Int,
    )

    private companion object {
        val CONCURRENCY_LEVELS: List<Int> = listOf(1, 16)
        val COMBINATIONS: List<Pair<Int, Int>> = listOf(1 to 1, 32 to 16, 8 to 1, 1 to 16, 32 to 1, 8 to 16)
        val LEASE_TIME: Duration = Duration.ofSeconds(10)
        val COMMAND_TIMEOUT: Duration = Duration.ofMillis(LettuceConst.DEFAULT_TIMEOUT_MILLIS)
        const val AGGREGATION_POLICY: String = "median-of-run-p95"
        const val REPORT_PATH_PROPERTY: String = "bluetape4k.multiKeyLeasePerformance.report"
        const val MAX_CONCURRENCY: Int = 16
        const val WARM_UP_ROUNDS: Int = 20
        const val MEASUREMENT_RUNS: Int = 3
        const val MEASURED_ROUNDS: Int = 300
        const val NORMALIZED_P95_RATIO_LIMIT: Double = 4.0
        const val PROBE_INTERVAL_MILLIS: Long = 10L
        const val MIN_PROBE_SAMPLES: Int = 10
        const val PROBE_COVERAGE_DIVISOR: Long = 2L
        const val ROUND_TIMEOUT_SECONDS: Long = 30L
        val WORKER_TERMINATION_TIMEOUT: Duration = Duration.ofSeconds(5)
        const val NANOS_PER_MILLISECOND: Long = 1_000_000L
        const val NANOS_PER_SECOND: Long = 1_000_000_000L
        const val PROBE_INTERVAL_NANOS: Long = PROBE_INTERVAL_MILLIS * NANOS_PER_MILLISECOND
    }
}
