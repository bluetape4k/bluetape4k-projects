package io.bluetape4k.cache.jcache.coroutines

import com.hazelcast.cache.HazelcastCachingProvider
import com.hazelcast.client.HazelcastClient
import com.hazelcast.client.config.ClientConfig
import io.bluetape4k.codec.encodeBase62
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.testcontainers.storage.HazelcastServer
import io.bluetape4k.utils.ShutdownQueue
import java.util.*
import javax.cache.configuration.MutableConfiguration

class HazelcastSuspendCacheTest: AbstractSuspendCacheTest() {

    companion object: KLoggingChannel() {
        private val hazelcastServer by lazy { HazelcastServer.Launcher.hazelcast }
        private val hazelcastClient by lazy {
            val cfg = ClientConfig().apply {
                networkConfig.addAddress(hazelcastServer.url)
            }
            HazelcastClient.newHazelcastClient(cfg).also {
                ShutdownQueue.register { it.shutdown() }
            }
        }
    }

    override val suspendCache: SuspendCache<String, Any> by lazy {
        val provider = HazelcastCachingProvider()
        val properties = HazelcastCachingProvider.propertiesByInstanceItself(hazelcastClient)
        val manager = provider.getCacheManager(provider.defaultURI, provider.defaultClassLoader, properties)

        val cacheName = "hazelcast-cocache-" + UUID.randomUUID().encodeBase62()
        val config = MutableConfiguration<String, Any>().apply {
            setTypes(String::class.java, Any::class.java)
        }
        val jcache = manager.getCache(cacheName, String::class.java, Any::class.java)
            ?: manager.createCache(cacheName, config)

        HazelcastSuspendCache(jcache)
    }
}
