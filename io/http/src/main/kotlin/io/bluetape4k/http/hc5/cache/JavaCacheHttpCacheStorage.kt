package io.bluetape4k.http.hc5.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.apache.hc.client5.http.cache.HttpCacheEntrySerializer
import org.apache.hc.client5.http.cache.HttpCacheStorage
import org.apache.hc.client5.http.cache.HttpCacheStorageEntry
import org.apache.hc.client5.http.impl.cache.AbstractSerializingCacheStorage
import org.apache.hc.client5.http.impl.cache.CacheConfig
import org.apache.hc.client5.http.impl.cache.HttpByteArrayCacheEntrySerializer
import org.apache.hc.client5.http.impl.cache.NoopCacheEntrySerializer
import javax.cache.Cache

/**
 * Java Cache를 이용한 Http Entity를 캐시하는 [HttpCacheStorage] 구현체입니다.
 */
class JavaCacheHttpCacheStorage<T>(
    private val cache: Cache<String, T>,
    config: CacheConfig = CacheConfig.DEFAULT,
    serializer: HttpCacheEntrySerializer<T>,
): AbstractSerializingCacheStorage<T, T>(config.maxUpdateRetries, serializer) {

    companion object: KLogging() {
        @JvmStatic
        fun createObjectCache(
            cache: Cache<String, HttpCacheStorageEntry>,
            config: CacheConfig = CacheConfig.DEFAULT,
        ): JavaCacheHttpCacheStorage<HttpCacheStorageEntry> {
            return JavaCacheHttpCacheStorage(cache, config, NoopCacheEntrySerializer.INSTANCE)
        }

        @JvmStatic
        fun createSerializedCache(
            cache: Cache<String, ByteArray>,
            config: CacheConfig = CacheConfig.DEFAULT,
            serializer: HttpCacheEntrySerializer<ByteArray> = HttpByteArrayCacheEntrySerializer.INSTANCE,
        ): JavaCacheHttpCacheStorage<ByteArray> {
            return JavaCacheHttpCacheStorage(cache, config, serializer)
        }
    }


    override fun digestToStorageKey(key: String): String = key

    override fun restore(storageKey: String): T? {
        log.trace { "retrieve cache. storageKey=$storageKey" }
        return cache[storageKey]
    }

    override fun getForUpdateCAS(storageKey: String): T? {
        log.trace { "get for update cas. storageKey=$storageKey" }
        return cache[storageKey]
    }

    override fun delete(storageKey: String) {
        log.trace { "delete cache. storageKey=$storageKey" }
        cache.remove(storageKey)
    }

    override fun bulkRestore(storageKeys: MutableCollection<String>): MutableMap<String, T> {
        log.trace { "bulk store cache. storageKeys=${storageKeys.joinToString(",")}" }
        if (storageKeys.isEmpty()) {
            return mutableMapOf()
        }
        val resultMap = mutableMapOf<String, T>()
        storageKeys.forEach { key ->
            cache[key]?.let { resultMap[key] = it }
        }
        return resultMap
    }

    override fun updateCAS(storageKey: String, cas: T, storageObject: T): Boolean {
        log.trace { "update cas. storageKey=$storageKey, cas=$cas, storageObject=$storageObject" }
        return cache.replace(storageKey, cas, storageObject)
    }

    override fun getStorageObject(cas: T): T = cas

    override fun store(storageKey: String, storageObject: T) {
        log.trace { "store cache. storageKey=$storageKey, storageObject=$storageObject" }
        cache.put(storageKey, storageObject)
    }
}
