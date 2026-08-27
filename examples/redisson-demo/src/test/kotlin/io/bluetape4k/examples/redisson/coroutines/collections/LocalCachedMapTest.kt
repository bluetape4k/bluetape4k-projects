package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import kotlinx.coroutines.withTimeout
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.client.RedisException
import org.redisson.codec.CompositeCodec
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val intCodec = CompositeCodec(
    RedissonCodecs.String,
    RedissonCodecs.Int,
    RedissonCodecs.Int,
)

private val doubleCodec = CompositeCodec(
    RedissonCodecs.String,
    RedissonCodecs.Double,
    RedissonCodecs.Double,
)

/**
 * [RLocalCachedMap] 예제
 *
 * 참고: [Local Cache](https://github.com/redisson/redisson/wiki/7.-distributed-collections#local-cache)
 */
class LocalCachedMapTest: AbstractRedissonCoroutineTest() {

    companion object: KLoggingChannel()

    private lateinit var redisson1: RedissonClient
    private lateinit var redisson2: RedissonClient

    private val cacheName = randomName()

    private val options1 = LocalCachedMapOptions.name<String, Int>(cacheName)
        .cacheSize(100)
        .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
        .maxIdle(10.seconds.toJavaDuration())
        .timeToLive(5.seconds.toJavaDuration())
        .codec(intCodec)

    private val options2 = LocalCachedMapOptions.name<String, Int>(cacheName)
        .cacheSize(100)
        .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
        .maxIdle(10.seconds.toJavaDuration())
        .timeToLive(5.seconds.toJavaDuration())
        .codec(intCodec)

    private val frontCache1: RLocalCachedMap<String, Int> by lazy { redisson1.getLocalCachedMap(options1) }
    private val frontCache2: RLocalCachedMap<String, Int> by lazy { redisson2.getLocalCachedMap(options2) }
    private val backCache: RMap<String, Int> by lazy { redisson.getMap(cacheName, intCodec) }

    @BeforeAll
    fun setup() {
        redisson1 = newRedisson(registerShutdown = false)
        redisson2 = newRedisson(registerShutdown = false)
    }

    @AfterAll
    fun cleanup() {
        var firstFailure: Throwable? = null

        if (this::redisson1.isInitialized) {
            runCatching { redisson1.shutdown(0, 5, TimeUnit.SECONDS) }
                .onFailure { firstFailure = it }
        }
        if (this::redisson2.isInitialized) {
            runCatching { redisson2.shutdown(0, 5, TimeUnit.SECONDS) }
                .onFailure { failure ->
                    if (firstFailure == null) {
                        firstFailure = failure
                    } else {
                        checkNotNull(firstFailure).addSuppressed(failure)
                    }
                }
        }

        firstFailure?.let { throw it }
    }

    @Test
    fun `frontCache1 에 cache item을 추가하면 frontCache2에 추가됩니다`() = runSuspendIO(timeout = 60.seconds) {
        val keyToAdd = randomName()

        log.debug { "front cache1: put key=$keyToAdd" }
        awaitRedis(frontCache1.fastPutAsync(keyToAdd, 42)).shouldBeTrue()
        await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)) untilSuspending {
            awaitRedis(backCache.containsKeyAsync(keyToAdd))
        }

        log.debug { "front cache2: get key=$keyToAdd" }
        awaitRedis(frontCache2.getAsync(keyToAdd)) shouldBeEqualTo 42
    }

    @Test
    fun `frontCache1의 cache item을 삭제하면 frontCache2에서도 삭제됩니다`() = runSuspendIO(timeout = 60.seconds) {
        val keyToRemove = randomName()

        log.debug { "front cache1: put $keyToRemove" }
        awaitRedis(frontCache1.fastPutAsync(keyToRemove, 42)).shouldBeTrue()
        await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)) untilSuspending {
            awaitRedis(backCache.containsKeyAsync(keyToRemove))
        }
        awaitRedis(frontCache2.getAsync(keyToRemove)) shouldBeEqualTo 42

        log.debug { "front cache1: remove $keyToRemove" }
        awaitRedis(frontCache1.fastRemoveAsync(keyToRemove)) shouldBeEqualTo 1L
        await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)) untilSuspending {
            !awaitRedis(backCache.containsKeyAsync(keyToRemove))
        }
        awaitRedis(frontCache2.getAsync(keyToRemove)).shouldBeNull()
    }

    @Test
    fun `backCache에 cache item을 추가하면 frontCache 에 반영된다`() = runSuspendIO(timeout = 60.seconds) {
        val key = randomName()

        awaitRedis(frontCache1.containsKeyAsync(key)).shouldBeFalse()
        awaitRedis(frontCache2.containsKeyAsync(key)).shouldBeFalse()

        awaitRedis(backCache.fastPutAsync(key, 42)).shouldBeTrue()

        await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)) untilSuspending {
            awaitRedis(frontCache1.containsKeyAsync(key)) &&
                awaitRedis(frontCache2.containsKeyAsync(key))
        }

        awaitRedis(frontCache1.containsKeyAsync(key)).shouldBeTrue()
        awaitRedis(frontCache2.containsKeyAsync(key)).shouldBeTrue()

        awaitRedis(backCache.fastRemoveAsync(key)) shouldBeEqualTo 1L

        await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)) untilSuspending {
            !awaitRedis(frontCache1.containsKeyAsync(key)) &&
                !awaitRedis(frontCache2.containsKeyAsync(key))
        }

        awaitRedis(frontCache1.containsKeyAsync(key)).shouldBeFalse()
        awaitRedis(frontCache2.containsKeyAsync(key)).shouldBeFalse()
    }

    @Test
    fun `frontCache1 remote update invalidates both cached values`() = runSuspendIO(timeout = 60.seconds) {
        val key = randomName()

        awaitRedis(frontCache1.fastPutAsync(key, 7)).shouldBeTrue()
        await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)) untilSuspending {
            awaitRedis(frontCache1.getAsync(key)) == 7 &&
                awaitRedis(frontCache2.getAsync(key)) == 7
        }

        awaitRedis(frontCache1.fastPutAsync(key, 42)).shouldBeFalse()
        await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)) untilSuspending {
            awaitRedis(frontCache1.getAsync(key)) == 42 &&
                awaitRedis(frontCache2.getAsync(key)) == 42
        }

        awaitRedis(frontCache1.fastRemoveAsync(key)) shouldBeEqualTo 1L
    }

    @Test
    fun `concurrent Int increments match independent remote final value`() = runSuspendIO(timeout = 60.seconds) {
        val name = randomName()
        val calls = 32 * 8
        val completed = AtomicInteger()
        val map1 = redisson1.getLocalCachedMap(
            LocalCachedMapOptions.name<String, Int>(name).codec(intCodec)
        )
        val map2 = redisson2.getLocalCachedMap(
            LocalCachedMapOptions.name<String, Int>(name).codec(intCodec)
        )
        val remote = redisson.getMap<String, Int>(name, intCodec)
        awaitRedis(remote.fastPutAsync("count", 0)).shouldBeTrue()
        awaitRedis(map1.getAsync("count")) shouldBeEqualTo 0
        awaitRedis(map2.getAsync("count")) shouldBeEqualTo 0

        withLocalCacheClearBarrier(map1, map2, "count") {
            withTimeout(30.seconds) {
                SuspendedJobTester()
                    .workers(4)
                    .rounds(calls)
                    .add {
                        awaitRedis(map1.addAndGetAsync("count", 1), timeout = 30.seconds)
                        completed.incrementAndGet()
                    }
                    .run()
            }

            completed.get() shouldBeEqualTo calls
            awaitRedis(remote.getAsync("count")) shouldBeEqualTo calls
        }
        await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)) untilSuspending {
            awaitRedis(map1.getAsync("count")) == calls &&
                awaitRedis(map2.getAsync("count")) == calls
        }
        awaitRedis(map1.getAsync("count")) shouldBeEqualTo calls
        awaitRedis(map2.getAsync("count")) shouldBeEqualTo calls
    }

    @Test
    fun `concurrent Double increments match independent remote final value`() = runSuspendIO(timeout = 60.seconds) {
        val name = randomName()
        val calls = 32 * 8
        val expected = calls * 0.25
        val completed = AtomicInteger()
        val map1 = redisson1.getLocalCachedMap(
            LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)
        )
        val map2 = redisson2.getLocalCachedMap(
            LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)
        )
        val remote = redisson.getMap<String, Double>(name, doubleCodec)
        awaitRedis(remote.fastPutAsync("ratio", 0.0)).shouldBeTrue()
        awaitRedis(map1.getAsync("ratio")) shouldBeEqualTo 0.0
        awaitRedis(map2.getAsync("ratio")) shouldBeEqualTo 0.0

        withLocalCacheClearBarrier(map1, map2, "ratio") {
            withTimeout(30.seconds) {
                SuspendedJobTester()
                    .workers(4)
                    .rounds(calls)
                    .add {
                        awaitRedis(map1.addAndGetAsync("ratio", 0.25), timeout = 30.seconds)
                        completed.incrementAndGet()
                    }
                    .run()
            }

            completed.get() shouldBeEqualTo calls
            awaitRedis(remote.getAsync("ratio")) shouldBeEqualTo expected
        }
        await.atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(100)) untilSuspending {
            awaitRedis(map1.getAsync("ratio")) == expected &&
                awaitRedis(map2.getAsync("ratio")) == expected
        }
        awaitRedis(map1.getAsync("ratio")) shouldBeEqualTo expected
        awaitRedis(map2.getAsync("ratio")) shouldBeEqualTo expected
    }

    @Test
    fun `non numeric stored value is rejected by numeric increment`() = runSuspendIO(timeout = 60.seconds) {
        val name = randomName()
        val raw = redisson.getMap<String, String>(name, RedissonCodecs.String)
        awaitRedis(raw.fastPutAsync("ratio", "not-a-number")).shouldBeTrue()

        val numeric = redisson1.getLocalCachedMap(
            LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)
        )

        assertFailsWith<RedisException> {
            awaitRedis(numeric.addAndGetAsync("ratio", 0.25))
        }
    }

    /**
     * 독립 client들의 local cache가 명시적 clear barrier를 완료할 때까지 기다립니다.
     *
     * [RLocalCachedMap.getAsync]는 cache miss를 Redis 조회로 보충하므로 barrier로 사용하지
     * 않습니다. 모든 작업이 끝난 후 [RLocalCachedMap.clearLocalCacheAsync]가 발행하는
     * 명시적 clear event의 완료를 기다려 양쪽 view가 기준 원격 값을 다시 읽게 합니다.
     */
    private suspend fun <V> withLocalCacheClearBarrier(
        source: RLocalCachedMap<String, V>,
        observer: RLocalCachedMap<String, V>,
        key: String,
        block: suspend () -> Unit,
    ) {
        observer.cachedKeySet().contains(key).shouldBeTrue()

        block()
        awaitRedis(source.clearLocalCacheAsync())
    }
}
