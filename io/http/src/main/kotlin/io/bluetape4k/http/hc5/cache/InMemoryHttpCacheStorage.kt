package io.bluetape4k.http.hc5.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import org.apache.hc.client5.http.cache.HttpCacheEntrySerializer
import org.apache.hc.client5.http.cache.HttpCacheStorage
import org.apache.hc.client5.http.cache.HttpCacheStorageEntry
import org.apache.hc.client5.http.impl.cache.AbstractSerializingCacheStorage
import org.apache.hc.client5.http.impl.cache.CacheConfig
import org.apache.hc.client5.http.impl.cache.HttpByteArrayCacheEntrySerializer
import org.apache.hc.client5.http.impl.cache.NoopCacheEntrySerializer

/** 메모리에 HTTP 엔터티를 저장하는 [HttpCacheStorage] 구현체입니다. */
class InMemoryHttpCacheStorage<T>(
    config: CacheConfig = CacheConfig.DEFAULT,
    serializer: HttpCacheEntrySerializer<T>,
): AbstractSerializingCacheStorage<T, T>(config.maxUpdateRetries, serializer) {

    companion object: KLogging() {
        /**
         * HTTP 처리에서 `createObjectCache` 함수를 제공합니다.
         */
        @JvmStatic
        fun createObjectCache(
            config: CacheConfig = CacheConfig.DEFAULT,
        ): InMemoryHttpCacheStorage<HttpCacheStorageEntry> {
            return InMemoryHttpCacheStorage(config, NoopCacheEntrySerializer.INSTANCE)
        }

        /**
         * HTTP 처리에서 `createSerializedCache` 함수를 제공합니다.
         */
        @JvmStatic
        fun createSerializedCache(
            config: CacheConfig = CacheConfig.DEFAULT,
            serializer: HttpCacheEntrySerializer<ByteArray> = HttpByteArrayCacheEntrySerializer.INSTANCE,
        ): InMemoryHttpCacheStorage<ByteArray> {
            return InMemoryHttpCacheStorage(config, serializer)
        }
    }

    private val cache: MutableMap<String, T> = mutableMapOf()

    /**
     * HTTP 처리에서 `digestToStorageKey` 함수를 제공합니다.
     */
    override fun digestToStorageKey(key: String): String = key

    /**
     * HTTP 처리에서 `restore` 함수를 제공합니다.
     */
    override fun restore(storageKey: String): T? {
        log.trace { "retrieve cache. storageKey=$storageKey" }
        return cache[storageKey]
    }

    /**
     * HTTP 처리에서 `getForUpdateCAS` 함수를 제공합니다.
     */
    override fun getForUpdateCAS(storageKey: String): T? = cache[storageKey]

    /**
     * HTTP 처리에서 `delete` 함수를 제공합니다.
     */
    override fun delete(storageKey: String) {
        log.trace { "delete cache. storageKey=$storageKey" }
        cache.remove(storageKey)
    }

    /**
     * HTTP 처리에서 `bulkRestore` 함수를 제공합니다.
     */
    override fun bulkRestore(storageKeys: MutableCollection<String>): MutableMap<String, T> {
        log.debug { "bulk store cache. storageKeys=${storageKeys.joinToString(",")}" }
        if (storageKeys.isEmpty()) {
            return mutableMapOf()
        }
        val result = mutableMapOf<String, T>()
        storageKeys.forEach { key ->
            cache[key]?.let { result[key] = it }
        }
        return result
    }

    /**
     * HTTP 처리에서 `updateCAS` 함수를 제공합니다.
     */
    override fun updateCAS(storageKey: String, cas: T, storageObject: T): Boolean {
        log.debug { "update cas. storageKey=$storageKey, cas=$cas, storageObject=$storageObject" }

        val oldValue = cache[storageKey]
        return if (cas == oldValue) {
            cache[storageKey] = storageObject
            true
        } else {
            false
        }
    }

    /**
     * HTTP 처리에서 `getStorageObject` 함수를 제공합니다.
     */
    override fun getStorageObject(cas: T): T = cas

    /**
     * HTTP 처리에서 `store` 함수를 제공합니다.
     */
    override fun store(storageKey: String, storageObject: T) {
        log.debug { "store cache. storageKey=$storageKey, storageObject=$storageObject" }
        cache[storageKey] = storageObject
    }
}
