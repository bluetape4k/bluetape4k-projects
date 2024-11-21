package io.bluetape4k.examples.redisson.coroutines.collections

import io.bluetape4k.examples.redisson.coroutines.AbstractRedissonCoroutineTest
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.redisson.coroutines.coAwait
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.await
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RMap
import org.redisson.api.RedissonClient
import org.redisson.api.options.LocalCachedMapOptions
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * [RLocalCachedMap] 예제
 *
 * 참고: [Local Cache](https://github.com/redisson/redisson/wiki/7.-distributed-collections#local-cache)
 */
class LocalCachedMapTest: AbstractRedissonCoroutineTest() {

    private lateinit var redisson1: RedissonClient
    private lateinit var redisson2: RedissonClient

    private val cacheName = randomName()

    private val options1 = LocalCachedMapOptions.name<String, Int>(cacheName)
        .cacheSize(100)
        .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
        .maxIdle(10.seconds.toJavaDuration())
        .timeToLive(5.seconds.toJavaDuration())


    private val options2 = LocalCachedMapOptions.name<String, Int>(cacheName)
        .cacheSize(100)
        .evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LFU)
        .maxIdle(10.seconds.toJavaDuration())
        .timeToLive(5.seconds.toJavaDuration())

    private val frontCache1: RLocalCachedMap<String, Int> by lazy { redisson1.getLocalCachedMap(options1) }
    private val frontCache2: RLocalCachedMap<String, Int> by lazy { redisson2.getLocalCachedMap(options2) }
    private val backCache: RMap<String, Int> by lazy { redisson.getMap(cacheName) }

    @BeforeAll
    fun setup() {
        redisson1 = newRedisson()
        redisson2 = newRedisson()
    }

    @AfterAll
    fun cleanup() {
        if (this::redisson1.isInitialized) {
            redisson1.shutdown()
        }
        if (this::redisson2.isInitialized) {
            redisson2.shutdown()
        }
    }

    @Test
    fun `frontCache1 에 cache item을 추가하면 frontCache2에 추가됩니다`() = runTest {
        val keyToAdd = randomName()

        log.debug { "front cache1: put key=$keyToAdd" }
        frontCache1.fastPutAsync(keyToAdd, 42).coAwait()
        await.until { backCache.containsKey(keyToAdd) }

        log.debug { "front cache2: get key=$keyToAdd" }
        frontCache2.getAsync(keyToAdd).coAwait() shouldBeEqualTo 42
    }

    @Test
    fun `frontCache1의 cache item을 삭제하면 frontCache2에서도 삭제됩니다`() = runTest {
        val keyToRemove = randomName()

        log.debug { "front cache1: put $keyToRemove" }
        frontCache1.fastPutAsync(keyToRemove, 42).coAwait()
        await.until { backCache.containsKey(keyToRemove) }
        frontCache2.getAsync(keyToRemove).coAwait() shouldBeEqualTo 42

        log.debug { "front cache1: remove $keyToRemove" }
        frontCache1.fastRemoveAsync(keyToRemove).coAwait()
        await.until { !backCache.containsKey(keyToRemove) }
        frontCache2.getAsync(keyToRemove).coAwait().shouldBeNull()
    }

    @Test
    fun `backCache에 cache item을 추가하면 frontCache 에 반영된다`() = runTest {
        val key = randomName()

        // 초기에 frontCache에 존재하지 않는다.
        frontCache1.containsKeyAsync(key).coAwait().shouldBeFalse()
        frontCache2.containsKeyAsync(key).coAwait().shouldBeFalse()

        // bachCache에 cache 등록
        backCache.fastPutAsync(key, 42).coAwait()

        await.atMost(Duration.ofSeconds(1)).until { frontCache1.containsKey(key) }

        // frontCache에 등록 반영
        frontCache1.containsKeyAsync(key).coAwait().shouldBeTrue()
        frontCache2.containsKeyAsync(key).coAwait().shouldBeTrue()

        // backCache에서 cache 삭제
        backCache.fastRemoveAsync(key).coAwait() shouldBeEqualTo 1L

        await.atMost(Duration.ofSeconds(1)).until { !frontCache1.containsKey(key) }

        // frontCache에 삭제 반영
        frontCache1.containsKeyAsync(key).coAwait().shouldBeFalse()
        frontCache2.containsKeyAsync(key).coAwait().shouldBeFalse()
    }
}
