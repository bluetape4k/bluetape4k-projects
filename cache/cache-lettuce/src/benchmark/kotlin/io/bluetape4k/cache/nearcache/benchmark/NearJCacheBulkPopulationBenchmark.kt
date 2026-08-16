package io.bluetape4k.cache.nearcache.benchmark

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.cache.nearcache.jcache.BulkFrontPopulationPolicy
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.infra.ThreadParams
import java.util.UUID
import java.util.concurrent.TimeUnit

/** 단일 thread path benchmark의 bulk cache 상태를 구분합니다. */
enum class BulkScenario {
    FRONT_HIT_BYPASS,
    FRONT_HIT_BOUNDED,
    BACK_MISS_BYPASS,
    BACK_MISS_BOUNDED,
    BACK_HIT_BYPASS,
    BACK_HIT_BOUNDED,
    BACK_HIT_OVERSIZED,
}

/** contention benchmark에서 비교할 front 저장 정책입니다. */
enum class BulkContentionPolicy {
    BYPASS_FRONT,
    POPULATE_IF_AT_MOST,
}

/**
 * `getAll`의 front hit, back miss, bounded back hit와 초과 batch 경로를 측정합니다.
 *
 * 각 invocation 전에 해당 key만 front에서 재설정하므로 이전 호출의 population이 다음 측정을
 * front hit로 바꾸지 않습니다.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Threads(1)
open class NearJCacheBulkPathBenchmark {
    lateinit var frontCache: JCache<String, String>
    lateinit var backCache: JCache<String, String>
    lateinit var nearCache: NearJCache<String, String>
    lateinit var keys: Set<String>
    lateinit var entries: Map<String, String>

    @Param(
        "FRONT_HIT_BYPASS",
        "FRONT_HIT_BOUNDED",
        "BACK_MISS_BYPASS",
        "BACK_MISS_BOUNDED",
        "BACK_HIT_BYPASS",
        "BACK_HIT_BOUNDED",
        "BACK_HIT_OVERSIZED",
    )
    lateinit var scenario: BulkScenario

    @Param("1", "4", "128")
    var batchSize: Int = 1

    @Setup(Level.Trial)
    fun setup() {
        val returnedEntryCount = if (scenario == BulkScenario.BACK_HIT_OVERSIZED) {
            batchSize + 1
        } else {
            batchSize
        }
        keys = (0 until returnedEntryCount).mapTo(linkedSetOf()) { "key-$it" }
        entries = keys.associateWith { "value-$it" }
        val configuration = benchmarkConfiguration()
        frontCache = JCaching.Caffeine.getOrCreate(
            "issue-1369-front-${UUID.randomUUID()}",
            configuration,
        )
        backCache = JCaching.Caffeine.getOrCreate(
            "issue-1369-back-${UUID.randomUUID()}",
            configuration,
        )
        if (scenario != BulkScenario.BACK_MISS_BYPASS && scenario != BulkScenario.BACK_MISS_BOUNDED) {
            backCache.putAll(entries)
        }
        nearCache = NearJCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                frontCacheConfiguration = configuration,
                isSynchronous = true,
                bulkFrontPopulationPolicy = when (scenario) {
                    BulkScenario.FRONT_HIT_BOUNDED,
                    BulkScenario.BACK_MISS_BOUNDED,
                    BulkScenario.BACK_HIT_BOUNDED,
                    BulkScenario.BACK_HIT_OVERSIZED,
                    -> BulkFrontPopulationPolicy.PopulateIfAtMost(batchSize)

                    else -> BulkFrontPopulationPolicy.BypassFront
                },
            ),
        )
        resetFront()
        val expectedSize = when (scenario) {
            BulkScenario.BACK_MISS_BYPASS,
            BulkScenario.BACK_MISS_BOUNDED,
            -> 0

            else -> entries.size
        }
        check(nearCache.getAll(keys).size == expectedSize)
        resetFront()
    }

    @Setup(Level.Invocation)
    fun resetFront() {
        frontCache.removeAll(keys)
        if (scenario.name.startsWith("FRONT_HIT")) {
            frontCache.putAll(entries)
        }
    }

    @Benchmark
    fun getAll(): Map<String, String> = nearCache.getAll(keys)

    @TearDown(Level.Trial)
    fun tearDown() {
        frontCache.clear()
        backCache.clear()
        nearCache.close()
        backCache.close()
    }
}

/**
 * shared `NearJCache`의 `mutationGate`에서 bulk population 정책별 contention을 측정합니다.
 *
 * thread별 key partition을 분리해 cache key 충돌 없이 동일 gate의 경쟁만 포함합니다.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class NearJCacheBulkContentionBenchmark {
    lateinit var frontCache: JCache<String, String>
    lateinit var backCache: JCache<String, String>
    lateinit var nearCache: NearJCache<String, String>

    @Param("BYPASS_FRONT", "POPULATE_IF_AT_MOST")
    lateinit var policy: BulkContentionPolicy

    @Setup(Level.Trial)
    fun setup() {
        val configuration = benchmarkConfiguration()
        frontCache = JCaching.Caffeine.getOrCreate(
            "issue-1369-contention-front-${UUID.randomUUID()}",
            configuration,
        )
        backCache = JCaching.Caffeine.getOrCreate(
            "issue-1369-contention-back-${UUID.randomUUID()}",
            configuration,
        )
        nearCache = NearJCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                frontCacheConfiguration = configuration,
                isSynchronous = true,
                bulkFrontPopulationPolicy = when (policy) {
                    BulkContentionPolicy.BYPASS_FRONT -> BulkFrontPopulationPolicy.BypassFront
                    BulkContentionPolicy.POPULATE_IF_AT_MOST ->
                        BulkFrontPopulationPolicy.PopulateIfAtMost(CONTENTION_BATCH_SIZE)
                },
            ),
        )
    }

    @Benchmark
    fun getAll(threadState: NearJCacheBulkThreadState): Map<String, String> =
        nearCache.getAll(threadState.keys)

    @TearDown(Level.Trial)
    fun tearDown() {
        frontCache.clear()
        backCache.clear()
        nearCache.close()
        backCache.close()
    }
}

/** thread index별 bulk key partition과 invocation reset을 소유합니다. */
@State(Scope.Thread)
open class NearJCacheBulkThreadState {
    lateinit var keys: Set<String>

    @Setup(Level.Trial)
    fun setup(benchmark: NearJCacheBulkContentionBenchmark, params: ThreadParams) {
        keys = (0 until CONTENTION_BATCH_SIZE).mapTo(linkedSetOf()) {
            "thread-${params.threadIndex}-key-$it"
        }
        benchmark.backCache.putAll(keys.associateWith { "value-$it" })
    }

    @Setup(Level.Invocation)
    fun resetFront(benchmark: NearJCacheBulkContentionBenchmark) {
        benchmark.frontCache.removeAll(keys)
    }
}

private const val CONTENTION_BATCH_SIZE = 128

private fun benchmarkConfiguration() = jcacheConfiguration<String, String> {
    setTypes(String::class.java, String::class.java)
    setStoreByValue(false)
    setStatisticsEnabled(false)
    setManagementEnabled(false)
}
