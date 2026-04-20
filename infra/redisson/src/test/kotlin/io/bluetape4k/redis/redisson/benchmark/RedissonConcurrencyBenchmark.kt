package io.bluetape4k.redis.redisson.benchmark

import io.bluetape4k.codec.Base58
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.redis.redisson.RedissonTestUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.system.measureTimeMillis

/**
 * Redisson 동시 처리 벤치마크.
 * 결과는 .omc/self-improve-redisson/state/benchmark_last.json 에 기록됩니다.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class RedissonConcurrencyBenchmark {

    companion object: KLogging() {
        private const val CONCURRENCY = 50
        private const val OPS_PER_COROUTINE = 100
        private const val WARMUP_OPS = 500
        private const val WARMUP_PASSES = 3
        private const val MEASUREMENT_PASSES = 3
        private const val KEY_PREFIX = "benchmark:"

        private val OUTPUT_FILE: File by lazy {
            // user.dir = <repo-root>/infra/redisson — 2단계 상위가 프로젝트 루트
            val moduleDir = File(System.getProperty("user.dir") ?: ".")
            val repoRoot = moduleDir.parentFile?.parentFile ?: moduleDir
            File(repoRoot, ".omc/self-improve-redisson/state/benchmark_last.json")
                .also { it.parentFile?.mkdirs() }
        }
    }

    private val redisson = RedissonTestUtils.redissonClient

    @Test
    @Order(1)
    fun `warmup`() {
        // Round 7 H12: 단일 put 반복 warmup → 실제 벤치마크와 동일한 concurrent 워크로드 3회 실행
        repeat(WARMUP_PASSES) { pass ->
            val map = redisson.getMap<String, String>("$KEY_PREFIX:warmup:$pass:${Base58.randomString(4)}")
            val warmupOps = AtomicLong(0)
            runBlocking(Dispatchers.IO) {
                (1..CONCURRENCY).map { coroutineId ->
                    async {
                        repeat(OPS_PER_COROUTINE) { opIdx ->
                            runCatching {
                                val key = "w$coroutineId-op$opIdx"
                                map.put(key, "value-$key-${System.nanoTime()}")
                                map.get(key)
                                warmupOps.incrementAndGet()
                            }
                        }
                    }
                }.awaitAll()
            }
            runCatching { map.delete() }
            log.debug { "Warmup pass ${pass + 1}/$WARMUP_PASSES 완료: ${warmupOps.get()} ops" }
        }
        log.info { "Warmup 완료: ${WARMUP_PASSES}회 concurrent 실행" }
    }

    @Test
    @Order(2)
    fun measureConcurrentThroughput() {
        val passScores = mutableListOf<Long>()
        var lastTotalOps = 0L
        var lastErrorOps = 0L
        var lastElapsedMs = 0L

        repeat(MEASUREMENT_PASSES) { passIdx ->
            val map = redisson.getMap<String, String>(
                "$KEY_PREFIX:throughput:$passIdx:${Base58.randomString(6)}"
            )
            val successOps = AtomicLong(0)
            val errorOps = AtomicLong(0)

            val elapsedMs = measureTimeMillis {
                runBlocking(Dispatchers.IO) {
                    (1..CONCURRENCY).map { coroutineId ->
                        async {
                            repeat(OPS_PER_COROUTINE) { opIdx ->
                                runCatching {
                                    val key = "c$coroutineId-op$opIdx"
                                    map.put(key, "value-$key-${System.nanoTime()}")
                                    map.get(key)
                                    successOps.incrementAndGet()
                                }.onFailure {
                                    errorOps.incrementAndGet()
                                }
                            }
                        }
                    }.awaitAll()
                }
            }

            runCatching { map.delete() }

            val totalOps = successOps.get()
            val opsPerSec = if (elapsedMs > 0) totalOps * 1000L / elapsedMs else 0L
            passScores += opsPerSec
            lastTotalOps = totalOps
            lastErrorOps = errorOps.get()
            lastElapsedMs = elapsedMs

            log.info { "Pass ${passIdx + 1}/$MEASUREMENT_PASSES: totalOps=$totalOps, errors=${errorOps.get()}, elapsed=${elapsedMs}ms, ops/sec=$opsPerSec" }
        }

        // 중앙값(median)을 primary 메트릭으로 사용 — outlier 저항
        val sorted = passScores.sorted()
        val median = sorted[sorted.size / 2]

        // stddev 계산: sqrt(mean of squared deviations)
        val mean = passScores.average()
        val variance = passScores.map { (it - mean) * (it - mean) }.average()
        val stddev = kotlin.math.sqrt(variance)

        log.info { "동시 처리 벤치마크 [median]: passes=$passScores, median=$median, mean=${mean.toLong()}, stddev=${stddev.toLong()}" }

        median shouldBeGreaterThan 0L

        writeResults(
            concurrentOpsPerSec = median,
            totalOps = lastTotalOps,
            errorOps = lastErrorOps,
            elapsedMs = lastElapsedMs,
            concurrency = CONCURRENCY,
            opsPerCoroutine = OPS_PER_COROUTINE,
            passScores = passScores,
            stddevOpsPerSec = stddev
        )
    }

    @Test
    @Order(3)
    fun measureLeaderElectionThroughput() {
        val successCount = AtomicLong(0)
        val failCount = AtomicLong(0)
        val lockKey = "$KEY_PREFIX:leader:${Base58.randomString(6)}"
        val LOCK_ITERATIONS = 20

        val elapsedMs = measureTimeMillis {
            repeat(LOCK_ITERATIONS) { i ->
                runCatching {
                    val lock = redisson.getLock(lockKey)
                    val acquired = lock.tryLock(500, 2000, TimeUnit.MILLISECONDS)
                    if (acquired) {
                        try {
                            successCount.incrementAndGet()
                        } finally {
                            if (lock.isHeldByCurrentThread) {
                                lock.unlock()
                            }
                        }
                    } else {
                        failCount.incrementAndGet()
                    }
                }.onFailure {
                    failCount.incrementAndGet()
                }
            }
        }

        val leaderOpsPerSec = if (elapsedMs > 0) successCount.get() * 1000L / elapsedMs else 0L
        log.info { "LeaderElection 벤치마크: success=${successCount.get()}, fail=${failCount.get()}, elapsed=${elapsedMs}ms, ops/sec=$leaderOpsPerSec" }

        appendLeaderResults(
            leaderOpsPerSec = leaderOpsPerSec,
            leaderSuccessCount = successCount.get(),
            leaderFailCount = failCount.get(),
            leaderElapsedMs = elapsedMs
        )
    }

    private fun writeResults(
        concurrentOpsPerSec: Long,
        totalOps: Long,
        errorOps: Long,
        elapsedMs: Long,
        concurrency: Int,
        opsPerCoroutine: Int,
        passScores: List<Long>,
        stddevOpsPerSec: Double,
    ) {
        val outputFile = OUTPUT_FILE.canonicalFile
        outputFile.parentFile.mkdirs()

        val passScoresJson = passScores.joinToString(prefix = "[", postfix = "]") { it.toString() }
        val stddevRounded = (stddevOpsPerSec * 100).toLong() / 100.0

        val json = buildString {
            append("{\n")
            append("  \"primary\": $concurrentOpsPerSec,\n")
            append("  \"concurrent_ops_per_sec\": $concurrentOpsPerSec,\n")
            append("  \"total_ops\": $totalOps,\n")
            append("  \"error_ops\": $errorOps,\n")
            append("  \"elapsed_ms\": $elapsedMs,\n")
            append("  \"concurrency\": $concurrency,\n")
            append("  \"ops_per_coroutine\": $opsPerCoroutine,\n")
            append("  \"pass_scores\": $passScoresJson,\n")
            append("  \"stddev_ops_per_sec\": $stddevRounded,\n")
            append("  \"timestamp\": \"${java.time.Instant.now()}\"\n")
            append("}")
        }
        outputFile.writeText(json)
        log.info { "벤치마크 결과 기록: ${outputFile.absolutePath}" }
    }

    private fun appendLeaderResults(
        leaderOpsPerSec: Long,
        leaderSuccessCount: Long,
        leaderFailCount: Long,
        leaderElapsedMs: Long,
    ) {
        val outputFile = OUTPUT_FILE.canonicalFile
        if (!outputFile.exists()) return

        runCatching {
            val existing = outputFile.readText().trimEnd().trimEnd('}')
            val updated = existing +
                ",\n  \"leader_ops_per_sec\": $leaderOpsPerSec" +
                ",\n  \"leader_success_count\": $leaderSuccessCount" +
                ",\n  \"leader_fail_count\": $leaderFailCount" +
                ",\n  \"leader_elapsed_ms\": $leaderElapsedMs\n}"
            outputFile.writeText(updated)
        }
    }
}
