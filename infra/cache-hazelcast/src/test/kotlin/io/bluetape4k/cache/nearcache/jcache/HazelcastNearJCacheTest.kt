package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.HazelcastCaches
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Disabled
import javax.cache.configuration.MutableConfiguration

@Disabled("Hazelcast 가 MutableCacheEntryListenerConfiguration 이 Serializable 이 아니라고 해서 지원할 수 없습니다.")
class HazelcastNearJCacheTest: AbstractNearJCacheTest() {

    companion object: KLogging()

    override val backCache: JCache<String, Any> by lazy {
        val config = MutableConfiguration<String, Any>().apply {
            setTypes(String::class.java, Any::class.java)
        }

        HazelcastCaches.jcache(
            HazelcastServers.hazelcastClient,
            "hazelcast-backcache",
            config
        )
    }

}
