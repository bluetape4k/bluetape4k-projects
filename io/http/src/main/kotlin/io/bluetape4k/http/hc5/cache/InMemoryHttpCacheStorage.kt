package io.bluetape4k.http.hc5.cache

import io.bluetape4k.collections.eclipse.emptyUnifiedMap
import io.bluetape4k.collections.eclipse.unifiedMapOf
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
import org.eclipse.collections.impl.map.mutable.UnifiedMap

/**
 * 메모리에 Http Entity를 캐시하는 [HttpCacheStorage] 구현체입니다.
 */
class InMemoryHttpCacheStorage<T>(
    config: CacheConfig = CacheConfig.DEFAULT,
    serializer: HttpCacheEntrySerializer<T>,
): AbstractSerializingCacheStorage<T, T>(config.maxUpdateRetries, serializer) {

    companion object: KLogging() {
        @JvmStatic
        fun createObjectCache(
            config: CacheConfig = CacheConfig.DEFAULT,
        ): InMemoryHttpCacheStorage<HttpCacheStorageEntry> {
            return InMemoryHttpCacheStorage(config, NoopCacheEntrySerializer.INSTANCE)
        }

        @JvmStatic
        fun createSerializedCache(
            config: CacheConfig = CacheConfig.DEFAULT,
            serializer: HttpCacheEntrySerializer<ByteArray> = HttpByteArrayCacheEntrySerializer.INSTANCE,
        ): InMemoryHttpCacheStorage<ByteArray> {
            return InMemoryHttpCacheStorage(config, serializer)
        }
    }

    private val cache: UnifiedMap<String, T> = unifiedMapOf()

    override fun digestToStorageKey(key: String): String = key

    override fun restore(storageKey: String): T? {
        log.trace { "retrieve cache. storageKey=$storageKey" }
        return cache[storageKey]
    }

    override fun getForUpdateCAS(storageKey: String): T? = cache[storageKey]

    override fun delete(storageKey: String) {
        log.trace { "delete cache. storageKey=$storageKey" }
        cache.remove(storageKey)
    }

    override fun bulkRestore(storageKeys: MutableCollection<String>): MutableMap<String, T> {
        log.debug { "bulk store cache. storageKeys=${storageKeys.joinToString(",")}" }
        if (storageKeys.isEmpty()) {
            return emptyUnifiedMap()
        }
        val result = unifiedMapOf<String, T>()
        storageKeys.forEach { key ->
            cache[key]?.let { result[key] = it }
        }
        return result
    }

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

    override fun getStorageObject(cas: T): T = cas

    override fun store(storageKey: String, storageObject: T) {
        log.debug { "store cache. storageKey=$storageKey, storageObject=$storageObject" }
        cache[storageKey] = storageObject
    }
}
