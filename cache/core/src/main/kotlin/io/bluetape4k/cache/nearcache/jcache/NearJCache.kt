package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCacheEntryEventListener
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.trace
import io.bluetape4k.support.asyncRunWithTimeout
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import javax.cache.configuration.MutableCacheEntryListenerConfiguration
import kotlin.concurrent.withLock

/**
 * 분산 환경에서 로컬 캐시(Front Cache)와 원격 캐시(Back Cache)를 함께 사용하는 2-Tier 캐시 구현체입니다.
 *
 * NearCache는 다음과 같은 특징을 가집니다:
 * - **빠른 읽기**: 로컬 캐시(Front)에서 먼저 조회하여 네트워크 비용을 절감
 * - **데이터 일관성**: Back Cache의 변경 이벤트를 수신하여 Front Cache를 동기화
 * - **유연한 동기화**: 동기/비동기 모드 지원
 * - **자동 만료 감지**: 백그라운드 스레드로 Back Cache 만료 감지 및 Front Cache 갱신
 *
 * ```kotlin
 * // Redis를 Back Cache로 사용하는 NearCache 생성
 * val nearCache = NearCache(
 *     nearCacheCfg = NearCacheConfig(
 *         frontCacheName = "my-local-cache",
 *         isSynchronous = false  // 비동기 모드
 *     ),
 *     backCache = redisCache
 * )
 *
 * // 사용
 * nearCache.put("key", value)      // Front와 Back에 동시 저장
 * val value = nearCache.get("key") // Front에서 먼저 조회
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property frontCache 로컬 캐시 (예: Caffeine, Ehcache)
 * @property backCache 원격 캐시 (예: Redis, Hazelcast)
 * @property config NearCache 설정
 *
 * @see NearJCacheConfig
 * @see io.bluetape4k.cache.jcache.JCacheEntryEventListener
 */
class NearJCache<K: Any, V: Any>(
    val frontCache: JCache<K, V>,
    val backCache: JCache<K, V>,
    private val config: NearJCacheConfig<K, V>,
): JCache<K, V> by backCache {
    companion object: KLogging() {
        /** Redis SCAN 명령의 배치 크기 */
        const val SCAN_BATCH_SIZE = 100L

        /** 기본 원격 캐시 동기화 타임아웃 (500ms) */
        val DEFAULT_SYNC_REMOTE_TIMEOUT: Duration = Duration.ofMillis(500)

        /**
         * NearCache 인스턴스를 생성합니다.
         *
         * @param K 캐시 키 타입
         * @param V 캐시 값 타입
         * @param nearCacheCfg Front Cache 생성을 위한 설정
         * @param backCache 분산 환경에서 사용할 원격 캐시 인스턴스
         * @return [NearJCache] 인스턴스
         */
        operator fun <K: Any, V: Any> invoke(
            nearCacheCfg: NearJCacheConfig<K, V>,
            backCache: JCache<K, V>,
        ): NearJCache<K, V> {
            val frontCacheManager = nearCacheCfg.cacheManagerFactory.create()

            // back cache의 event를 수신하여 반영할 front cache 생성
            log.info { "front cache 생성. name=${nearCacheCfg.cacheName}" }
            val frontCache =
                frontCacheManager.createCache(nearCacheCfg.cacheName, nearCacheCfg.frontCacheConfiguration)

            // back cache의 event를 받아 front cache에 반영합니다.
            val jCacheEntryEventListenerCfg =
                MutableCacheEntryListenerConfiguration(
                    { JCacheEntryEventListener(frontCache) },
                    null,
                    false,
                    nearCacheCfg.isSynchronous
                )
            log.info { "back cache의 이벤트를 수신할 수 있도록 listener 등록. listenerCfg=$jCacheEntryEventListenerCfg" }
            backCache.registerCacheEntryListener(jCacheEntryEventListenerCfg)

            log.info { "Create NearCache instance. config=$nearCacheCfg" }
            return NearJCache(frontCache, backCache, nearCacheCfg)
        }
    }

    private val lock = ReentrantLock()

    override fun iterator(): MutableIterator<Cache.Entry<K, V>> = frontCache.iterator()

    override fun clear() {
        log.debug { "Near Cache의 Front cache를 Clear합니다." }
        runCatching { frontCache.clear() }
    }

    /**
     * Front Cache와 Back Cache 모두 비웁니다.
     *
     * 단, Back Cache를 공유한 다른 NearCache 인스턴스에는 전파되지 않습니다.
     * 전파가 필요한 경우 `removeAll()`을 사용하세요.
     *
     * ```kotlin
     * val nearCache = NearJCache(frontCache, backCache, config)
     * nearCache.put("hello", 5)
     * nearCache.clearAllCache()
     * val value = nearCache.getDeeply("hello")
     * // value == null
     * ```
     */
    fun clearAllCache() {
        log.debug {
            "front cache, back cache 모두 clear 합니다. 단 back cache 를 공유한 다른 near cache에는 전파되지 않습니다. " +
                    "전파를 위해서는 removeAll을 사용하세요"
        }
        runCatching { frontCache.clear() }
        runCatching { backCache.clear() }
    }

    override fun close() {
        lock.withLock {
            log.debug { "Near Cache 의 Front Cache를 Close 합니다." }
            runCatching {
                frontCache.close()
            }
        }
    }

    override fun isClosed(): Boolean = frontCache.isClosed

    // clear()는 front만 비우므로 containsKey()도 front만 확인하는 것이 의도된 동작
    override fun containsKey(key: K): Boolean = frontCache.containsKey(key)

    override operator fun get(key: K): V? { // 모든 조회는 Front 에서만 한다
        return frontCache.get(key)
    }

    /**
     * Front Cache에서 값을 우선 조회하고, 없으면 Back Cache까지 조회합니다.
     *
     * Back Cache에서 값을 찾은 경우 Front Cache에 채워 넣어 이후 조회를 빠르게 처리합니다.
     *
     * ```kotlin
     * val nearCache = NearJCache(frontCache, backCache, config)
     * nearCache.put("hello", 5)
     * nearCache.clear()  // front만 비움
     * val value = nearCache.getDeeply("hello")
     * // value == 5  (back cache에서 조회 후 front에 채워 넣음)
     * ```
     *
     * @param key 조회할 캐시 키
     * @return 조회된 값, 없으면 `null`
     */
    fun getDeeply(key: K): V? =
        frontCache.get(key)
            ?: backCache.get(key)?.also { value ->
                runCatching { frontCache.put(key, value) }
            }

    fun getAll(vararg keys: K): MutableMap<K, V> = getAll(keys.toSet())

    override fun getAll(keys: Set<K>): MutableMap<K, V> { // 모든 조회는 Front 에서만 한다
        return frontCache.getAll(keys)
    }

    override fun getAndRemove(key: K): V? {
        if (containsKey(key)) {
            val oldValue = get(key)
            remove(key)
            return oldValue
        }
        return null
    }

    override fun getAndReplace(key: K, value: V): V? {
        log.trace { "get and replace. key=$key" }
        if (containsKey(key)) {
            log.trace { "get entry, and put new value. key=$key, new value=$value" }
            val oldValue = get(key)
            put(key, value)
            return oldValue
        }
        return null
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    override fun put(key: K, value: V) {
        frontCache.put(key, value).apply {
            syncBackCache {
                backCache.put(key, value)
            }
        }
    }

    override fun putAll(map: Map<out K, V>) {
        frontCache.putAll(map).apply {
            syncBackCache {
                backCache.putAll(map)
            }
        }
    }

    override fun putIfAbsent(key: K, value: V): Boolean =
        frontCache.putIfAbsent(key, value).also {
            if (it) {
                syncBackCache {
                    if (!backCache.containsKey(key)) {
                        backCache.put(key, value)
                    }
                }
            }
        }

    override fun remove(key: K): Boolean =
        frontCache.remove(key).also {
            if (it) {
                syncBackCache {
                    backCache.remove(key)
                }
            }
        }

    override fun remove(key: K, oldValue: V): Boolean =
        frontCache.remove(key, oldValue).also {
            if (it) {
                syncBackCache {
                    // TODO: 왜  backCache.remove(key, oldValue) 를 직접 사용하지 않았는지 이유를 기록해야 한다
                    // NOTE: 아마 remove(key, oldValue) 는 event 를 발생시키지 않아서 직접 remove를 수행하도록 하는 걸로 추측한다
                    if (backCache.containsKey(key) && backCache.get(key) == oldValue) {
                        backCache.remove(key)
                    }
                }
            }
        }

    override fun removeAll() {
        frontCache.removeAll().apply {
            syncBackCache {
                // Redisson 에서는 bulk operation 의 경우 event 가 발생하지 않습니다!!!
                backCache.chunked(100) { chunk ->
                    chunk.forEach { runCatching { backCache.remove(it.key) } }
                    // Thread.sleep(1)
                }
            }
        }
    }

    override fun removeAll(keys: Set<K>) {
        frontCache.removeAll(keys).apply {
            syncBackCache {
                // Redisson 에서는 bulk operation 의 경우 event 가 발생하지 않습니다!!!
                keys.forEach { runCatching { backCache.remove(it) } }
            }
        }
    }

    /**
     * 여러 키를 vararg 형식으로 일괄 삭제합니다.
     *
     * ```kotlin
     * val nearCache = NearJCache(frontCache, backCache, config)
     * nearCache.put("key1", 1)
     * nearCache.put("key2", 2)
     * nearCache.removeAll("key1", "key2")
     * val v1 = nearCache.getDeeply("key1")
     * // v1 == null
     * ```
     */
    fun removeAll(vararg keys: K) {
        removeAll(keys.toSet())
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean =
        frontCache.replace(key, oldValue, newValue).also {
            if (it) {
                syncBackCache {
                    if (backCache.containsKey(key) && backCache.get(key) == oldValue) {
                        backCache.put(key, newValue)
                    }
                }
            }
        }

    override fun replace(key: K, value: V): Boolean =
        frontCache.replace(key, value).also {
            if (it) {
                syncBackCache {
                    // Redisson 에서는 replace 가 event 를 발생시키지 않습니다.
                    if (backCache.containsKey(key)) {
                        backCache.put(key, value)
                    }
                }
            }
        }

    override fun <T: Any> unwrap(clazz: Class<T>): T? {
        if (clazz.isAssignableFrom(javaClass)) {
            return clazz.cast(this)
        }
        return null
    }

    private inline fun syncBackCache(crossinline syncTask: () -> Unit) {
        if (config.isSynchronous) {
            runCatching { syncTask() }
        } else {
            val timeoutMillis = config.syncRemoteTimeout.coerceAtLeast(NearJCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT)
            asyncRunWithTimeout(timeoutMillis) {
                runCatching { syncTask() }
            }
        }
    }
}
