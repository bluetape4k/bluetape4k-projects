package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.testcontainers.storage.RedisServer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import org.redisson.api.RedissonClient
import java.io.Serializable
import java.util.concurrent.TimeUnit
import javax.cache.configuration.CompleteConfiguration
import javax.cache.expiry.CreatedExpiryPolicy
import javax.cache.expiry.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class RedisNearCacheTest: AbstractNearCacheTest() {

    companion object: KLogging() {
        private val redis: RedisServer by lazy { RedisServer.Launcher.redis }

        private val redisson: RedissonClient by lazy {
            RedisServer.Launcher.RedissonLib.getRedisson()
        }

        fun randomHashMap(): HashMap<String, Any?> {
            return HashMap<String, Any?>().apply {
                put("key", randomKey())
                put("key2", randomKey())
            }
        }
    }

    override val nearCacheCfg1 = NearCacheConfig<String, Any>(checkExpiryPeriod = 1_000)
    override val nearCacheCfg2 = NearCacheConfig<String, Any>(checkExpiryPeriod = 1_000)

    override val backCache: JCache<String, Any> by lazy {
        val jcacheConfiguration: CompleteConfiguration<String, Any> = jcacheConfiguration<String, Any> {
            // NOTE: CreatedExpiryPolicy, AccessedExpiryPolicy 등이 있다
            // AccessedExpiryPolicy 는 access 한 이후로 TTL이 갱신된다
            setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration(TimeUnit.MILLISECONDS, 3_000)))
        }

        JCaching.Redisson.getOrCreate<String, Any>(
            "back-cache-" + randomKey(),
            redisson,
            jcacheConfiguration
        )
    }

    // NOTE: Redisson 3.19.1 사용 시, NearCache 에서 kotlin.collections.LinkedHashMap 에 대해 역직렬화에 실패하는 버그가 있습니다.
    // NOTE: java.util.HashMap 을 사용하면, 성공합니다.
    // 참고: https://github.com/redisson/redisson/issues/4809
    data class CacheItem(
        val key: String = randomKey(),
        val value: String = randomValue(),
        val header: Map<String, Any?> = randomHashMap(),
        val claims: Map<String, Any?> = randomHashMap(),
        val signature: String? = Fakers.randomString(16),
    ): Serializable

    @Test
    fun `Redisson Cache Expire 동작 여부`() {
        val key1 = randomKey()
        val key2 = randomKey()
        val value1 = CacheItem()
        val value2 = CacheItem()

        nearCache1.put(key1, value1)
        nearCache2.put(key2, value2)

        await.until { nearCache1.containsKey(key2) }
        await.until { nearCache2.containsKey(key1) }

        await
            .atMost(30.seconds.toJavaDuration())
            .pollDelay(5.seconds.toJavaDuration())
            .until {
                val count = nearCache1.count()
                log.debug { "nearCache1 size=$count" }
                count == 0
            }

        backCache.containsKey(key1).shouldBeFalse()
        backCache.containsKey(key2).shouldBeFalse()

        nearCache1.containsKey(key1).shouldBeFalse()
        nearCache2.containsKey(key1).shouldBeFalse()
    }

    @Test
    fun `remote cache entry가 expire 되면 near cache도 expire 되어야 한다`() {
        val keyCount = 100
        val keys = fastList(keyCount) { it }
            .chunked(10) {
                val entries = it.associate { randomKey() to CacheItem() }
                nearCache1.putAll(entries)
                entries.keys
            }
            .flatten()

        await atMost 5.seconds.toJavaDuration() until { nearCache2.count() > keyCount / 10 }

        // NOTE: backCache 에서 TTL 이 지나면, nearCache 에서도 TTL 이 적용되어 expire 되어야 합니다.
        await
            .atMost(10.seconds.toJavaDuration())
            .pollDelay(2.seconds.toJavaDuration())
            .pollInterval(100.milliseconds.toJavaDuration())
            .until {
                val existKeyCount = nearCache2.count()
                log.debug { "nearCache2 exists key count=$existKeyCount" }
                existKeyCount == 0
            }

        nearCache1.count() shouldBeEqualTo 0
        nearCache2.count() shouldBeEqualTo 0

        // NearCache 내부에서 backCache의 expiration을 검사하여 모든 NearCache에 onExpired event 가 전달되게 합니다.
        val searchKey = keys.random()
        nearCache1.containsKey(searchKey).shouldBeFalse()
        nearCache2.containsKey(searchKey).shouldBeFalse()
    }
}
