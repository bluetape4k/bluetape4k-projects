package io.bluetape4k.cache.nearcache.coroutines

import com.hazelcast.cache.HazelcastCachingProvider
import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import io.bluetape4k.cache.jcache.coroutines.HazelcastSuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.testcontainers.storage.HazelcastServer
import io.bluetape4k.utils.ShutdownQueue
import org.junit.jupiter.api.Disabled
import java.util.*
import javax.cache.configuration.MutableConfiguration

@Disabled("Hazelcast Client JCache listener는 non-serializable front cache를 캡처한 listener factory 등록을 허용하지 않습니다.")
class HazelcastNearSuspendCacheTest: AbstractNearSuspendCacheTest() {

    companion object {
        private val hazelcastServer by lazy { HazelcastServer.Launcher.hazelcast }
        private val hazelcastClient by lazy {
            HazelcastClient.newHazelcastClient(
                ClientConfig().apply {
                    networkConfig.addAddress(hazelcastServer.url)
                }
            ).also { ShutdownQueue.register { it.shutdown() } }
        }
    }

    override val backSuspendCache: SuspendCache<String, Any> by lazy {
        val provider = HazelcastCachingProvider()
        val properties = HazelcastCachingProvider.propertiesByInstanceItself(hazelcastClient)
        val manager = provider.getCacheManager(provider.defaultURI, provider.defaultClassLoader, properties)

        val cacheName = "hazelcast-back-cocache-" + UUID.randomUUID().encodeBase62()
        val config = MutableConfiguration<String, Any>().apply {
            setTypes(String::class.java, Any::class.java)
        }
        val jcache = manager.getCache(cacheName, String::class.java, Any::class.java)
            ?: manager.createCache(cacheName, config)

        HazelcastSuspendCache(jcache)
    }
}
