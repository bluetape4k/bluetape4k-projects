package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.time.Duration

class CaffeineCoCacheTest: AbstractCoCacheTest() {

    companion object: KLoggingChannel()

    override val coCache by lazy {
        CaffeineCoCache<String, Any> {
            expireAfterWrite(Duration.ofSeconds(60))
            maximumSize(100_000)
        }
    }
}
