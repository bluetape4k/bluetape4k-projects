package io.bluetape4k.cache.jcache

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import javax.cache.Cache
import javax.cache.event.CacheEntryEvent
import javax.cache.event.EventType

@OptIn(ExperimentalCoroutinesApi::class)
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
    fun `maxInFlightCallbacks는 양수여야 한다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())

        assertFailsWith<IllegalArgumentException> {
            SuspendJCacheEntryEventListener.forTest(
                targetCache = targetCache,
                scope = listenerScope,
                maxInFlightCallbacks = 0,
            )
        }
    }

    @Test
    fun `onCreated - targetCache에 putAll 이 호출된다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.putAll(any()) } returns Unit

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)
        val event = mockEvent("k1", "v1", EventType.CREATED)

        listener.onCreated(mutableListOf(event))
        runCurrent()

        coVerify { targetCache.putAll(mapOf("k1" to "v1")) }
        listener.close()
    }

    @Test
    fun `onCreated는 callback 반환 후 변경된 원본 iterable과 무관하게 batch 사본을 반영한다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.putAll(any()) } returns Unit
        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)
        val first = mockEvent("k1", "v1", EventType.CREATED)
        val second = mockEvent("k2", "v2", EventType.CREATED)
        val events = mutableListOf(first)

        listener.onCreated(events)
        events.clear()
        events += second
        runCurrent()

        coVerify(exactly = 1) { targetCache.putAll(mapOf("k1" to "v1")) }
        listener.close()
    }

    @Test
    fun `onUpdated - targetCache에 putAll 이 호출된다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.putAll(any()) } returns Unit

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)
        val event = mockEvent("k2", "v2", EventType.UPDATED)

        listener.onUpdated(mutableListOf(event))
        runCurrent()

        coVerify { targetCache.putAll(mapOf("k2" to "v2")) }
        listener.close()
    }

    @Test
    fun `onRemoved - targetCache에 removeAll 이 호출된다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.removeAll(any<Set<String>>()) } returns Unit

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)
        val event = mockEvent("k3", "v3", EventType.REMOVED)

        listener.onRemoved(mutableListOf(event))
        runCurrent()

        coVerify { targetCache.removeAll(setOf("k3")) }
        listener.close()
    }

    @Test
    fun `onRemoved는 null value 이벤트를 key-only snapshot으로 처리한다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.removeAll(any<Set<String>>()) } returns Unit

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)
        val event = mockk<CacheEntryEvent<String, String>>()
        every { event.key } returns "removed-without-value"
        every { event.value } returns null
        every { event.eventType } returns EventType.REMOVED
        every { event.source } returns mockk(relaxed = true)

        listener.onRemoved(mutableListOf(event))
        runCurrent()

        coVerify { targetCache.removeAll(setOf("removed-without-value")) }
        listener.close()
    }

    @Test
    fun `onExpired - targetCache에 removeAll 이 호출된다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.removeAll(any<Set<String>>()) } returns Unit

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)
        val event = mockEvent("k4", "v4", EventType.EXPIRED)

        listener.onExpired(mutableListOf(event))
        runCurrent()

        coVerify { targetCache.removeAll(setOf("k4")) }
        listener.close()
    }

    @Test
    fun `targetCache 가 closed 이면 이벤트를 무시한다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns true

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)
        val event = mockEvent("k5", "v5", EventType.CREATED)

        // 예외 없이 완료되어야 하며, putAll 이 호출되지 않아야 한다
        listener.onCreated(mutableListOf(event))
        runCurrent()

        coVerify(exactly = 0) { targetCache.putAll(any()) }
        listener.close()
    }

    @Test
    fun `close - scope 가 취소된다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)

        // 예외 없이 완료되어야 한다
        listener.close()
        listener.close()
        listener.onCreated(mutableListOf(mockEvent("k6", "v6", EventType.CREATED)))
        runCurrent()
        coVerify(exactly = 0) { targetCache.putAll(any()) }
    }

    @Test
    fun `CancellationException은 listener child job을 취소 상태로 남긴다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        val started = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val cancellation = CancellationException("listener cancelled")
        val invocationCount = AtomicInteger()
        coEvery { targetCache.putAll(any()) } coAnswers {
            if (invocationCount.incrementAndGet() == 1) {
                started.complete(Unit)
                release.await()
            } else {
                secondStarted.complete(Unit)
            }
        }

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listenerJob = listenerScope.coroutineContext[Job]!!
        val listener = SuspendJCacheEntryEventListener.forTest(
            targetCache,
            listenerScope,
            maxInFlightCallbacks = 1,
        )
        listener.onCreated(mutableListOf(mockEvent("k7", "v7", EventType.CREATED)))
        runCurrent()
        started.await()
        val child = listenerJob.children.single()

        release.completeExceptionally(cancellation)
        runCurrent()
        child.join()

        child.isCancelled.shouldBeTrue()

        listener.onCreated(mutableListOf(mockEvent("k7-second", "v7-second", EventType.CREATED)))
        runCurrent()
        secondStarted.await()
        coVerify(exactly = 2) { targetCache.putAll(any()) }

        listener.close()
    }

    @Test
    fun `close는 이미 시작된 callback의 cooperative cancellation을 요청한다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        val started = CompletableDeferred<Unit>()
        coEvery { targetCache.putAll(any()) } coAnswers {
            started.complete(Unit)
            awaitCancellation()
        }

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listenerJob = listenerScope.coroutineContext[Job]!!
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)
        listener.onCreated(mutableListOf(mockEvent("k8", "v8", EventType.CREATED)))
        runCurrent()
        started.await()
        val child = listenerJob.children.single()

        listener.close()
        runCurrent()
        child.join()

        child.isCancelled.shouldBeTrue()
    }

    @Test
    fun `bounded admission은 callback job 수와 accepted operation 수를 제한한다`() = runTest {
        val maxInFlight = 2
        val callbackCount = 8
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        val startedCount = AtomicInteger()
        val allStarted = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        coEvery { targetCache.putAll(any()) } coAnswers {
            if (startedCount.incrementAndGet() == maxInFlight) {
                allStarted.complete(Unit)
            }
            release.await()
        }

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listenerJob = listenerScope.coroutineContext[Job]!!
        val listener = SuspendJCacheEntryEventListener.forTest(
            targetCache,
            listenerScope,
            maxInFlightCallbacks = maxInFlight,
        )
        repeat(callbackCount) { index ->
            listener.onCreated(mutableListOf(mockEvent("burst-$index", "value", EventType.CREATED)))
        }
        runCurrent()
        allStarted.await()

        listenerJob.children.toList().size.shouldBeEqualTo(maxInFlight)
        release.complete(Unit)
        runCurrent()

        coVerify(exactly = maxInFlight) { targetCache.putAll(any()) }
        listener.close()
    }

    @Test
    fun `close는 bounded cooperative callback burst를 모두 취소한다`() = runTest {
        val maxInFlight = 2
        val callbackCount = 8
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        val startedCount = AtomicInteger()
        val allStarted = CompletableDeferred<Unit>()
        coEvery { targetCache.putAll(any()) } coAnswers {
            if (startedCount.incrementAndGet() == maxInFlight) {
                allStarted.complete(Unit)
            }
            awaitCancellation()
        }

        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listenerJob = listenerScope.coroutineContext[Job]!!
        val listener = SuspendJCacheEntryEventListener.forTest(
            targetCache,
            listenerScope,
            maxInFlightCallbacks = maxInFlight,
        )
        repeat(callbackCount) { index ->
            listener.onCreated(mutableListOf(mockEvent("close-burst-$index", "value", EventType.CREATED)))
        }
        runCurrent()
        allStarted.await()
        val children = listenerJob.children.toList()

        children.size.shouldBeEqualTo(maxInFlight)
        listener.close()
        runCurrent()
        children.joinAll()

        children.all { it.isCancelled }.shouldBeTrue()
    }

    @Test
    fun `취소된 scope에서 시작되지 않은 lazy child도 permit을 반환한다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.putAll(any()) } returns Unit

        val logger = SuspendJCacheEntryEventListener.log as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.DEBUG
        logger.addAppender(appender)
        val cancelledJob = SupervisorJob().apply { cancel() }
        val listenerScope = CoroutineScope(coroutineContext + cancelledJob)
        val listener = SuspendJCacheEntryEventListener.forTest(
            targetCache,
            listenerScope,
            maxInFlightCallbacks = 1,
        )
        try {
            listener.onCreated(mutableListOf(mockEvent("lazy-1", "value", EventType.CREATED)))
            listener.onCreated(mutableListOf(mockEvent("lazy-2", "value", EventType.CREATED)))
            runCurrent()

            appender.list
                .map { it.formattedMessage }
                .count { it.contains("admission is full") }
                .shouldBeEqualTo(0)
            coVerify(exactly = 0) { targetCache.putAll(any()) }
        } finally {
            listener.close()
            logger.detachAppender(appender)
            appender.stop()
            logger.level = previousLevel
        }
    }

    @Test
    fun `일반 예외는 sibling callback을 취소하지 않는다`() = runTest {
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        val invocationCount = AtomicInteger()
        val secondStarted = CompletableDeferred<Unit>()
        coEvery { targetCache.putAll(any()) } coAnswers {
            if (invocationCount.incrementAndGet() == 1) {
                throw IllegalStateException("backend failure")
            }
            secondStarted.complete(Unit)
        }

        val logger = SuspendJCacheEntryEventListener.log as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.ERROR
        logger.addAppender(appender)
        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listenerJob = listenerScope.coroutineContext[Job]!!
        val listener = SuspendJCacheEntryEventListener.forTest(
            targetCache,
            listenerScope,
            maxInFlightCallbacks = 2,
        )
        try {
            listener.onCreated(mutableListOf(mockEvent("exception-1", "value", EventType.CREATED)))
            listener.onCreated(mutableListOf(mockEvent("exception-2", "value", EventType.CREATED)))
            runCurrent()
            secondStarted.await()

            listenerJob.isActive.shouldBeTrue()
            coVerify(exactly = 2) { targetCache.putAll(any()) }
            val errorMessages = appender.list
                .map { it.formattedMessage }
                .filter { it.contains("Fail to put all created cache entries") }
            errorMessages.isNotEmpty().shouldBeTrue()
            errorMessages.all { it.contains("cache=") && !it.contains("backend failure") }.shouldBeTrue()
        } finally {
            listener.close()
            logger.detachAppender(appender)
            appender.stop()
            logger.level = previousLevel
        }
    }

    @Test
    fun `admission overflow log에는 raw payload가 포함되지 않는다`() = runTest {
        val secretKey = "overflow-secret-key"
        val secretValue = "overflow-secret-value"
        val secretSource = "overflow-secret-source"
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        val started = CompletableDeferred<Unit>()
        coEvery { targetCache.putAll(any()) } coAnswers {
            started.complete(Unit)
            awaitCancellation()
        }
        val overflowEvent = mockk<CacheEntryEvent<String, String>>()
        val source = mockk<Cache<Any, Any>>()
        every { overflowEvent.key } returns secretKey
        every { overflowEvent.value } returns secretValue
        every { overflowEvent.eventType } returns EventType.CREATED
        every { source.toString() } returns secretSource
        every { overflowEvent.source } returns source

        val logger = SuspendJCacheEntryEventListener.log as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.DEBUG
        logger.addAppender(appender)
        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(
            targetCache,
            listenerScope,
            maxInFlightCallbacks = 1,
        )
        try {
            listener.onCreated(mutableListOf(mockEvent("first", "value", EventType.CREATED)))
            runCurrent()
            started.await()
            listener.onCreated(mutableListOf(overflowEvent))
            runCurrent()

            val overflowMessages = appender.list
                .map { it.formattedMessage }
                .filter { it.contains("admission is full") }
            overflowMessages.isNotEmpty().shouldBeTrue()
            overflowMessages.all { message ->
                message.contains("operation=put all created cache entries") &&
                        !message.contains(secretKey) &&
                        !message.contains(secretValue) &&
                        !message.contains(secretSource)
            }.shouldBeTrue()
        } finally {
            listener.close()
            runCurrent()
            logger.detachAppender(appender)
            appender.stop()
            logger.level = previousLevel
        }
    }

    @Test
    fun `event callback log에는 raw key value source를 포함하지 않는다`() = runTest {
        val secretKey = "secret-key-should-not-log"
        val secretValue = "secret-value-should-not-log"
        val secretSource = "secret-source-should-not-log"
        val targetCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        every { targetCache.isClosed() } returns false
        coEvery { targetCache.putAll(any()) } returns Unit
        val event = mockk<CacheEntryEvent<String, String>>()
        val source = mockk<Cache<Any, Any>>()
        every { event.key } returns secretKey
        every { event.value } returns secretValue
        every { source.toString() } returns secretSource
        every { event.source } returns source
        every { event.eventType } returns EventType.CREATED

        val logger = SuspendJCacheEntryEventListener.log as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.TRACE
        logger.addAppender(appender)
        val listenerScope = CoroutineScope(coroutineContext + SupervisorJob())
        val listener = SuspendJCacheEntryEventListener.forTest(targetCache, listenerScope)
        try {
            listener.onCreated(mutableListOf(event))
            runCurrent()
            val messages = appender.list
                .map { it.formattedMessage }
                .filter { it.contains("BackCache cache entry created") }

            messages.isNotEmpty().shouldBeTrue()
            messages.all { message ->
                !message.contains(secretKey) &&
                        !message.contains(secretValue) &&
                        !message.contains(secretSource)
            }.shouldBeTrue()
        } finally {
            listener.close()
            logger.detachAppender(appender)
            appender.stop()
            logger.level = previousLevel
        }
    }
}
