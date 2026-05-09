package io.bluetape4k.cache.jcache

import com.hazelcast.cache.HazelcastCachingProvider
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.*
import javax.cache.configuration.MutableConfiguration

class HazelcastSuspendJCacheTest: AbstractSuspendJCacheTest() {

    companion object: KLoggingChannel()

    override val suspendJCache: SuspendJCache<String, Any> by lazy {
        val provider = HazelcastCachingProvider()
        val properties = HazelcastCachingProvider.propertiesByInstanceItself(HazelcastServers.hazelcastClient)
        val manager = provider.getCacheManager(provider.defaultURI, provider.defaultClassLoader, properties)

        val cacheName = "hazelcast-cocache-" + UUID.randomUUID().encodeBase62()
        val config = MutableConfiguration<String, Any>().apply {
            setTypes(String::class.java, Any::class.java)
        }
        val jcache = manager.getCache(cacheName, String::class.java, Any::class.java)
            ?: manager.createCache(cacheName, config)

        HazelcastSuspendJCache(jcache)
    }
}
