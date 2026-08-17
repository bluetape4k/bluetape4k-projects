package io.bluetape4k.cache.nearcache.benchmark

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.cache.nearcache.jcache.NearJCache
import io.bluetape4k.cache.nearcache.jcache.NearJCacheClearAuthority
import io.bluetape4k.cache.nearcache.jcache.NearJCacheConfig
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.TearDown
import org.openjdk.jmh.annotations.Threads
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

/**
 * `NearJCache` 통계 활성화 전후의 disabled hot path 비용을 비교합니다.
 *
 * 같은 committed harness를 baseline과 candidate에 사용하며, Caffeine JCache만 사용하므로
 * Docker나 Testcontainers가 필요하지 않습니다.
 */
@Threads(1)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(3)
open class NearJCacheStatisticsBenchmark {
    private lateinit var nearCache: NearJCache<String, String>
    private lateinit var backCache: JCache<String, String>
    private val keys = setOf("key-1", "key-2", "key-3", "key-4")
    private val entries = keys.associateWith { "value" }

    @Param("false")
    var statisticsEnabled: Boolean = false

    @Setup(Level.Trial)
    fun setup() {
        val frontConfig = jcacheConfiguration<String, String> {
            setTypes(String::class.java, String::class.java)
            setStoreByValue(false)
            setStatisticsEnabled(statisticsEnabled)
            setManagementEnabled(false)
        }
        val front = JCaching.Caffeine.getOrCreate("issue-1351-front", frontConfig)
        backCache = JCaching.Caffeine.getOrCreate("issue-1351-back", frontConfig)
        nearCache = NearJCache(
            frontCache = front,
            backCache = backCache,
            config = NearJCacheConfig(frontCacheConfiguration = frontConfig, isSynchronous = true),
            clearAuthority = NearJCacheClearAuthority.EXCLUSIVE_BACK_CACHE,
        )
        nearCache.putAll(entries)
    }

    @Benchmark
    fun frontHit(): String? = nearCache["key-1"]

    @Benchmark
    fun bulkHit(): Map<String, String> = nearCache.getAll(keys)

    @Benchmark
    fun put(): Unit = nearCache.put("put-key", "value")

    @Benchmark
    fun putAll(): Unit = nearCache.putAll(entries)

    @Benchmark
    fun getAndPut(): String? = nearCache.getAndPut("compound-key", "value")

    @TearDown(Level.Trial)
    fun tearDown() {
        nearCache.clear()
        nearCache.close()
        backCache.close()
    }
}
