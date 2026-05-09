package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.time.Duration

class CaffeineSuspendJCacheTest: AbstractSuspendJCacheTest() {

    companion object: KLoggingChannel()

    override val suspendJCache =
        CaffeineSuspendJCache<String, Any> {
            expireAfterWrite(Duration.ofSeconds(60))
            maximumSize(100_000)
        }
}
