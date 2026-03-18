package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryExpiredListener
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener

/**
 * Back Cache의 엔트리 이벤트(생성/수정/삭제/만료)를 수신하여 Front Cache([targetCache])에 반영하는 리스너입니다.
 *
 * JCache 이벤트 콜백은 동기식으로 호출되므로, `runBlocking` 대신 전용 [kotlinx.coroutines.CoroutineScope]에서
 * `launch`를 사용하여 스레드 풀 고갈과 데드락을 방지합니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property targetCache 이벤트를 반영할 Front Cache
 */
class SuspendJCacheEntryEventListener<K: Any, V: Any>(
    private val targetCache: SuspendJCache<K, V>,
): CacheEntryCreatedListener<K, V>,
   CacheEntryUpdatedListener<K, V>,
   CacheEntryRemovedListener<K, V>,
   CacheEntryExpiredListener<K, V> {

    companion object: KLoggingChannel()

    // JCache 이벤트 스레드를 블로킹하지 않기 위해 전용 코루틴 스코프 사용
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Called after one or more entries have been created.
     *
     * @param events The entries just created.
     * @throws CacheEntryListenerException if there is problem executing the listener
     */
    override fun onCreated(events: MutableIterable<CacheEntryEvent<out K, out V>>) {
        log.trace { "BackCache cache entry created. events=${events.joinToString { it.asText() }}" }
        if (!targetCache.isClosed()) {
            scope.launch {
                runCatching {
                    targetCache.putAll(events.associate { it.key to it.value })
                }.onFailure { e ->
                    log.error(e) { "Fail to put all created cache entries." }
                }
            }
        }
    }

    /**
     * Called after one or more entries have been updated.
     *
     * @param events The entries just updated.
     * @throws CacheEntryListenerException if there is problem executing the listener
     */
    override fun onUpdated(events: MutableIterable<CacheEntryEvent<out K, out V>>) {
        log.trace { "BackCache cache entry updated. events=${events.joinToString { it.asText() }}" }
        if (!targetCache.isClosed()) {
            scope.launch {
                runCatching {
                    targetCache.putAll(events.associate { it.key to it.value })
                }.onFailure { e ->
                    log.error(e) { "Fail to put all updated cache entries." }
                }
            }
        }
    }

    /**
     * Called after one or more entries have been removed. If no entry existed for
     * a key an event is not raised for it.
     *
     * @param events The entries just removed.
     * @throws CacheEntryListenerException if there is problem executing the listener
     */
    override fun onRemoved(events: MutableIterable<CacheEntryEvent<out K, out V>>) {
        log.trace { "BackCache cache entry removed. events=${events.joinToString { it.asText() }}" }
        if (!targetCache.isClosed()) {
            scope.launch {
                runCatching {
                    targetCache.removeAll(events.map { it.key }.toSet())
                }.onFailure { e ->
                    log.error(e) { "Fail to remove all removed cache entries." }
                }
            }
        }
    }

    /**
     * Called after one or more entries have been expired by the cache. This is not
     * necessarily when an entry is expired, but when the cache detects the expiry.
     *
     * @param events The entries just removed.
     * @throws CacheEntryListenerException if there is problem executing the listener
     */
    override fun onExpired(events: MutableIterable<CacheEntryEvent<out K, out V>>) {
        log.trace { "BackCache cache entry expired. events=${events.joinToString { it.asText() }}" }
        if (!targetCache.isClosed()) {
            scope.launch {
                runCatching {
                    targetCache.removeAll(events.map { it.key }.toSet())
                }.onFailure { e ->
                    log.error(e) { "Fail to remove all expired cache entries." }
                }
            }
        }
    }

    private fun <K, V> CacheEntryEvent<K, V>.asText(): String =
        "source=$source, key=$key, value=$value"
}
