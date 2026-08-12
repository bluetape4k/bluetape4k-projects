package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import java.util.concurrent.CancellationException
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent
import javax.cache.event.EventType
import javax.cache.event.CacheEntryExpiredListener
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener

/**
 * Back cache에서 entry 변화가 발생하면, event를 발행하고, 이를 [targetCache]에 반영하도록 하는 Listener 입니다.
 *
 * ```kotlin
 * val frontCache: JCache<String, Int> = JCaching.Caffeine.getOrCreate("front")
 * val backCache: JCache<String, Int> = JCaching.Caffeine.getOrCreate("back")
 * val listener = JCacheEntryEventListener(frontCache)
 * val listenerCfg = MutableCacheEntryListenerConfiguration(
 *     { listener }, null, false, false
 * )
 * backCache.registerCacheEntryListener(listenerCfg)
 * backCache.put("hello", 5)
 * // frontCache에 "hello" -> 5 가 자동으로 동기화됨
 * ```
 *
 * @property targetCache [javax.cache.event.CacheEntryEvent]가 반영될 Local Cache
 * @property eventHandler 선택적으로 이벤트 적용 경계를 호출하는 핸들러
 */
class JCacheEntryEventListener<K, V>(
    private val targetCache: JCache<K, V>,
    private val eventHandler: ((EventType, List<CacheEntryEvent<out K, out V>>) -> Unit)? = null,
): CacheEntryCreatedListener<K, V>,
   CacheEntryUpdatedListener<K, V>,
   CacheEntryRemovedListener<K, V>,
   CacheEntryExpiredListener<K, V> {

    companion object: KLogging()

    override fun onCreated(events: Iterable<CacheEntryEvent<out K, out V>>) {
        handleEvents(EventType.CREATED, events) { eventList ->
            targetCache.putAll(eventList.associate { it.key to it.value })
        }
    }

    override fun onUpdated(events: Iterable<CacheEntryEvent<out K, out V>>) {
        handleEvents(EventType.UPDATED, events) { eventList ->
            targetCache.putAll(eventList.associate { it.key to it.value })
        }
    }

    override fun onRemoved(events: Iterable<CacheEntryEvent<out K, out V>>) {
        handleEvents(EventType.REMOVED, events) { eventList ->
            targetCache.removeAll(eventList.map { it.key }.toSet())
        }
    }

    override fun onExpired(events: Iterable<CacheEntryEvent<out K, out V>>) {
        handleEvents(EventType.EXPIRED, events) { eventList ->
            targetCache.removeAll(eventList.map { it.key }.toSet())
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleEvents(
        eventType: EventType,
        events: Iterable<CacheEntryEvent<out K, out V>>,
        defaultHandler: (List<CacheEntryEvent<out K, out V>>) -> Unit,
    ) {
        val eventList = events.toList()
        log.trace {
            "Back cache event received. type=$eventType, targetCache=${targetCache.name}, " +
                    "eventCount=${eventList.size}"
        }
        if (targetCache.isClosed) return

        try {
            eventHandler?.invoke(eventType, eventList) ?: defaultHandler(eventList)
        } catch (e: CancellationException) {
            throw e
        } catch (e: RuntimeException) {
            log.error(e) { "Failed to apply back cache event. type=$eventType, eventCount=${eventList.size}" }
        }
    }
}
