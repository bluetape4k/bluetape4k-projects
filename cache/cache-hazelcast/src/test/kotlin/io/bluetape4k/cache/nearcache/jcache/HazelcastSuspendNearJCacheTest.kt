package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.HazelcastCaches
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.cache.jcache.CaffeineSuspendJCache
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Disabled
import java.time.Duration

@Disabled(
    "이유: Hazelcast는 CacheEntryListenerConfiguration을 클러스터 전체에 직렬화하여 배포하므로 " +
        "CaffeineSuspendJCache(non-Serializable)를 캡처한 SuspendJCacheEntryEventListener를 등록할 수 없습니다. " +
        "HazelcastSerializationException: NotSerializableException(CaffeineSuspendJCache). " +
        "Tracked: #490"
)
class HazelcastSuspendNearJCacheTest: AbstractSuspendNearJCacheTest() {

    companion object: KLoggingChannel()

    override val backSuspendJCache: SuspendJCache<String, Any> =
        HazelcastCaches.suspendJCache(HazelcastServers.hazelcastClient, "back-suspend-jcache")

    override fun createFrontSuspendCache(expireAfterAccess: Duration): SuspendJCache<String, Any> {
        return CaffeineSuspendJCache {
            expireAfterAccess(expireAfterAccess)
            maximumSize(100_000)
        }
    }

}
