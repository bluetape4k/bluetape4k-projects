package io.bluetape4k.hazelcast.cache

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.hazelcast.cache.coroutines.HazelcastSuspendNearCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.atomicfu.atomic
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

/**
 * [HazelcastNearCache]를 생성하고 생명주기를 관리하는 Manager 클래스입니다.
 *
 * 동일한 이름의 캐시를 중복 생성하지 않고 재사용하는 getOrCreate 시멘틱을 제공하며,
 * [close] 호출 시 관리 중인 모든 캐시를 정리합니다.
 *
 * ```kotlin
 * val manager = hazelcastClient.nearCacheManager()
 *
 * val config = HazelcastNearCacheConfig("orders")
 * val cache = manager.nearCache<String, Order>(config)
 *
 * manager.close()
 * ```
 *
 * @property client Hazelcast 클라이언트 인스턴스
 */
class HazelcastNearCacheManager(val client: HazelcastInstance): Closeable {

    companion object: KLogging()

    private val caches = ConcurrentHashMap<String, HazelcastNearCache<*, *>>()
    private val closed = atomic(false)

    /** 관리 중인 모든 캐시 이름 목록 (sync + suspend 캐시 합산) */
    val cacheNames: Set<String> get() = caches.keys

    /** Manager가 닫힌 상태인지 여부 */
    val isClosed: Boolean by closed

    /**
     * closed 상태에서 작업을 수행하려 할 때 [IllegalStateException]을 발생시킵니다.
     */
    private fun checkNotClosed() {
        check(!isClosed) { "HazelcastNearCacheManager가 이미 닫혀 있습니다." }
    }

    /**
     * [HazelcastNearCache]를 반환합니다.
     *
     * 동일한 [HazelcastNearCacheConfig.cacheName]의 캐시가 이미 존재하면 재사용합니다.
     *
     * @param K 캐시 키 타입
     * @param V 캐시 값 타입
     * @param config Near Cache 설정
     * @return [HazelcastNearCache] 인스턴스
     */
    @Suppress("UNCHECKED_CAST")
    fun <K: Any, V: Any> nearCache(config: HazelcastNearCacheConfig): HazelcastNearCache<K, V> {
        checkNotClosed()
        return caches.getOrPut(config.cacheName) {
            log.debug { "HazelcastNearCache 생성. mapName=${config.cacheName}" }
            val map = client.getMap<K, V>(config.cacheName)
            HazelcastNearCache(map)
        } as HazelcastNearCache<K, V>
    }

    /**
     * 지정한 이름의 캐시를 소멸시키고 레지스트리에서 제거합니다.
     *
     * sync 캐시는 [HazelcastNearCache.destroy]로 IMap 리소스를 해제하고,
     * suspend 캐시는 [HazelcastSuspendNearCache.clear]로 Front Cache를 초기화합니다.
     *
     * @param cacheName 제거할 캐시의 IMap 이름
     */
    fun destroyCache(cacheName: String) {
        caches.remove(cacheName)?.let { cache ->
            log.debug { "HazelcastNearCache 소멸. cacheName=$cacheName" }
            runCatching { cache.destroy() }
                .onFailure { log.warn(it) { "HazelcastNearCache 소멸 중 오류 발생. cacheName=$cacheName" } }
        }
    }

    /**
     * 관리 중인 모든 캐시를 정리하고 Manager를 닫습니다.
     *
     * sync 캐시는 [HazelcastNearCache.destroy], suspend 캐시는 [HazelcastSuspendNearCache.clear]를 호출한 뒤
     * 레지스트리를 비우고 closed 상태로 전환합니다.
     */
    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            log.debug { "HazelcastNearCacheManager 종료. caches=${caches.size}" }
            caches.values.forEach { cache ->
                runCatching { cache.destroy() }
                    .onFailure { log.warn(it) { "HazelcastNearCache 소멸 중 오류 발생." } }
            }
            caches.clear()
        }
    }
}

/**
 * [HazelcastInstance]에서 [HazelcastNearCacheManager]를 생성합니다.
 *
 * ```kotlin
 * val manager = hazelcastClient.nearCacheManager()
 * val cache = manager.nearCache<String, Order>(HazelcastNearCacheConfig("orders"))
 * ```
 *
 * @return [HazelcastNearCacheManager] 인스턴스
 */
fun HazelcastInstance.nearCacheManager(): HazelcastNearCacheManager = HazelcastNearCacheManager(this)
