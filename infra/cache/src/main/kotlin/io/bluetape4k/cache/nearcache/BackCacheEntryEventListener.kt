package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryExpiredListener
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener


/**
 * Back cache에서 entry 변화가 발생하면, event를 발행하고, 이를 [targetCache]에 반영하도록 하는 Listener 입니다.
 *
 * @property targetCache [CacheEntryEvent]가 반영될 Local Cache
 */
class BackCacheEntryEventListener<K, V>(
    private val targetCache: JCache<K, V>,
): CacheEntryCreatedListener<K, V>,
   CacheEntryUpdatedListener<K, V>,
   CacheEntryRemovedListener<K, V>,
   CacheEntryExpiredListener<K, V> {

    companion object: KLogging()

    override fun onCreated(events: Iterable<CacheEntryEvent<out K, out V>>) {
        log.trace {
            "Back cache entry is created. targetCache=${targetCache.name}, events=${events.joinToString { it.asText() }}"
        }
        if (!targetCache.isClosed) {
            runCatching {
                targetCache.putAll(events.associate { it.key to it.value })
            }.onFailure { e ->
                log.error(e) { "Fail to put all created cache entries." }
            }
        }
    }

    override fun onUpdated(events: Iterable<CacheEntryEvent<out K, out V>>) {
        log.trace {
            "Back cache entry is updated. targetCache=${targetCache.name}, events=${events.joinToString { it.asText() }}"
        }
        if (!targetCache.isClosed) {
            runCatching {
                targetCache.putAll(events.associate { it.key to it.value })
            }.onFailure { e ->
                log.error(e) { "Fail to put all updated cache entries." }
            }
        }
    }

    override fun onRemoved(events: Iterable<CacheEntryEvent<out K, out V>>) {
        log.trace {
            "Back cache entry is removed. targetCache=${targetCache.name}, events=${events.joinToString { it.asText() }}"
        }
        if (!targetCache.isClosed) {
            runCatching {
                targetCache.removeAll(events.map { it.key }.toSet())
            }.onFailure { e ->
                log.error(e) { "Fail to remove all removed cache entries." }
            }
        }
    }

    override fun onExpired(events: Iterable<CacheEntryEvent<out K, out V>>) {
        log.trace {
            "Back cache entry is expired. targetCache=${targetCache.name}, events=${events.joinToString { it.asText() }}"
        }
        if (!targetCache.isClosed) {
            runCatching {
                targetCache.removeAll(events.map { it.key }.toSet())
            }.onFailure { e ->
                log.error(e) { "Fail to remove all expired cache entries." }
            }
        }
    }

    private fun <K, V> CacheEntryEvent<K, V>.asText(): String =
        "source=$source, key=$key, value=$value"
}
