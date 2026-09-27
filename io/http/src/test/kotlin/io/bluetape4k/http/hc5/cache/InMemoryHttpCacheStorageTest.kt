package io.bluetape4k.http.hc5.cache

import io.bluetape4k.logging.KLogging
import org.apache.hc.client5.http.cache.HttpCacheStorage

class InMemoryHttpCacheStorageTest: AbstractHttpCacheStorageTest() {

    companion object: KLogging()

    override fun createCacheStorage(): HttpCacheStorage {
        return InMemoryHttpCacheStorage.createObjectCache()
    }
}
