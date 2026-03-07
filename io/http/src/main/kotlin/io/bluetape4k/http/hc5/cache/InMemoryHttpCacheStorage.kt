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
import java.util.concurrent.ConcurrentHashMap

/** 메모리에 HTTP 엔터티를 저장하는 [HttpCacheStorage] 구현체입니다. */
class InMemoryHttpCacheStorage<T>(
    config: CacheConfig = CacheConfig.DEFAULT,
    serializer: HttpCacheEntrySerializer<T>,
): AbstractSerializingCacheStorage<T, T>(config.maxUpdateRetries, serializer) {

    companion object: KLogging() {
        /**
         * [HttpCacheStorageEntry]를 직렬화 없이 그대로 저장하는 인메모리 캐시를 생성합니다.
         *
         * @param config 캐시 설정 ([CacheConfig])
         * @return [InMemoryHttpCacheStorage] 인스턴스
         */
        @JvmStatic
        fun createObjectCache(
            config: CacheConfig = CacheConfig.DEFAULT,
        ): InMemoryHttpCacheStorage<HttpCacheStorageEntry> {
            return InMemoryHttpCacheStorage(config, NoopCacheEntrySerializer.INSTANCE)
        }

        /**
         * [HttpCacheStorageEntry]를 [ByteArray]로 직렬화해 저장하는 인메모리 캐시를 생성합니다.
         *
         * @param config 캐시 설정 ([CacheConfig])
         * @param serializer HTTP 캐시 엔트리 직렬화기
         * @return [InMemoryHttpCacheStorage] 인스턴스
         */
        @JvmStatic
        fun createSerializedCache(
            config: CacheConfig = CacheConfig.DEFAULT,
            serializer: HttpCacheEntrySerializer<ByteArray> = HttpByteArrayCacheEntrySerializer.INSTANCE,
        ): InMemoryHttpCacheStorage<ByteArray> {
            return InMemoryHttpCacheStorage(config, serializer)
        }
    }

    private val cache: MutableMap<String, T> = ConcurrentHashMap()

    /**
     * 스토리지 키를 그대로 반환합니다 (별도 변환 없음).
     *
     * @param key 원본 캐시 키
     * @return 스토리지 키 (key 동일)
     */
    override fun digestToStorageKey(key: String): String = key

    /**
     * 지정한 키에 해당하는 캐시 엔트리를 조회합니다.
     *
     * @param storageKey 조회할 캐시 키
     * @return 캐시 엔트리, 없으면 null
     */
    override fun restore(storageKey: String): T? {
        log.trace { "retrieve cache. storageKey=$storageKey" }
        return cache[storageKey]
    }

    /**
     * CAS(Compare-And-Swap) 업데이트를 위해 현재 캐시 엔트리를 조회합니다.
     *
     * @param storageKey 조회할 캐시 키
     * @return 현재 캐시 엔트리, 없으면 null
     */
    override fun getForUpdateCAS(storageKey: String): T? = cache[storageKey]

    /**
     * 지정한 키의 캐시 엔트리를 삭제합니다.
     *
     * @param storageKey 삭제할 캐시 키
     */
    override fun delete(storageKey: String) {
        log.trace { "delete cache. storageKey=$storageKey" }
        cache.remove(storageKey)
    }

    /**
     * 여러 키에 해당하는 캐시 엔트리를 한 번에 조회합니다.
     *
     * @param storageKeys 조회할 캐시 키 컬렉션
     * @return 키 → 캐시 엔트리 맵 (존재하는 항목만 포함)
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
     * CAS(Compare-And-Swap) 방식으로 캐시 엔트리를 업데이트합니다.
     * 현재 값이 [cas]와 같을 때만 [storageObject]로 교체합니다.
     *
     * @param storageKey 업데이트할 캐시 키
     * @param cas 비교할 기존 값
     * @param storageObject 교체할 새 값
     * @return 업데이트 성공 여부
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
     * CAS 값을 스토리지 객체로 그대로 반환합니다.
     *
     * @param cas CAS 값
     * @return 스토리지 객체 (cas 동일)
     */
    override fun getStorageObject(cas: T): T = cas

    /**
     * 지정한 키에 캐시 엔트리를 저장합니다.
     *
     * @param storageKey 저장할 캐시 키
     * @param storageObject 저장할 캐시 엔트리
     */
    override fun store(storageKey: String, storageObject: T) {
        log.debug { "store cache. storageKey=$storageKey, storageObject=$storageObject" }
        cache[storageKey] = storageObject
    }
}
