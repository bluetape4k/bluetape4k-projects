package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.HazelcastCaches
import io.bluetape4k.cache.HazelcastServers
import io.bluetape4k.cache.jcache.CaffeineSuspendJCache
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.junit.jupiter.api.Disabled
import java.time.Duration

@Disabled("HazelcastSuspendNearJCache 가 EventListener 제대로 동작하지 않습니다")
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
