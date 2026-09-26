package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.coroutines.support.awaitUntil
import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import kotlinx.coroutines.withTimeout
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.withPollInterval
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.client.RedisException
import org.redisson.codec.CompositeCodec
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds
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

    companion object: KLoggingChannel() {
        private const val CACHE_SIZE = 100_000
    }

    private lateinit var redisson1: RedissonClient
    private lateinit var redisson2: RedissonClient

    private val cacheName = randomName()

    private val options1 = LocalCachedMapOptions.name<String, Int>(cacheName)
        .cacheSize(CACHE_SIZE)
        .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
        .maxIdle(10.seconds.toJavaDuration())
        .timeToLive(5.seconds.toJavaDuration())
        .codec(intCodec)

    private val options2 = LocalCachedMapOptions.name<String, Int>(cacheName)
        .cacheSize(CACHE_SIZE)
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
                        firstFailure.shouldNotBeNull().addSuppressed(failure)
                    }
                }
        }

        firstFailure?.let { throw it }
    }

    @Test
    fun `frontCache1 에 cache item을 추가하면 frontCache2에 추가됩니다`() = runSuspendIO(timeout = 60.seconds) {
        val keyToAdd = randomName()

        log.debug { "front cache1: put key=$keyToAdd" }
        frontCache1.fastPutAsync(keyToAdd, 42).awaitUntil().shouldBeTrue()
        await atMost 5.seconds withPollInterval 100.milliseconds untilSuspending {
            backCache.containsKeyAsync(keyToAdd).awaitUntil()
        }

        log.debug { "front cache2: get key=$keyToAdd" }
        frontCache2.getAsync(keyToAdd).awaitUntil() shouldBeEqualTo 42
    }

    @Test
    fun `frontCache1의 cache item을 삭제하면 frontCache2에서도 삭제됩니다`() = runSuspendIO(timeout = 60.seconds) {
        val keyToRemove = randomName()

        log.debug { "front cache1: put $keyToRemove" }
        frontCache1.fastPutAsync(keyToRemove, 42).awaitUntil().shouldBeTrue()
        await atMost 5.seconds withPollInterval 100.milliseconds untilSuspending {
            backCache.containsKeyAsync(keyToRemove).awaitUntil()
        }
        frontCache2.getAsync(keyToRemove).awaitUntil() shouldBeEqualTo 42

        log.debug { "front cache1: remove $keyToRemove" }
        frontCache1.fastRemoveAsync(keyToRemove).awaitUntil() shouldBeEqualTo 1L
        await atMost 5.seconds withPollInterval 100.milliseconds untilSuspending {
            backCache.containsKeyAsync(keyToRemove).awaitUntil().not()
        }
        frontCache2.getAsync(keyToRemove).awaitUntil().shouldBeNull()
    }

    @Test
    fun `backCache에 cache item을 추가하면 frontCache 에 반영된다`() = runSuspendIO(timeout = 60.seconds) {
        val key = randomName()

        frontCache1.containsKeyAsync(key).awaitUntil().shouldBeFalse()
        frontCache2.containsKeyAsync(key).awaitUntil().shouldBeFalse()

        backCache.fastPutAsync(key, 42).awaitUntil().shouldBeTrue()

        await atMost 5.seconds withPollInterval 100.milliseconds untilSuspending {
            frontCache1.containsKeyAsync(key).awaitUntil() &&
                    frontCache2.containsKeyAsync(key).awaitUntil()
        }

        frontCache1.containsKeyAsync(key).awaitUntil().shouldBeTrue()
        frontCache2.containsKeyAsync(key).awaitUntil().shouldBeTrue()

        backCache.fastRemoveAsync(key).awaitUntil() shouldBeEqualTo 1L

        await atMost 5.seconds withPollInterval 100.milliseconds untilSuspending {
            frontCache1.containsKeyAsync(key).awaitUntil().not() &&
                    frontCache2.containsKeyAsync(key).awaitUntil().not()
        }

        frontCache1.containsKeyAsync(key).awaitUntil().shouldBeFalse()
        frontCache2.containsKeyAsync(key).awaitUntil().shouldBeFalse()
    }

    @Test
    fun `frontCache1 remote update invalidates both cached values`() = runSuspendIO(60.seconds) {
        val key = randomName()

        frontCache1.fastPutAsync(key, 7).awaitUntil().shouldBeTrue()
        await atMost 5.seconds withPollInterval 100.milliseconds untilSuspending {
            frontCache1.getAsync(key).awaitUntil() == 7 &&
                    frontCache2.getAsync(key).awaitUntil() == 7
        }

        frontCache1.fastPutAsync(key, 42).awaitUntil().shouldBeFalse()
        await atMost 5.seconds withPollInterval 100.milliseconds untilSuspending {
            frontCache1.getAsync(key).awaitUntil() == 42 &&
                    frontCache2.getAsync(key).awaitUntil() == 42
        }

        frontCache1.fastRemoveAsync(key).awaitUntil() shouldBeEqualTo 1L
    }

    @Test
    fun `concurrent Int increments match independent remote final value`() = runSuspendIO(60.seconds) {
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
        remote.fastPutAsync("count", 0).awaitUntil().shouldBeTrue()
        map1.getAsync("count").awaitUntil() shouldBeEqualTo 0
        map2.getAsync("count").awaitUntil() shouldBeEqualTo 0

        withLocalCacheClearBarrier(map1, map2, "count") {
            withTimeout(30.seconds) {
                SuspendedJobTester()
                    .workers(4)
                    .rounds(calls)
                    .add {
                        map1.addAndGetAsync("count", 1).awaitUntil(30.seconds)
                        completed.incrementAndGet()
                    }
                    .run()
            }

            completed.get() shouldBeEqualTo calls
            remote.getAsync("count").awaitUntil() shouldBeEqualTo calls
        }
        await atMost 5.seconds withPollInterval 100.milliseconds untilSuspending {
            map1.getAsync("count").awaitUntil() == calls &&
                    map2.getAsync("count").awaitUntil() == calls
        }
        map1.getAsync("count").awaitUntil() shouldBeEqualTo calls
        map2.getAsync("count").awaitUntil() shouldBeEqualTo calls
    }

    @Test
    fun `concurrent Double increments match independent remote final value`() = runSuspendIO(60.seconds) {
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
        remote.fastPutAsync("ratio", 0.0).awaitUntil().shouldBeTrue()
        map1.getAsync("ratio").awaitUntil() shouldBeEqualTo 0.0
        map2.getAsync("ratio").awaitUntil() shouldBeEqualTo 0.0

        withLocalCacheClearBarrier(map1, map2, "ratio") {
            withTimeout(30.seconds) {
                SuspendedJobTester()
                    .workers(4)
                    .rounds(calls)
                    .add {
                        map1.addAndGetAsync("ratio", 0.25).awaitUntil(timeout = 30.seconds)
                        completed.incrementAndGet()
                    }
                    .run()
            }

            completed.get() shouldBeEqualTo calls
            remote.getAsync("ratio").awaitUntil() shouldBeEqualTo expected
        }
        await atMost 5.seconds withPollInterval 100.milliseconds untilSuspending {
            map1.getAsync("ratio").awaitUntil() == expected &&
                    map2.getAsync("ratio").awaitUntil() == expected
        }
        map1.getAsync("ratio").awaitUntil() shouldBeEqualTo expected
        map2.getAsync("ratio").awaitUntil() shouldBeEqualTo expected
    }

    @Test
    fun `non numeric stored value is rejected by numeric increment`() = runSuspendIO(60.seconds) {
        val name = randomName()
        val raw = redisson.getMap<String, String>(name, RedissonCodecs.String)
        raw.fastPutAsync("ratio", "not-a-number").awaitUntil().shouldBeTrue()

        val numeric = redisson1.getLocalCachedMap(
            LocalCachedMapOptions.name<String, Double>(name).codec(doubleCodec)
        )

        assertFailsWith<RedisException> {
            numeric.addAndGetAsync("ratio", 0.25).awaitUntil()
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
        source.clearLocalCacheAsync().awaitUntil()
    }
}
