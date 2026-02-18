package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.trace
import io.bluetape4k.logging.warn
import io.bluetape4k.support.asyncRunWithTimeout
import java.time.Duration
import java.util.concurrent.locks.ReentrantLock
import javax.cache.Cache
import javax.cache.configuration.MutableCacheEntryListenerConfiguration
import kotlin.concurrent.withLock
import kotlin.system.measureTimeMillis


/**
 * 분산환경의 원격 캐시만 사용하는 게 아니라, 로컬 캐시를 두어, 빠르게 access 할 수 있도록 합니다.
 *
 * @property frontCache  로컬 캐시
 * @property backCache   분산 환경에서 사용할 원격 캐시
 */
class NearCache<K: Any, V: Any> private constructor(
    val frontCache: JCache<K, V>,
    val backCache: JCache<K, V>,
    private val config: NearCacheConfig<K, V>,
): JCache<K, V> by backCache {

    companion object: KLogging() {
        val DEFAULT_SYNC_REMOTE_TIMEOUT: Duration = Duration.ofMillis(500)

        /**
         * 분산환경의 원격 캐시만 사용하는 게 아니라, 로컬 캐시를 두어, 빠르게 access 할 수 있도록 합니다.
         *
         * @param nearCacheCfg front cache 생성을 위한 configuration
         * @param backCache 분산환경에서의 원격 cache instance
         * @return [NearCache] 인스턴스
         */
        operator fun <K: Any, V: Any> invoke(
            nearCacheCfg: NearCacheConfig<K, V>,
            backCache: JCache<K, V>,
        ): NearCache<K, V> {
            val frontCacheManager = nearCacheCfg.cacheManagerFactory.create()

            // back cache의 event를 수신하여 반영할 front cache 생성
            log.info { "front cache 생성. name=${nearCacheCfg.frontCacheName}" }
            val frontCache =
                frontCacheManager.createCache(nearCacheCfg.frontCacheName, nearCacheCfg.frontCacheConfiguration)

            // back cache의 event를 받아 front cache에 반영합니다.
            val cacheEntryEventListenerCfg = MutableCacheEntryListenerConfiguration(
                { BackCacheEntryEventListener(frontCache) }, null, false, nearCacheCfg.isSynchronous
            )
            log.info { "back cache의 이벤트를 수신할 수 있도록 listener 등록. listenerCfg=$cacheEntryEventListenerCfg" }
            backCache.registerCacheEntryListener(cacheEntryEventListenerCfg)

            log.info { "Create NearCache instance. config=$nearCacheCfg" }
            return NearCache(frontCache, backCache, nearCacheCfg)
        }
    }

    private var thread: Thread? = null
    private val lock = ReentrantLock()

    init {
        if (config.checkExpiryPeriod >= NearCacheConfig.MIN_EXPIRY_CHECK_PERIOD) {
            thread = checkBackCacheExpiration()
        }
    }

    private fun checkBackCacheExpiration(): Thread {
        return Thread.ofVirtual().name("nearcache-expiration-check").start {
            try {
                Thread.sleep(config.checkExpiryPeriod)
                while (!isClosed && !Thread.currentThread().isInterrupted) {
                    log.trace { "backCache의 cache entry가 expire 되었는지 검사합니다... check expiration period=${config.checkExpiryPeriod}" }
                    var entrySize = 0
                    val elapsed = measureTimeMillis {
                        runCatching {
                            this.chunked(100) { entries ->
                                if (isClosed || Thread.currentThread().isInterrupted) {
                                    return@chunked
                                }
                                val frontKeys = entries.map { it.key }.toSet()
                                entrySize += frontKeys.size
                                log.trace { "Front Cache item 유효기간 조사=$entrySize" }
                                frontKeys.forEach {
                                    if (!backCache.containsKey(it)) {
                                        frontCache.remove(it)
                                    }
                                }
                                Thread.sleep(1)
                            }
                        }
                    }
                    log.trace { "backCache cache entry expire 검사 완료. front cache item size=$entrySize, elapsed=$elapsed msec" }
                    Thread.sleep(config.checkExpiryPeriod)
                }
                log.debug { "backCache epiration 검사를 종료합니다" }
            } catch (e: InterruptedException) {
                log.warn(e) { "backCache expiration 검사가 중단되었습니다" }
                // ignote InterruptedException
            }
        }
    }

    override fun iterator(): MutableIterator<Cache.Entry<K, V>> {
        return frontCache.iterator()
    }

    override fun clear() {
        log.debug { "Near Cache의 Front cache를 Clear합니다." }
        runCatching { frontCache.clear() }
    }

    fun clearAllCache() {
        log.debug {
            "front cache, back cache 모두 clear 합니다. 단 back cache 를 공유한 다른 near cache에는 전파되지 않습니다. " + "전파를 위해서는 removeAll을 사용하세요"
        }
        runCatching { frontCache.clear() }
        runCatching { backCache.clear() }
    }

    override fun close() {
        lock.withLock {
            log.debug { "Near Cache 의 Front Cache를 Close 합니다." }
            runCatching {
                frontCache.close()
                thread?.let {
                    if (it.isAlive) {
                        it.interrupt()
                    }
                }
            }
        }
    }

    override fun isClosed(): Boolean = frontCache.isClosed

    override fun containsKey(key: K): Boolean {
        return frontCache.containsKey(key)
    }

    override operator fun get(key: K): V? { // 모든 조회는 Front 에서만 한다
        return frontCache.get(key)
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

    override fun putIfAbsent(key: K, value: V): Boolean {
        return frontCache.putIfAbsent(key, value).also {
            if (it) {
                syncBackCache {
                    if (!backCache.containsKey(key)) {
                        backCache.put(key, value)
                    }
                }
            }
        }
    }

    override fun remove(key: K): Boolean {
        return frontCache.remove(key).also {
            if (it) {
                syncBackCache {
                    backCache.remove(key)
                }
            }
        }
    }

    override fun remove(key: K, oldValue: V): Boolean {
        return frontCache.remove(key, oldValue).also {
            if (it) {
                syncBackCache { // TODO: 왜  backCache.remove(key, oldValue) 를 직접 사용하지 않았는지 이유를 기록해야 한다
                    // NOTE: 아마 remove(key, oldValue) 는 event 를 발생시키지 않아서 직접 remove를 수행하도록 하는 걸로 추측한다
                    if (backCache.containsKey(key) && backCache.get(key) == oldValue) {
                        backCache.remove(key)
                    }
                }
            }
        }
    }

    override fun removeAll() {
        frontCache.removeAll().apply {
            syncBackCache { // Redisson 에서는 bulk operation 의 경우 event 가 발생하지 않습니다!!!
                backCache.chunked(100) { chunk ->
                    chunk.forEach { runCatching { backCache.remove(it.key) } }
                    Thread.sleep(1)
                }
            }
        }
    }

    override fun removeAll(keys: Set<K>) {
        frontCache.removeAll(keys).apply {
            syncBackCache { // Redisson 에서는 bulk operation 의 경우 event 가 발생하지 않습니다!!!
                keys.forEach { runCatching { backCache.remove(it) } }
            }
        }
    }

    fun removeAll(vararg keys: K) {
        removeAll(keys.toUnifiedSet())
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean {
        return frontCache.replace(key, oldValue, newValue).also {
            if (it) {
                syncBackCache {
                    if (backCache.containsKey(key) && backCache.get(key) == oldValue) {
                        backCache.put(key, newValue)
                    }
                }
            }
        }
    }

    override fun replace(key: K, value: V): Boolean {
        return frontCache.replace(key, value).also {
            if (it) {
                syncBackCache { // Redisson 에서는 replace 가 event 를 발생시키지 않습니다.
                    if (backCache.containsKey(key)) {
                        backCache.put(key, value)
                    }
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
            val timeoutMillis = config.syncRemoteTimeout.coerceAtLeast(NearCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT)
            asyncRunWithTimeout(timeoutMillis) {
                runCatching { syncTask() }
            }
        }
    }
}
