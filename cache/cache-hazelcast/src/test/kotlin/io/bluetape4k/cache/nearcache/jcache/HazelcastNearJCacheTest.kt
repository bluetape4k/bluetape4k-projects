package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.HazelcastCaches
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Disabled
import javax.cache.configuration.MutableConfiguration

@Disabled(
    "이유: Hazelcast는 MutableCacheEntryListenerConfiguration을 클러스터 전체에 직렬화하여 배포하므로 " +
        "non-Serializable 리스너(JCacheEntryEventListener)를 등록할 수 없습니다. " +
        "HazelcastSerializationException: NotSerializableException. " +
        "Tracked: #490"
)
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
