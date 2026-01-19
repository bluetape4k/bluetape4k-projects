package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.time.Duration

class CaffeineSuspendCacheTest: AbstractSuspendCacheTest() {

    companion object: KLoggingChannel()

    override val suspendCache =
        CaffeineSuspendCache<String, Any> {
            expireAfterWrite(Duration.ofSeconds(60))
            maximumSize(100_000)
        }
}
