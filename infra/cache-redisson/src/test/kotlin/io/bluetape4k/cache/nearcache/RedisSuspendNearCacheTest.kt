package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.RedisServers
import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.RedissonSuspendCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.idgenerators.uuid.TimebasedUuid
import io.bluetape4k.junit5.awaitility.untilSuspending
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeTrue
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import javax.cache.expiry.CreatedExpiryPolicy
import javax.cache.expiry.Duration
import kotlin.time.Duration.Companion.seconds

class RedisSuspendNearCacheTest: AbstractSuspendNearCacheTest() {

    companion object: KLogging()

    override val backSuspendCache: SuspendCache<String, Any> by lazy {
        val configuration = jcacheConfiguration<String, Any> {
            setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration(TimeUnit.MILLISECONDS, 1000L)))
        }
        RedissonSuspendCache(
            "redis-back-cocache" + TimebasedUuid.Epoch.nextIdAsString(),
            RedisServers.redisson,
            configuration
        )
    }

    override fun createFrontSuspendCache(expireAfterAccess: java.time.Duration): SuspendCache<String, Any> =
        CaffeineSuspendCache {
            this.expireAfterAccess(expireAfterAccess)
            this.maximumSize(10_000)
        }

    @Test
    fun `Redisson 전용 SuspendNearCache를 생성하고 동작해야 한다`() = runSuspendIO {
        val cacheName = "redis-near-suspend-" + TimebasedUuid.Epoch.nextIdAsString()
        val cache = RedissonSuspendNearCache<String, Any>(cacheName, RedisServers.redisson)
        cache shouldBeInstanceOf SuspendNearCache::class

        val key = getKey()
        val value = getValue()
        cache.put(key, value)
        cache.get(key) shouldBeEqualTo value

        cache.clearAll()

        // NOTE: Redisson 의 DEFAULT_EXPIRY_CHECK_PERIOD = 30_000ms 이다.
        await atMost 40.seconds untilSuspending {
            !cache.containsKey(key)
        }
        cache.close()
    }

    @Test
    fun `back cache entry가 expire 되면 event listener를 통해 front cache가 삭제됩니다`() = runSuspendIO {
        val key = getKey()
        val value = getValue()

        suspendNearCache1.put(key, value)
        await untilSuspending { suspendNearCache2.containsKey(key) }

        suspendNearCache1.containsKey(key).shouldBeTrue()
        suspendNearCache2.containsKey(key).shouldBeTrue()

        // NOTE: backCache 에서 cache expire 가 수행될 때까지 대기한다 (backCache.entries 에 접근하면 expired event 가 발생한다)
        // NearCache 내에서 Expire 검사 Thread로 동작해야 합니다.
        await atMost 10.seconds untilSuspending { !suspendNearCache2.containsKey(key) }
        await atMost 10.seconds untilSuspending { !suspendNearCache1.containsKey(key) }

        backSuspendCache.containsKey(key).shouldBeFalse()
        suspendNearCache1.containsKey(key).shouldBeFalse()
        suspendNearCache2.containsKey(key).shouldBeFalse()
    }
}
