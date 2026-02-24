package io.bluetape4k.ignite.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.apache.ignite.client.IgniteClient
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [IgniteClientNearCache]와 [IgniteClientSuspendNearCache]를 생성하고 생명주기를 관리하는 Manager 클래스입니다.
 *
 * 동일한 이름의 캐시를 중복 생성하지 않고 재사용하는 getOrCreate 시멘틱을 제공하며,
 * [close] 호출 시 관리 중인 모든 캐시의 Front Cache(Caffeine)를 정리합니다.
 * Back Cache(Ignite)는 건드리지 않습니다.
 *
 * ```kotlin
 * val manager = igniteClient.nearCacheManager()
 *
 * val config = IgniteNearCacheConfig(cacheName = "ORDERS")
 * val cache = manager.nearCache<String, Order>(config)
 * val suspendCache = manager.suspendNearCache<String, Order>(config)
 *
 * manager.close()
 * ```
 *
 * @property client Ignite 2.x 씬 클라이언트 인스턴스
 */
class IgniteClientNearCacheManager(val client: IgniteClient): Closeable {

    companion object: KLogging()

    private val caches = ConcurrentHashMap<String, IgniteClientNearCache<*, *>>()
    private val suspendCaches = ConcurrentHashMap<String, IgniteClientSuspendNearCache<*, *>>()
    private val closed = AtomicBoolean(false)

    /** 관리 중인 모든 캐시 이름 목록 (sync + suspend 캐시 합산) */
    val cacheNames: Set<String>
        get() = caches.keys + suspendCaches.keys

    /** Manager가 닫힌 상태인지 여부 */
    val isClosed: Boolean get() = closed.get()

    /**
     * closed 상태에서 작업을 수행하려 할 때 [IllegalStateException]을 발생시킵니다.
     */
    private fun checkNotClosed() {
        check(!closed.get()) { "IgniteClientNearCacheManager가 이미 닫혀 있습니다." }
    }

    /**
     * [IgniteClientNearCache]를 반환합니다.
     *
     * 동일한 [IgniteNearCacheConfig.cacheName]의 캐시가 이미 존재하면 재사용합니다.
     *
     * @param K 캐시 키 타입
     * @param V 캐시 값 타입
     * @param config Near Cache 설정
     * @return [IgniteClientNearCache] 인스턴스
     */
    @Suppress("UNCHECKED_CAST")
    fun <K: Any, V: Any> nearCache(config: IgniteNearCacheConfig): IgniteClientNearCache<K, V> {
        checkNotClosed()
        return caches.getOrPut(config.cacheName) {
            log.debug { "IgniteClientNearCache 생성. cacheName=${config.cacheName}" }
            IgniteClientNearCache<Any, Any>(client, config)
        } as IgniteClientNearCache<K, V>
    }

    /**
     * [IgniteClientSuspendNearCache]를 반환합니다.
     *
     * 동일한 [IgniteNearCacheConfig.cacheName]의 캐시가 이미 존재하면 재사용합니다.
     *
     * @param K 캐시 키 타입
     * @param V 캐시 값 타입
     * @param config Near Cache 설정
     * @return [IgniteClientSuspendNearCache] 인스턴스
     */
    @Suppress("UNCHECKED_CAST")
    fun <K: Any, V: Any> suspendNearCache(config: IgniteNearCacheConfig): IgniteClientSuspendNearCache<K, V> {
        checkNotClosed()
        return suspendCaches.getOrPut(config.cacheName) {
            log.debug { "IgniteClientSuspendNearCache 생성. cacheName=${config.cacheName}" }
            IgniteClientSuspendNearCache<Any, Any>(client, config)
        } as IgniteClientSuspendNearCache<K, V>
    }

    /**
     * 지정한 이름의 캐시를 레지스트리에서 제거하고 Front Cache(Caffeine)를 초기화합니다.
     *
     * Back Cache(Ignite)는 건드리지 않습니다.
     *
     * @param cacheName 제거할 캐시 이름
     */
    fun destroyCache(cacheName: String) {
        caches.remove(cacheName)?.let { cache ->
            log.debug { "IgniteClientNearCache Front Cache 초기화. cacheName=$cacheName" }
            runCatching { cache.clearFrontCache() }
                .onFailure { log.warn(it) { "IgniteClientNearCache Front Cache 초기화 중 오류 발생. cacheName=$cacheName" } }
        }
        suspendCaches.remove(cacheName)?.let { cache ->
            log.debug { "IgniteClientSuspendNearCache Front Cache 초기화. cacheName=$cacheName" }
            runCatching { cache.clearFrontCache() }
                .onFailure { log.warn(it) { "IgniteClientSuspendNearCache Front Cache 초기화 중 오류 발생. cacheName=$cacheName" } }
        }
    }

    /**
     * 관리 중인 모든 캐시의 Front Cache(Caffeine)를 초기화하고 Manager를 닫습니다.
     *
     * Back Cache(Ignite)는 건드리지 않습니다.
     * 레지스트리를 비우고 closed 상태로 전환합니다.
     */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            log.debug { "IgniteClientNearCacheManager 종료. caches=${caches.size}, suspendCaches=${suspendCaches.size}" }
            caches.values.forEach { cache ->
                runCatching { cache.clearFrontCache() }
                    .onFailure { log.warn(it) { "IgniteClientNearCache Front Cache 초기화 중 오류 발생." } }
            }
            caches.clear()

            suspendCaches.values.forEach { cache ->
                runCatching { cache.clearFrontCache() }
                    .onFailure { log.warn(it) { "IgniteClientSuspendNearCache Front Cache 초기화 중 오류 발생." } }
            }
            suspendCaches.clear()
        }
    }
}

/**
 * [IgniteClient]에서 [IgniteClientNearCacheManager]를 생성합니다.
 *
 * ```kotlin
 * val manager = igniteClient.nearCacheManager()
 * val cache = manager.nearCache<String, Order>(IgniteNearCacheConfig(cacheName = "ORDERS"))
 * ```
 *
 * @return [IgniteClientNearCacheManager] 인스턴스
 */
fun IgniteClient.nearCacheManager(): IgniteClientNearCacheManager = IgniteClientNearCacheManager(this)
