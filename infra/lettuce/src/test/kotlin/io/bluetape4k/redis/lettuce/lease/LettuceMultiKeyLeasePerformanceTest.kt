package io.bluetape4k.redis.lettuce.lease

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.LettuceClients
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
import java.time.Duration
import java.util.Collections
import java.util.Locale
import java.util.UUID
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
 * Characterizes Redis-side multi-key lease cost outside the default test task.
 *
 * Re-run `:bluetape4k-lettuce:multiKeyLeasePerformanceTest` after changing the Lua scripts or the default `maxKeys`.
 * Absolute latency is environment-dependent; the regression assertion compares normalized p95 values within one run.
 */
@Tag("performance")
internal class LettuceMultiKeyLeasePerformanceTest {

    @Test
    fun `characterize lease latency throughput and connection responsiveness`() = runSuspendIO {
        RedisServer().use { server ->
            server.start()
            val client = LettuceClients.clientOf(server.host, server.port)
            val workloadConnections = mutableListOf<StatefulRedisConnection<String, String>>()
            var probeConnection: StatefulRedisConnection<String, String>? = null
            val workloadExecutor = Executors.newFixedThreadPool(MAX_CONCURRENCY) as ThreadPoolExecutor
            val probeExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
            try {
                repeat(MAX_CONCURRENCY) {
                    workloadConnections += client.connect(StringCodec.UTF8)
                }
                probeConnection = client.connect(StringCodec.UTF8)
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
                    PROBE_INTERVAL_MILLIS,
                    TimeUnit.MILLISECONDS,
                )
                try {
                    val runId = UUID.randomUUID().toString()
                    val results = KEY_COUNTS.flatMap { keyCount ->
                        CONCURRENCY_LEVELS.map { concurrency ->
                            runCombination(
                                runId,
                                keyCount,
                                concurrency,
                                workloadConnections,
                                workloadExecutor,
                                probeSamples,
                            )
                        }
                    }

                    CONCURRENCY_LEVELS.forEach { concurrency ->
                        val p95At8 = results.single { it.keyCount == 8 && it.concurrency == concurrency }.acquireP95Millis
                        val p95At32 = results.single { it.keyCount == 32 && it.concurrency == concurrency }.acquireP95Millis
                        p95At32 shouldBeLessOrEqualTo p95At8 * 4.0
                    }
                    results.forEach { result ->
                        result.errors shouldBeEqualTo 0
                        result.timeouts shouldBeEqualTo 0
                        result.probeP99Millis shouldBeLessThan CONNECTION_TIMEOUT.toMillis().toDouble()
                    }
                    probeErrors.get() shouldBeEqualTo 0

                    val redisVersion = probeConnection.sync().info("server")
                        .lineSequence()
                        .firstOrNull { it.startsWith("redis_version:") }
                        ?.substringAfter(':')
                        ?.trim()
                        ?: "unknown"
                    writeReport(results, redisVersion)
                } finally {
                    probeTask.cancel(true)
                }
            } finally {
                probeExecutor.shutdownNow()
                probeExecutor.awaitTermination(10, TimeUnit.SECONDS)
                workloadExecutor.shutdown()
                if (!workloadExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    workloadExecutor.shutdownNow()
                    workloadExecutor.awaitTermination(10, TimeUnit.SECONDS)
                }
                workloadExecutor.activeCount shouldBeEqualTo 0
                probeConnection?.close()
                workloadConnections.asReversed().forEach { it.close() }
                client.shutdown()
            }
        }
    }

    private fun runCombination(
        runId: String,
        keyCount: Int,
        concurrency: Int,
        connections: List<StatefulRedisConnection<String, String>>,
        executor: ThreadPoolExecutor,
        probeSamples: MutableList<Long>,
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
        val startedAt = System.nanoTime()
        repeat(MEASURED_ROUNDS) { round ->
            val measurement = runRound(
                keys,
                leases,
                commands,
                executor,
                WARM_UP_ROUNDS + round,
                timeouts,
                errors,
            )
            acquireSamples += measurement.acquireNanos
            releaseSamples += measurement.releaseNanos
        }
        val elapsedNanos = System.nanoTime() - startedAt

        await()
            .atMost(Duration.ofSeconds(5))
            .until { executor.activeCount == 0 }
        commands.exists(*keys.toTypedArray()) shouldBeEqualTo 0L
        val combinationProbeSamples = synchronized(probeSamples) {
            probeSamples.drop(probeStart)
        }
        val operationCount = acquireSamples.size + releaseSamples.size
        return PerformanceResult(
            keyCount = keyCount,
            concurrency = concurrency,
            acquireP50Millis = acquireSamples.percentileMillis(50.0),
            acquireP95Millis = acquireSamples.percentileMillis(95.0),
            releaseP50Millis = releaseSamples.percentileMillis(50.0),
            releaseP95Millis = releaseSamples.percentileMillis(95.0),
            throughputPerSecond = operationCount * NANOS_PER_SECOND.toDouble() / elapsedNanos,
            probeP95Millis = combinationProbeSamples.percentileMillis(95.0),
            probeP99Millis = combinationProbeSamples.percentileMillis(99.0),
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
                val token = "owner-$round-$index-${UUID.randomUUID()}"
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
            winners.size shouldBeEqualTo 1
            losers.size shouldBeEqualTo leases.size - 1
            attempts.size shouldBeEqualTo winners.size + losers.size

            val winner = winners.single()
            val releaseStartedAt = System.nanoTime()
            leases[winner.index].release(keys, winner.token) shouldBeEqualTo MultiKeyReleaseResult.Released
            val releaseNanos = System.nanoTime() - releaseStartedAt
            commands.exists(*keys.toTypedArray()) shouldBeEqualTo 0L
            return RoundMeasurement(attempts.map { it.acquireNanos }, releaseNanos)
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
        Files.writeString(reportPath, report)
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
        require(isNotEmpty()) { "Performance samples must not be empty." }
        val sorted = sorted()
        val index = (ceil(percentile / 100.0 * sorted.size).toInt() - 1).coerceIn(sorted.indices)
        return sorted[index] / NANOS_PER_MILLISECOND.toDouble()
    }

    private fun PerformanceResult.toJson(): String = buildString {
        append("{")
        append("\"keyCount\":$keyCount,")
        append("\"concurrency\":$concurrency,")
        append("\"acquireP50Millis\":${acquireP50Millis.jsonNumber()},")
        append("\"acquireP95Millis\":${acquireP95Millis.jsonNumber()},")
        append("\"releaseP50Millis\":${releaseP50Millis.jsonNumber()},")
        append("\"releaseP95Millis\":${releaseP95Millis.jsonNumber()},")
        append("\"throughputPerSecond\":${throughputPerSecond.jsonNumber()},")
        append("\"probeP95Millis\":${probeP95Millis.jsonNumber()},")
        append("\"probeP99Millis\":${probeP99Millis.jsonNumber()},")
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
    )

    private data class PerformanceResult(
        val keyCount: Int,
        val concurrency: Int,
        val acquireP50Millis: Double,
        val acquireP95Millis: Double,
        val releaseP50Millis: Double,
        val releaseP95Millis: Double,
        val throughputPerSecond: Double,
        val probeP95Millis: Double,
        val probeP99Millis: Double,
        val timeouts: Int,
        val errors: Int,
    )

    private companion object {
        val KEY_COUNTS: List<Int> = listOf(1, 8, 32)
        val CONCURRENCY_LEVELS: List<Int> = listOf(1, 16)
        val LEASE_TIME: Duration = Duration.ofSeconds(10)
        val CONNECTION_TIMEOUT: Duration = Duration.ofMillis(
            System.getProperty("bluetape4k.lettuce.connectTimeoutMs", "5000").toLong(),
        )
        const val MAX_CONCURRENCY: Int = 16
        const val WARM_UP_ROUNDS: Int = 20
        const val MEASURED_ROUNDS: Int = 100
        const val PROBE_INTERVAL_MILLIS: Long = 10L
        const val ROUND_TIMEOUT_SECONDS: Long = 30L
        const val NANOS_PER_MILLISECOND: Long = 1_000_000L
        const val NANOS_PER_SECOND: Long = 1_000_000_000L
    }
}
