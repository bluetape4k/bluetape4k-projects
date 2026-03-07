package io.bluetape4k.http.hc5.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.hc.client5.http.cache.HttpCacheEntrySerializer
import org.apache.hc.client5.http.cache.HttpCacheStorage
import org.apache.hc.client5.http.cache.HttpCacheStorageEntry
import org.apache.hc.client5.http.impl.cache.AbstractSerializingCacheStorage
import org.apache.hc.client5.http.impl.cache.CacheConfig
import org.apache.hc.client5.http.impl.cache.HttpByteArrayCacheEntrySerializer
import org.apache.hc.client5.http.impl.cache.NoopCacheEntrySerializer
import javax.cache.Cache

/** Java 캐시를 이용해 HTTP 엔터티를 저장하는 [HttpCacheStorage] 구현체입니다. */
class JavaCacheHttpCacheStorage<T>(
    private val cache: Cache<String, T>,
    config: CacheConfig = CacheConfig.DEFAULT,
    serializer: HttpCacheEntrySerializer<T>,
): AbstractSerializingCacheStorage<T, T>(config.maxUpdateRetries, serializer) {

    companion object: KLogging() {
        /**
         * [HttpCacheStorageEntry]를 직렬화 없이 그대로 저장하는 JCache 기반 캐시를 생성합니다.
         *
         * @param cache JCache [Cache] 인스턴스
         * @param config 캐시 설정 ([CacheConfig])
         * @return [JavaCacheHttpCacheStorage] 인스턴스
         */
        @JvmStatic
        fun createObjectCache(
            cache: Cache<String, HttpCacheStorageEntry>,
            config: CacheConfig = CacheConfig.DEFAULT,
        ): JavaCacheHttpCacheStorage<HttpCacheStorageEntry> {
            return JavaCacheHttpCacheStorage(cache, config, NoopCacheEntrySerializer.INSTANCE)
        }

        /**
         * [HttpCacheStorageEntry]를 [ByteArray]로 직렬화해 저장하는 JCache 기반 캐시를 생성합니다.
         *
         * @param cache JCache [Cache] 인스턴스
         * @param config 캐시 설정 ([CacheConfig])
         * @param serializer HTTP 캐시 엔트리 직렬화기
         * @return [JavaCacheHttpCacheStorage] 인스턴스
         */
        @JvmStatic
        fun createSerializedCache(
            cache: Cache<String, ByteArray>,
            config: CacheConfig = CacheConfig.DEFAULT,
            serializer: HttpCacheEntrySerializer<ByteArray> = HttpByteArrayCacheEntrySerializer.INSTANCE,
        ): JavaCacheHttpCacheStorage<ByteArray> {
            return JavaCacheHttpCacheStorage(cache, config, serializer)
        }
    }

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
        log.debug { "retrieve cache. storageKey=$storageKey" }
        return cache[storageKey]
    }

    /**
     * CAS(Compare-And-Swap) 업데이트를 위해 현재 캐시 엔트리를 조회합니다.
     *
     * @param storageKey 조회할 캐시 키
     * @return 현재 캐시 엔트리, 없으면 null
     */
    override fun getForUpdateCAS(storageKey: String): T? {
        log.debug { "get for update cas. storageKey=$storageKey" }
        return cache[storageKey]
    }

    /**
     * 지정한 키의 캐시 엔트리를 삭제합니다.
     *
     * @param storageKey 삭제할 캐시 키
     */
    override fun delete(storageKey: String) {
        log.debug { "delete cache. storageKey=$storageKey" }
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
        val resultMap = mutableMapOf<String, T>()
        storageKeys.forEach { key ->
            cache[key]?.let { resultMap[key] = it }
        }
        return resultMap
    }

    /**
     * CAS(Compare-And-Swap) 방식으로 캐시 엔트리를 업데이트합니다.
     * JCache의 [Cache.replace]를 사용해 원자적으로 교체합니다.
     *
     * @param storageKey 업데이트할 캐시 키
     * @param cas 비교할 기존 값
     * @param storageObject 교체할 새 값
     * @return 업데이트 성공 여부
     */
    override fun updateCAS(storageKey: String, cas: T, storageObject: T): Boolean {
        log.debug { "update cas. storageKey=$storageKey, cas=$cas, storageObject=$storageObject" }
        return cache.replace(storageKey, cas, storageObject)
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
        cache.put(storageKey, storageObject)
    }
}
