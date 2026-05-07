package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.coEvery
import org.junit.jupiter.api.Test
import javax.cache.event.CacheEntryEvent
import javax.cache.event.EventType

class SuspendJCacheEntryEventListenerTest {

    companion object: KLoggingChannel()

    private fun mockEvent(key: String, value: String, eventType: EventType): CacheEntryEvent<String, String> {
        val event = mockk<CacheEntryEvent<String, String>>()
        every { event.key } returns key
        every { event.value } returns value
        every { event.eventType } returns eventType
        every { event.source } returns mockk(relaxed = true)
        return event
    }

    @Test
    fun `onCreated - targetCache에 putAll 이 호출된다`() {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.putAll(any()) } returns Unit

        val listener = SuspendJCacheEntryEventListener(targetCache)
        val event = mockEvent("k1", "v1", EventType.CREATED)

        listener.onCreated(mutableListOf(event))
        Thread.sleep(200)

        coVerify { targetCache.putAll(mapOf("k1" to "v1")) }
    }

    @Test
    fun `onUpdated - targetCache에 putAll 이 호출된다`() {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.putAll(any()) } returns Unit

        val listener = SuspendJCacheEntryEventListener(targetCache)
        val event = mockEvent("k2", "v2", EventType.UPDATED)

        listener.onUpdated(mutableListOf(event))
        Thread.sleep(200)

        coVerify { targetCache.putAll(mapOf("k2" to "v2")) }
    }

    @Test
    fun `onRemoved - targetCache에 removeAll 이 호출된다`() {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.removeAll(any<Set<String>>()) } returns Unit

        val listener = SuspendJCacheEntryEventListener(targetCache)
        val event = mockEvent("k3", "v3", EventType.REMOVED)

        listener.onRemoved(mutableListOf(event))
        Thread.sleep(200)

        coVerify { targetCache.removeAll(setOf("k3")) }
    }

    @Test
    fun `onExpired - targetCache에 removeAll 이 호출된다`() {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.removeAll(any<Set<String>>()) } returns Unit

        val listener = SuspendJCacheEntryEventListener(targetCache)
        val event = mockEvent("k4", "v4", EventType.EXPIRED)

        listener.onExpired(mutableListOf(event))
        Thread.sleep(200)

        coVerify { targetCache.removeAll(setOf("k4")) }
    }

    @Test
    fun `targetCache 가 closed 이면 이벤트를 무시한다`() {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns true

        val listener = SuspendJCacheEntryEventListener(targetCache)
        val event = mockEvent("k5", "v5", EventType.CREATED)

        // 예외 없이 완료되어야 하며, putAll 이 호출되지 않아야 한다
        listener.onCreated(mutableListOf(event))
        Thread.sleep(100)

        coVerify(exactly = 0) { targetCache.putAll(any()) }
    }

    @Test
    fun `close - scope 가 취소된다`() {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false

        val listener = SuspendJCacheEntryEventListener(targetCache)

        // 예외 없이 완료되어야 한다
        listener.close()
    }
}
