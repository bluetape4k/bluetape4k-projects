package io.bluetape4k.redis.lettuce.benchmark

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.future.await
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Lettuce async throughput 벤치마크.
 *
 * self-improve-lettuce 루프의 기준 지표로 사용됩니다.
 * 결과는 `.omc/self-improve-lettuce/state/benchmark_last.json`에 기록됩니다.
 */
@Tag("benchmark")
class LettuceThroughputBenchmark : AbstractLettuceTest() {

    companion object : KLogging() {
        private const val OPS_COUNT = 5_000
        private const val VALUE_SIZE = 64

        private val RESULT_FILE: File by lazy {
            val repoRoot = System.getProperty("user.dir") ?: "."
            File("$repoRoot/.omc/self-improve-lettuce/state/benchmark_last.json")
                .also { it.parentFile?.mkdirs() }
        }
    }

    private val asyncCommands by lazy { LettuceClients.asyncCommands(client) }

    @Test
    fun measureAsyncThroughput() = runSuspendIO {
        val keyPrefix = "bench:async:${System.currentTimeMillis()}:"
        val value = "v".repeat(VALUE_SIZE)

        // Warmup
        repeat(200) { i -> asyncCommands.set("${keyPrefix}warm:$i", value).await() }

        val start = System.currentTimeMillis()

        // Async SET — 모두 동시에 발사
        (0 until OPS_COUNT).map { i ->
            async { asyncCommands.set("$keyPrefix$i", value).await() }
        }.awaitAll()

        // Async GET — 모두 동시에 발사
        (0 until OPS_COUNT).map { i ->
            async { asyncCommands.get("$keyPrefix$i").await() }
        }.awaitAll()

        val elapsedMs = System.currentTimeMillis() - start
        val opsPerSec = if (elapsedMs > 0) (OPS_COUNT * 2 * 1000L / elapsedMs) else 0L

        // Cleanup
        (0 until OPS_COUNT).map { i ->
            async { asyncCommands.del("$keyPrefix$i").await() }
        }.awaitAll()
        repeat(200) { i -> asyncCommands.del("${keyPrefix}warm:$i").await() }

        val result = mapOf(
            "throughput_ops_per_sec" to opsPerSec,
            "mode" to "async",
            "ops_count" to (OPS_COUNT * 2),
            "elapsed_ms" to elapsedMs,
            "value_size_bytes" to VALUE_SIZE
        )

        val json = buildString {
            append("{")
            result.entries.forEachIndexed { idx, (k, v) ->
                if (idx > 0) append(", ")
                append("\"$k\": ")
                if (v is String) append("\"$v\"") else append(v)
            }
            append("}")
        }

        RESULT_FILE.writeText(json)
        log.info("Benchmark result: $json")
    }
}
