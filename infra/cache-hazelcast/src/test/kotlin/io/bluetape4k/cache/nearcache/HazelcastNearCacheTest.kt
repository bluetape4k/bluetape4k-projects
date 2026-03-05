package io.bluetape4k.cache.nearcache

import com.hazelcast.cache.HazelcastCachingProvider
import io.bluetape4k.cache.HazelcastServerProvider
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Disabled
import java.util.*
import javax.cache.configuration.MutableConfiguration

@Disabled("Hazelcast Client JCache listener는 non-serializable front cache를 캡처한 listener factory 등록을 허용하지 않습니다.")
class HazelcastNearCacheTest: AbstractNearCacheTest() {

    companion object: KLogging()

    override val backCache: JCache<String, Any> by lazy {
        val provider = HazelcastCachingProvider()
        val properties =
            HazelcastCachingProvider.propertiesByInstanceItself(HazelcastServerProvider.hazelcastClient)
        val manager = provider.getCacheManager(provider.defaultURI, provider.defaultClassLoader, properties)

        val cacheName = "hazelcast-back-cocache-" + UUID.randomUUID().encodeBase62()
        val config = MutableConfiguration<String, Any>().apply {
            setTypes(String::class.java, Any::class.java)
        }

        manager.getCache(cacheName, String::class.java, Any::class.java)
            ?: manager.createCache(cacheName, config)
    }

}
