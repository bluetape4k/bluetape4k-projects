package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldBeZero
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.codec.Base58
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceConst
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisCommandTimeoutException
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
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

/**
 * 기본 test task 밖에서 Redis-side multi-key lease 비용을 특성화합니다.
 *
 * Lua script 또는 기본 `maxKeys`를 변경한 뒤 `:bluetape4k-lettuce:multiKeyLeasePerformanceTest`를 다시 실행합니다.
 * 절대 latency는 환경 의존적이므로 regression assertion은 한 run 안의 normalized p95 값을 비교합니다.
 * 이 test는 전용 Redis server와 explicit executor를 의도적으로 소유합니다. shared launcher는 오염시킬 수 있습니다
 * latency samples, while `MultithreadingTester` cannot preserve per-attempt timing, persistent connections, and the
 * independently scheduled PING probe that this characterization requires.
 */
@Tag("performance")
internal class LettuceMultiKeyLeasePerformanceTest {

    @Test
    fun `characterize lease latency throughput and connection responsiveness`() = runSuspendIO {
        Files.deleteIfExists(reportPath())
        var passedResults: List<PerformanceResult>? = null
        var passedRedisVersion: String? = null
        RedisServer().use { server ->
            server.start()
            val client = LettuceClients.clientOf(server.host, server.port)
            val workloadConnections = mutableListOf<StatefulRedisConnection<String, String>>()
            var probeConnection: StatefulRedisConnection<String, String>? = null
            val workloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENCY) as ThreadPoolExecutor
            val probeExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
            var bodyFailure: Throwable? = null
            try {
                repeat(MAX_CONCURRENCY) {
                    workloadConnections += client.connect(StringCodec.UTF8)
                }
                probeConnection = client.connect(StringCodec.UTF8)
                val probeSamples = Collections.synchronizedList(mutableListOf<Long>())
                val probeErrors = AtomicInteger()
                val probeCompletions = AtomicInteger()
                val probeTask = probeExecutor.scheduleAtFixedRate(
                    {
                        val startedAt = System.nanoTime()
                        try {
                            probeConnection.sync().ping()
                            probeSamples += System.nanoTime() - startedAt
                        } catch (_: Throwable) {
                            probeErrors.incrementAndGet()
                        } finally {
                            probeCompletions.incrementAndGet()
                        }
                    },
                    0L,
                    PROBE_INTERVAL_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                try {
                    val runId = Base58.randomString(12)
                    val results = COMBINATIONS.map { (keyCount, concurrency) ->
                        runCombination(
                            runId,
                            keyCount,
                            concurrency,
                            workloadConnections,
                            workloadExecutor,
                            probeSamples,
                            probeCompletions,
                        )
                    }

                    val redisVersion = probeConnection.sync().info("server")
                        .lineSequence()
                        .firstOrNull { it.startsWith("redis_version:") }
                        ?.substringAfter(':')
                        ?.trim()
                        ?: "unknown"
                    CONCURRENCY_LEVELS.forEach { concurrency ->
                        val resultAt8 = results.single {
                            it.keyCount == 8 && it.concurrency == concurrency
                        }
                        val resultAt32 = results.single {
                            it.keyCount == 32 && it.concurrency == concurrency
                        }
                        resultAt32.acquireP95MillisPerKey shouldBeLessOrEqualTo
                            resultAt8.acquireP95MillisPerKey * 4.0
                        resultAt32.acquireP95Millis shouldBeLessOrEqualTo resultAt8.acquireP95Millis * 4.0
                    }
                    results.forEach { result ->
                        result.errors.shouldBeZero()
                        result.timeouts.shouldBeZero()
                        result.probeSampleCount shouldBeGreaterOrEqualTo MIN_PROBE_SAMPLES
                        result.probeP99Millis shouldBeLessThan COMMAND_TIMEOUT.toMillis().toDouble()
                    }
                    probeErrors.get().shouldBeZero()
                    passedResults = results
                    passedRedisVersion = redisVersion
                } finally {
                    probeTask.cancel(true)
                }
            } catch (failure: Throwable) {
                bodyFailure = failure
                throw failure
            } finally {
                val cleanupFailures = mutableListOf<Throwable>()
                fun attemptCleanup(block: () -> Unit) {
                    try {
                        block()
                    } catch (failure: Throwable) {
                        cleanupFailures += failure
                    }
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
                cleanupFailures.firstOrNull()?.let { first ->
                    cleanupFailures.drop(1).forEach(first::addSuppressed)
                    bodyFailure?.addSuppressed(first) ?: throw first
                }
            }
        }
        writeReport(checkNotNull(passedResults), checkNotNull(passedRedisVersion))
    }

    private fun runCombination(
        runId: String,
        keyCount: Int,
        concurrency: Int,
        connections: List<StatefulRedisConnection<String, String>>,
        executor: ThreadPoolExecutor,
        probeSamples: MutableList<Long>,
        probeCompletions: AtomicInteger,
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
            } catch (_: Throwable) {
                if (errors.get() == errorsBeforeRound) errors.incrementAndGet()
                return@repeat
            }
            acquireSamples += measurement.acquireNanos
            releaseSamples += measurement.releaseNanos
            acquiredCount += measurement.acquiredCount
            conflictedCount += measurement.conflictedCount
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
            acquireP95MillisPerKey = acquireSamples.percentileMillis(95.0) / keyCount,
            releaseP50Millis = releaseSamples.percentileMillis(50.0),
            releaseP95Millis = releaseSamples.percentileMillis(95.0),
            scenarioThroughputPerSecond = operationCount * NANOS_PER_SECOND.toDouble() / elapsedNanos,
            probeP95Millis = combinationProbeSamples.percentileMillis(95.0),
            probeP99Millis = combinationProbeSamples.percentileMillis(99.0),
            probeSampleCount = combinationProbeSamples.size,
            acquiredCount = acquiredCount,
            conflictedCount = conflictedCount,
            timeouts = timeouts.get(),
            errors = errors.get(),
        )
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
        val futures = leases.mapIndexed { index, lease ->
            executor.submit<AcquireAttempt> {
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
            }
        }
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
            return RoundMeasurement(
                acquireNanos = attempts.map { it.acquireNanos },
                releaseNanos = releaseNanos,
                acquiredCount = winners.size,
                conflictedCount = losers.size,
            )
        } finally {
            futures.forEach { future ->
                if (!future.isDone) future.cancel(true)
            }
            commands.del(*keys.toTypedArray())
        }
    }

    private fun writeReport(results: List<PerformanceResult>, redisVersion: String) {
        val report = buildString {
            appendLine("{")
            appendLine("  \"redisImage\": \"${RedisServer.IMAGE}:${RedisServer.TAG}\",")
            appendLine("  \"status\": \"passed\",")
            appendLine("  \"generatedAt\": \"${Instant.now()}\",")
            appendLine("  \"redisVersion\": \"$redisVersion\",")
            appendLine("  \"javaVersion\": \"${System.getProperty("java.version")}\",")
            appendLine("  \"kotlinVersion\": \"${KotlinVersion.CURRENT}\",")
            appendLine("  \"lettuceVersion\": \"${RedisClient::class.java.`package`.implementationVersion ?: "unknown"}\",")
            appendLine("  \"cpuCount\": ${Runtime.getRuntime().availableProcessors()},")
            appendLine("  \"warmUpRounds\": $WARM_UP_ROUNDS,")
            appendLine("  \"measuredRounds\": $MEASURED_ROUNDS,")
            appendLine("  \"metricDirection\": { \"latency\": \"lower is better\", \"throughput\": \"higher is better\" },")
            appendLine("  \"results\": [")
            results.forEachIndexed { index, result ->
                append("    ${result.toJson()}")
                appendLine(if (index == results.lastIndex) "" else ",")
            }
            appendLine("  ]")
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

    private fun PerformanceResult.toJson(): String = buildString {
        append("{")
        append("\"keyCount\":$keyCount,")
        append("\"concurrency\":$concurrency,")
        append("\"acquireP50Millis\":${acquireP50Millis.jsonNumber()},")
        append("\"acquireP95Millis\":${acquireP95Millis.jsonNumber()},")
        append("\"acquireP95MillisPerKey\":${acquireP95MillisPerKey.jsonNumber()},")
        append("\"releaseP50Millis\":${releaseP50Millis.jsonNumber()},")
        append("\"releaseP95Millis\":${releaseP95Millis.jsonNumber()},")
        append("\"scenarioThroughputPerSecond\":${scenarioThroughputPerSecond.jsonNumber()},")
        append("\"probeP95Millis\":${probeP95Millis.jsonNumber()},")
        append("\"probeP99Millis\":${probeP99Millis.jsonNumber()},")
        append("\"probeSampleCount\":$probeSampleCount,")
        append("\"acquiredCount\":$acquiredCount,")
        append("\"conflictedCount\":$conflictedCount,")
        append("\"timeouts\":$timeouts,")
        append("\"errors\":$errors")
        append("}")
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

    private data class PerformanceResult(
        val keyCount: Int,
        val concurrency: Int,
        val acquireP50Millis: Double,
        val acquireP95Millis: Double,
        val acquireP95MillisPerKey: Double,
        val releaseP50Millis: Double,
        val releaseP95Millis: Double,
        val scenarioThroughputPerSecond: Double,
        val probeP95Millis: Double,
        val probeP99Millis: Double,
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
        const val MAX_CONCURRENCY: Int = 16
        const val WARM_UP_ROUNDS: Int = 20
        const val MEASURED_ROUNDS: Int = 100
        const val PROBE_INTERVAL_MILLIS: Long = 10L
        const val MIN_PROBE_SAMPLES: Int = 10
        const val PROBE_COVERAGE_DIVISOR: Long = 2L
        const val ROUND_TIMEOUT_SECONDS: Long = 30L
        const val NANOS_PER_MILLISECOND: Long = 1_000_000L
        const val NANOS_PER_SECOND: Long = 1_000_000_000L
        const val PROBE_INTERVAL_NANOS: Long = PROBE_INTERVAL_MILLIS * NANOS_PER_MILLISECOND
    }
}
