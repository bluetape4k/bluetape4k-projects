package io.bluetape4k.cache.jcache

import io.bluetape4k.cache.IgniteServers
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IgniteSuspendCacheTest: AbstractSuspendCacheTest() {

    companion object: KLogging()

    override val suspendCache: SuspendCache<String, Any> by lazy {
        IgniteClientSuspendCache(
            IgniteServers.getOrCreateCache("ignite2-suspendCache")
        )
    }
}
