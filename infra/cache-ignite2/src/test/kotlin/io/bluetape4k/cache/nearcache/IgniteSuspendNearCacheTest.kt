package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.IgniteServers.igniteClient
import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.IgniteClientSuspendCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*

// Order(1): IgniteCachingProvider 로 embedded Ignite 노드를 시작하는 IgniteNearCacheTest 보다
// 먼저 실행되어야 thin client 의 backSuspendCache 가 간섭 없이 동작합니다.
@Order(1)
class IgniteSuspendNearCacheTest: AbstractSuspendNearCacheTest() {

    companion object: KLoggingChannel()

    override val backSuspendCache: SuspendCache<String, Any> by lazy {
        IgniteClientSuspendCache(
            igniteClient.getOrCreateCache("ignite2-back-cocache-" + UUID.randomUUID().encodeBase62())
        )
    }

    override fun createFrontSuspendCache(expireAfterAccess: Duration): SuspendCache<String, Any> =
        CaffeineSuspendCache {
            this.expireAfterAccess(expireAfterAccess)
            this.maximumSize(10_000)
        }

    @Test
    fun `Ignite 전용 NearSuspendCache를 생성하고 동작해야 한다`() = runSuspendIO {
        val cacheName = "ignite2-near-suspend-" + UUID.randomUUID().encodeBase62()
        val cache = IgniteSuspendNearCache<String, Any>(
            frontSuspendCache = CaffeineSuspendCache { maximumSize(10_000) },
            backSuspendCache = IgniteClientSuspendCache(igniteClient.getOrCreateCache(cacheName)),
        )
        cache shouldBeInstanceOf SuspendNearCache::class

        val key = getKey()
        val value = getValue()
        cache.put(key, value)
        cache.get(key) shouldBeEqualTo value

        cache.clearAll()
        cache.containsKey(key).shouldBeFalse()
        cache.close()
    }
}
