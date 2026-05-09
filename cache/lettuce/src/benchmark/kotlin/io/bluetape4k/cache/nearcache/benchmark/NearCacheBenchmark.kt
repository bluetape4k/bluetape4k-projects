package io.bluetape4k.cache.nearcache.benchmark

import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodecs
import io.bluetape4k.testcontainers.storage.RedisServer
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.StringCodec
import io.lettuce.core.protocol.ProtocolVersion
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
import java.util.concurrent.atomic.AtomicLong

/**
 * LettuceNearCache (L1=Caffeine, L2=Redis RESP3) 처리량 벤치마크.
 *
 * ## 사전조건
 * Docker 데몬이 실행 중이어야 한다 (Testcontainers Redis 7+).
 *
 * ## 측정 시나리오
 * - **l1Hit**: L1(Caffeine) 적중 — Redis 왕복 없음
 * - **l2Hit**: L1 비운 후 get → Redis 왕복 + L1 재충전 (clearLocal 비용 포함)
 * - **l2Miss**: 미존재 키 get → Redis 왕복 후 null 반환
 * - **putSingle**: write-through put 1건 (L1 + L2)
 * - **putAll**: batchSize 건 묶음 put
 *
 * ## remove 벤치마크
 * `removeSingle`은 @Setup(Level.Invocation) 격리를 위해 [NearCacheRemoveBenchmark] 별도 클래스에서 측정.
 *
 * ## 참고
 * - l2Hit 수치에는 `clearLocal()` 비용이 포함됨 (Benchmark.md Analysis 섹션에 명시)
 * - `putSingle`/`putAll` 에는 CLIENT TRACKING 등록용 RESP3 GET 왕복이 포함됨
 *
 * ## 실행
 * ```bash
 * ./gradlew :bluetape4k-cache-lettuce:benchmark
 * ```
 */
@Threads(1)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class NearCacheBenchmark {

    companion object : KLogging()

    /** putAll 묶음 크기 */
    @Param("100")
    var batchSize: Int = 100

    /** 페이로드 크기(bytes) — L2 Redis 왕복 비용 가시화 */
    @Param("512", "4096", "16384")
    var payloadSize: Int = 512

    private lateinit var redisClient: RedisClient
    private lateinit var cache: LettuceNearCache<String>

    private val counter = AtomicLong()
    private lateinit var warmValue: String

    /** l1Hit 전용 — Trial 동안 L1+L2 에 안정적으로 존재하는 키 */
    private val warmKey = "bench-warm"

    /** l2Hit 전용 — Redis 에만 존재 (Trial 셋업 후 L1 에서 제거됨) */
    private val l2WarmKey = "bench-l2warm"

    @Setup(Level.Trial)
    fun setupTrial() {
        warmValue = "A".repeat(payloadSize)

        val server = RedisServer.Launcher.redis
        redisClient = RedisClient.create(
            RedisServer.Launcher.LettuceLib.getRedisURI(server.host, server.port)
        ).also { client ->
            client.options = ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3)
                .build()
            ShutdownQueue.register { client.shutdown() }
        }

        cache = LettuceNearCache(
            redisClient = redisClient,
            codec = LettuceBinaryCodecs.default(),
            config = LettuceNearCacheConfig(
                cacheName = "bench-near",
                maxLocalSize = 100_000L,
                recordStats = true,
            ),
        )

        // l1Hit warmKey: L1 + L2 에 사전 적재
        cache.put(warmKey, warmValue)
        cache.get(warmKey)   // L1 채우기 보장

        // l2WarmKey: L2(Redis) 에만 존재하도록 put 후 clearLocal
        cache.put(l2WarmKey, warmValue)
        cache.clearLocal()   // L1 전체 비움 → l2WarmKey 는 Redis 에만 남음
        // warmKey 를 L1 에 다시 채움
        cache.put(warmKey, warmValue)
        cache.get(warmKey)
    }

    @Setup(Level.Iteration)
    fun warmupIteration() {
        // warmKey 가 혹시 L1 에서 빠졌을 경우 재충전 (안전망)
        if (cache.get(warmKey) == null) {
            cache.put(warmKey, warmValue)
        }
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        if (::cache.isInitialized) {
            runCatching { cache.clearAll() }
            runCatching { cache.close() }
        }
        runCatching { redisClient.shutdown() }
    }

    // -------------------------------------------------------------------------
    // Benchmark methods
    // -------------------------------------------------------------------------

    /**
     * L1(Caffeine) 적중 — Redis 왕복 없이 순수 메모리 조회.
     */
    @Benchmark
    fun l1Hit(): String? = cache.get(warmKey)

    /**
     * L1 비운 후 Redis 적중 — Redis 왕복 + L1 재충전.
     *
     * 주의: `clearLocal()` 비용이 측정값에 포함됨.
     */
    @Benchmark
    fun l2Hit(): String? {
        cache.clearLocal()
        return cache.get(l2WarmKey)
    }

    /**
     * 양쪽 캐시 모두 miss — Redis GET(null 반환) 비용 측정.
     */
    @Benchmark
    fun l2Miss(): String? = cache.get("miss-${counter.incrementAndGet()}")

    /**
     * write-through put 단건 — L1 + L2(Redis SET + CLIENT TRACKING GET).
     */
    @Benchmark
    fun putSingle() {
        cache.put("put-${counter.incrementAndGet()}", warmValue)
    }

    /**
     * 묶음 put — batchSize 건을 한 번에 put.
     */
    @Benchmark
    fun putAll() {
        val n = counter.addAndGet(batchSize.toLong())
        val batch = (0 until batchSize).associate { i -> "pb-${n - i}" to warmValue }
        cache.putAll(batch)
    }
}

/**
 * NearCache `remove` 처리량 벤치마크.
 *
 * `removeSingle` 측정만 포함 — @Setup(Level.Invocation) 으로 pre-put 하여
 * remove 경로만 순수 측정한다. 별도 클래스로 분리하여 다른 벤치마크 오염을 방지.
 */
@Threads(1)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@Fork(1)
open class NearCacheRemoveBenchmark {

    companion object : KLogging()

    @Param("512", "4096", "16384")
    var payloadSize: Int = 512

    private lateinit var redisClient: RedisClient
    private lateinit var cache: LettuceNearCache<String>

    private val counter = AtomicLong()
    private lateinit var warmValue: String
    private lateinit var currentRemoveKey: String

    @Setup(Level.Trial)
    fun setupTrial() {
        warmValue = "A".repeat(payloadSize)

        val server = RedisServer.Launcher.redis
        redisClient = RedisClient.create(
            RedisServer.Launcher.LettuceLib.getRedisURI(server.host, server.port)
        ).also { client ->
            client.options = ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3)
                .build()
            ShutdownQueue.register { client.shutdown() }
        }

        cache = LettuceNearCache(
            redisClient = redisClient,
            codec = LettuceBinaryCodecs.default(),
            config = LettuceNearCacheConfig(
                cacheName = "bench-remove",
                maxLocalSize = 100_000L,
                recordStats = true,
            ),
        )
    }

    /** 매 invocation 전 새 키를 put — remove 만 측정하기 위한 사전 준비. JMH 측정에서 제외됨. */
    @Setup(Level.Invocation)
    fun prepareRemoveKey() {
        currentRemoveKey = "rm-${counter.incrementAndGet()}"
        cache.put(currentRemoveKey, warmValue)
    }

    @TearDown(Level.Trial)
    fun tearDown() {
        if (::cache.isInitialized) {
            runCatching { cache.clearAll() }
            runCatching { cache.close() }
        }
        runCatching { redisClient.shutdown() }
    }

    /**
     * remove 단건 — L1 + L2(Redis DEL) 경로 처리량.
     * `@Setup(Level.Invocation)` 의 pre-put 비용을 JMH 가 타임스탬프 차감으로 제외를 시도하지만,
     * sub-millisecond 연산에서는 타임스탬프 획득 비용 자체가 측정에 영향을 줄 수 있다.
     */
    @Benchmark
    fun removeSingle() = cache.remove(currentRemoveKey)
}
