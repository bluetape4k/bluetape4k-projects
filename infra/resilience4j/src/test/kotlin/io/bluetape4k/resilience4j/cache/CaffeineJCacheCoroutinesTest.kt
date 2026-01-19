package io.bluetape4k.resilience4j.cache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import javax.cache.Cache

class CaffeineJCacheCoroutinesTest: AbstractJCacheCoroutinesTest() {

    companion object: KLoggingChannel()

    override val jcache: Cache<String, String> by lazy {
        CaffeineJCacheProvider.getJCache("caffeine.coroutines")
    }
}
