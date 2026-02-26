package io.bluetape4k.hazelcast.cache.coroutines

import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.hazelcast.cache.HazelcastNearCacheConfig
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.atomicfu.atomic
import java.io.Closeable
import java.util.concurrent.ConcurrentHashMap

class HazelcastSuspendNearCacheManager(val client: HazelcastInstance): Closeable {

    companion object: KLoggingChannel()

    private val caches = ConcurrentHashMap<String, HazelcastSuspendNearCache<*, *>>()
    private val closed = atomic(false)

    val cacheNames: Set<String>
        get() = caches.keys

    val isClosed: Boolean by closed

    private fun checkNotClosed() {
        check(!isClosed) { "HazelcastSuspendNearCacheManager가 이미 닫혀 있습니다." }
    }

    /**
     * [HazelcastSuspendNearCache]를 반환합니다.
     *
     * 동일한 [HazelcastNearCacheConfig.cacheName]의 캐시가 이미 존재하면 재사용합니다.
     *
     * @param K 캐시 키 타입
     * @param V 캐시 값 타입
     * @param config Near Cache 설정
     * @return [HazelcastSuspendNearCache] 인스턴스
     */
    @Suppress("UNCHECKED_CAST")
    fun <K: Any, V: Any> nearCache(config: HazelcastNearCacheConfig): HazelcastSuspendNearCache<K, V> {
        checkNotClosed()
        return caches.computeIfAbsent(config.cacheName) {
            log.debug { "HazelcastSuspendNearCache 생성. cacheName=${config.cacheName}" }
            val map = client.getMap<K, V>(config.cacheName)
            HazelcastSuspendNearCache(map, config)
        } as HazelcastSuspendNearCache<K, V>
    }

    fun destroyCache(cacheName: String) {
        caches.remove(cacheName)?.let { cache ->
            log.debug { "HazelcastSuspendNearCache Front Cache 초기화. cacheName=$cacheName" }
            runCatching { cache.clear() }
                .onFailure {
                    log.warn(it) { "HazelcastSuspendNearCache Clear 중 오류 발생. cacheName=$cacheName" }
                }
        }
    }

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            log.debug { "HazelcastSuspendNearCacheManager 종료. caches=${caches.size}" }

            caches.values.forEach { cache ->
                runCatching { cache.clear() }
                    .onFailure {
                        log.warn(it) { "HazelcastSuspendNearCache Clear 중 오류 발생. cacheName=${cache.name}" }
                    }
            }
            caches.clear()
        }
    }
}

fun HazelcastInstance.suspendNearCacheManager(): HazelcastSuspendNearCacheManager =
    HazelcastSuspendNearCacheManager(this)
