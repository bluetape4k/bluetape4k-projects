package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.runs
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener
import javax.cache.event.EventType

class NearJCacheWriteThroughReentrancyTest {

    @Test
    fun `동기 CRUD write-through의 inline listener 재진입이 교착되지 않는다`() {
        runInlineListenerOperation(
            operation = "put",
            configureBack = { backCache, fire ->
                every { backCache.put("key", "value") } answers {
                    fire(EventType.CREATED)
                }
            },
            invoke = { it.put("key", "value") },
        )
        runInlineListenerOperation(
            operation = "putAll",
            configureBack = { backCache, fire ->
                every { backCache.putAll(mapOf("key" to "value")) } answers {
                    fire(EventType.CREATED)
                }
            },
            invoke = { it.putAll(mapOf("key" to "value")) },
        )
        runInlineListenerOperation(
            operation = "putIfAbsent",
            configureFront = { frontCache ->
                every { frontCache.putIfAbsent("key", "value") } returns true
            },
            configureBack = { backCache, fire ->
                every { backCache.containsKey("key") } returns false
                every { backCache.put("key", "value") } answers {
                    fire(EventType.CREATED)
                }
            },
            invoke = { it.putIfAbsent("key", "value") },
        )
        runInlineListenerOperation(
            operation = "remove",
            configureFront = { frontCache ->
                every { frontCache.remove("key") } returns true
            },
            configureBack = { backCache, fire ->
                every { backCache.remove("key") } answers {
                    fire(EventType.REMOVED)
                    true
                }
            },
            invoke = { it.remove("key") },
        )
        runInlineListenerOperation(
            operation = "replace",
            configureFront = { frontCache ->
                every { frontCache.replace("key", "value") } returns true
            },
            configureBack = { backCache, fire ->
                every { backCache.containsKey("key") } returns true
                every { backCache.put("key", "value") } answers {
                    fire(EventType.UPDATED)
                }
            },
            invoke = { it.replace("key", "value") },
        )
    }

    private fun runInlineListenerOperation(
        operation: String,
        configureFront: (JCache<String, String>) -> Unit = {},
        configureBack: (JCache<String, String>, (EventType) -> Unit) -> Unit,
        invoke: (NearJCache<String, String>) -> Unit,
    ) {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val listenerConfiguration = slot<CacheEntryListenerConfiguration<String, String>>()
        val event = mockk<CacheEntryEvent<String, String>>(relaxed = true)
        every { event.key } returns "key"
        every { event.value } returns "value"
        every { backCache.registerCacheEntryListener(capture(listenerConfiguration)) } just runs
        configureFront(frontCache)
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true, syncRemoteTimeout = 1L),
        )
        nearCache.registerBackCacheListener()
        configureBack(backCache) { eventType ->
            val listener = listenerConfiguration.captured.cacheEntryListenerFactory!!.create()
            when (eventType) {
                EventType.CREATED ->
                    (listener as CacheEntryCreatedListener<String, String>).onCreated(listOf(event))
                EventType.UPDATED ->
                    (listener as CacheEntryUpdatedListener<String, String>).onUpdated(listOf(event))
                EventType.REMOVED ->
                    (listener as CacheEntryRemovedListener<String, String>).onRemoved(listOf(event))
                EventType.EXPIRED -> error("unexpected event type")
            }
        }

        val failure = AtomicReference<Throwable?>()
        val finished = CountDownLatch(1)
        val worker = virtualThread(start = false, name = "near-jcache-reentrancy-$operation") {
            try {
                invoke(nearCache)
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                finished.countDown()
            }
        }
        try {
            worker.start()
            check(finished.await(2, TimeUnit.SECONDS)) {
                "synchronous $operation did not complete; inline listener likely re-entered mutationGate"
            }
            failure.get()?.let { throw AssertionError("synchronous $operation failed", it) }
        } finally {
            if (worker.isAlive) {
                worker.interrupt()
                worker.join(2_000)
            }
            nearCache.close()
        }
    }
}
