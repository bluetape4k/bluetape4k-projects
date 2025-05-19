package io.bluetape4k.resilience4j.cache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import javax.cache.Cache

class Cache2kJCacheCoroutinesTest: AbstractJCacheCoroutinesTest() {

    companion object: KLoggingChannel()

    override val jcache: Cache<String, String> = Cache2kJCacheProvider.getJCache("cache2k.coroutines")

}
