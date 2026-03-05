package io.bluetape4k.cache.nearcache

import com.hazelcast.cache.HazelcastCachingProvider
import io.bluetape4k.cache.HazelcastServerProvider
import io.bluetape4k.cache.jcache.CaffeineSuspendCache
import io.bluetape4k.cache.jcache.HazelcastSuspendCache
import io.bluetape4k.cache.jcache.SuspendCache
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*
import javax.cache.configuration.MutableConfiguration

@Disabled("Hazelcast Client JCache listener는 non-serializable front cache를 캡처한 listener factory 등록을 허용하지 않습니다.")
class HazelcastSuspendNearCacheTest: AbstractSuspendNearCacheTest() {

    companion object: KLogging()

    override val backSuspendCache: SuspendCache<String, Any> by lazy {
        val provider = HazelcastCachingProvider()
        val properties = HazelcastCachingProvider.propertiesByInstanceItself(HazelcastServerProvider.hazelcastClient)
        val manager = provider.getCacheManager(provider.defaultURI, provider.defaultClassLoader, properties)

        val cacheName = "hazelcast-back-cocache-" + UUID.randomUUID().encodeBase62()
        val config = MutableConfiguration<String, Any>().apply {
            setTypes(String::class.java, Any::class.java)
        }
        val jcache = manager.getCache(cacheName, String::class.java, Any::class.java)
            ?: manager.createCache(cacheName, config)

        HazelcastSuspendCache(jcache)
    }

    override fun createFrontSuspendCache(expireAfterAccess: Duration): SuspendCache<String, Any> =
        CaffeineSuspendCache {
            this.expireAfterAccess(expireAfterAccess)
            this.maximumSize(10_000)
        }

    @Test
    fun `Hazelcast 전용 NearSuspendCache를 생성하고 동작해야 한다`() = runSuspendIO {
        val cacheName = "hazelcast-near-suspend-" + UUID.randomUUID().encodeBase62()
        val cache = HazelcastSuspendNearCache<String, Any>(cacheName)
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
