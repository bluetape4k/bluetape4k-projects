package io.bluetape4k.http.hc5.cache

import org.apache.hc.client5.http.cache.HttpCacheStorage

class InMemoryHttpCacheStorageTest: AbstractHttpCacheStorageTest() {

    override fun createCacheStorage(): HttpCacheStorage {
        return InMemoryHttpCacheStorage.createObjectCache()
    }
}
