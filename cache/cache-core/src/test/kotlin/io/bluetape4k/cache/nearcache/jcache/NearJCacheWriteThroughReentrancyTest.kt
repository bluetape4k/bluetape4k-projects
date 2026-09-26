package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.concurrent.await
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener
import javax.cache.event.EventType
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class NearJCacheWriteThroughReentrancyTest {

    companion object: KLogging()

    @Test
    fun `동기 CRUD listener 재진입이 교착되지 않는다`() {
        testPut()
        testPutAll()
        testPutIfAbsent()
        testRemove()
        testReplace()
    }

    private fun testPut() {
        runInlineListenerOperation(
            operation = "put",
            configureBack = { backCache, fire ->
                every { backCache.put("key", "value") } answers {
                    fire(EventType.CREATED)
                }
            },
            invoke = { it.put("key", "value") },
            assertFrontReconciled = { frontCache ->
                verify { frontCache.putAll(mapOf("key" to "value")) }
            },
        )
    }

    private fun testPutAll() {
        runInlineListenerOperation(
            operation = "putAll",
            configureBack = { backCache, fire ->
                every { backCache.putAll(mapOf("key" to "value")) } answers {
                    fire(EventType.CREATED)
                }
            },
            invoke = { it.putAll(mapOf("key" to "value")) },
            assertFrontReconciled = { frontCache ->
                verify { frontCache.putAll(mapOf("key" to "value")) }
            },
        )
    }

    private fun testPutIfAbsent() {
        runInlineListenerOperation(
            operation = "putIfAbsent",
            configureBack = { backCache, fire ->
                every { backCache.putIfAbsent("key", "value") } answers {
                    fire(EventType.CREATED)
                    true
                }
            },
            invoke = { it.putIfAbsent("key", "value") },
            assertFrontReconciled = { frontCache ->
                verify { frontCache.putAll(mapOf("key" to "value")) }
            },
        )
    }

    private fun testRemove() {
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
            assertFrontReconciled = { frontCache ->
                verify { frontCache.removeAll(setOf("key")) }
            },
        )
    }

    private fun testReplace() {
        runInlineListenerOperation(
            operation = "replace",
            configureBack = { backCache, fire ->
                every { backCache.replace("key", "value") } answers {
                    fire(EventType.UPDATED)
                    true
                }
            },
            invoke = { it.replace("key", "value") },
            assertFrontReconciled = { frontCache ->
                verify { frontCache.putAll(mapOf("key" to "value")) }
            },
        )
    }

    @Test
    fun `동기 provider의 별도 callback thread도 self-event를 재조정한다`() {
        runInlineListenerOperation(
            operation = "separate-thread",
            configureBack = { backCache, fire ->
                every { backCache.put("key", "value") } answers {
                    dispatchListenerOnSeparateThread(fire, EventType.CREATED)
                }
            },
            invoke = { it.put("key", "value") },
            assertFrontReconciled = { frontCache ->
                verify { frontCache.putAll(mapOf("key" to "value")) }
            },
        )
    }

    @Test
    fun `비동기 self-event는 mutation gate 순서를 유지한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val listenerConfiguration = slot<CacheEntryListenerConfiguration<String, String>>()
        val event = mockk<CacheEntryEvent<String, String>>(relaxed = true)
        every { event.key } returns "key"
        every { event.value } returns "value"
        every { backCache.registerCacheEntryListener(capture(listenerConfiguration)) } just runs

        val backWriteStarted = CountDownLatch(1)
        val releaseBackWrite = CountDownLatch(1)
        val callbackStarted = CountDownLatch(1)
        val callbackReturned = CountDownLatch(1)
        val blockerStarted = CountDownLatch(1)
        val releaseBlocker = CountDownLatch(1)
        every { frontCache.put("blocker", "value") } answers {
            blockerStarted.countDown()
            releaseBlocker.await(2.seconds)
        }

        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = false, syncRemoteTimeout = 100L),
        )
        nearCache.registerBackCacheListener()
        val listener = listenerConfiguration.captured.cacheEntryListenerFactory!!.create()
                as CacheEntryCreatedListener<String, String>
        every { backCache.put("key", "value") } answers {
            backWriteStarted.countDown()
            releaseBackWrite.await(2.seconds)
            callbackStarted.countDown()
            listener.onCreated(listOf(event))
            callbackReturned.countDown()
        }

        try {
            nearCache.put("key", "value")
            backWriteStarted.await(2.seconds).shouldBeTrue()

            val blocker = virtualThread(start = false, name = "near-jcache-async-gate-blocker") {
                nearCache.put("blocker", "value")
            }
            blocker.start()
            blockerStarted.await(2.seconds).shouldBeTrue()

            releaseBackWrite.countDown()
            callbackStarted.await(2.seconds).shouldBeTrue()
            check(!callbackReturned.await(100.milliseconds)) {
                "async self-event bypassed mutationGate ordering"
            }
            verify(exactly = 0) { frontCache.putAll(mapOf("key" to "value")) }

            releaseBlocker.countDown()
            callbackReturned.await(2.seconds).shouldBeTrue()
            blocker.join(2_000)
            verify(exactly = 1) { frontCache.putAll(mapOf("key" to "value")) }
        } finally {
            releaseBackWrite.countDown()
            releaseBlocker.countDown()
            nearCache.close()
        }
    }

    @Test
    fun `timeout 뒤 late back completion은 후속 write보다 먼저 종료된다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondFinished = CountDownLatch(1)
        val calls = AtomicInteger()
        val values = CopyOnWriteArrayList<String>()
        every { backCache.put("key", any()) } answers {
            val value = secondArg<String>()
            values += value
            if (calls.incrementAndGet() == 1) {
                firstStarted.countDown()
                var released = false
                while (!released) {
                    try {
                        released = releaseFirst.await(10.milliseconds)
                    } catch (_: InterruptedException) {
                        // Ignore interruption to model a provider that completes after the caller timeout.
                    }
                }
            }
        }

        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true, syncRemoteTimeout = 50L),
        )
        try {
            runCatching { nearCache.put("key", "first") }.isFailure.shouldBeTrue()
            firstStarted.await(1.seconds).shouldBeTrue()

            val secondWorker = virtualThread(start = false, name = "near-jcache-late-completion-follow-up") {
                nearCache.put("key", "second")
                secondFinished.countDown()
            }
            secondWorker.start()
            secondFinished.await(100.milliseconds).shouldBeFalse()

            releaseFirst.countDown()
            secondFinished.await(2.seconds).shouldBeTrue()
            values.toList() shouldBeEqualTo listOf("first", "second")
        } finally {
            releaseFirst.countDown()
            nearCache.close()
        }
    }

    private fun runInlineListenerOperation(
        operation: String,
        configureFront: (JCache<String, String>) -> Unit = {},
        configureBack: (JCache<String, String>, (EventType) -> Unit) -> Unit,
        invoke: (NearJCache<String, String>) -> Unit,
        assertFrontReconciled: (JCache<String, String>) -> Unit = {},
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
            finished.await(2.seconds).shouldBeTrue()
            failure.get()?.let { throw AssertionError("synchronous $operation failed", it) }
            assertFrontReconciled(frontCache)
        } finally {
            if (worker.isAlive) {
                worker.interrupt()
                worker.join(2_000)
            }
            nearCache.close()
        }
    }

    private fun dispatchListenerOnSeparateThread(
        fire: (EventType) -> Unit,
        eventType: EventType,
    ) {
        val finished = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        val listenerThread = virtualThread(start = false, name = "near-jcache-listener-callback") {
            try {
                fire(eventType)
            } catch (error: Throwable) {
                failure.set(error)
            } finally {
                finished.countDown()
            }
        }
        listenerThread.start()
        check(finished.await(2.seconds)) {
            "synchronous listener callback did not complete on its separate thread"
        }
        failure.get()?.let { throw AssertionError("synchronous listener callback failed", it) }
    }
}
