package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent

class NearJCacheCompoundOperationContractTest {

    @Test
    fun `동기 getAndPut은 back 원자 연산 결과를 반환하고 front를 새 값으로 갱신한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.getAndPut("key", "new") } returns "old"
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        nearCache.getAndPut("key", "new") shouldBeEqualTo "old"

        verify(exactly = 1) { backCache.getAndPut("key", "new") }
        verify(exactly = 1) { frontCache.put("key", "new") }
    }

    @Test
    fun `동기 getAndRemove는 front miss에서도 back 원자 연산 결과를 반환하고 front를 제거한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.getAndRemove("key") } returns "old"
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        nearCache.getAndRemove("key") shouldBeEqualTo "old"

        verify(exactly = 1) { backCache.getAndRemove("key") }
        verify(exactly = 1) { frontCache.remove("key") }
    }

    @Test
    fun `동기 getAndReplace는 front miss에서도 back 원자 연산 결과를 반환하고 front를 새 값으로 갱신한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.getAndReplace("key", "new") } returns "old"
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        nearCache.getAndReplace("key", "new") shouldBeEqualTo "old"

        verify(exactly = 1) { backCache.getAndReplace("key", "new") }
        verify(exactly = 1) { frontCache.put("key", "new") }
    }

    @Test
    fun `동기 compound back 실패는 front를 변경하지 않고 호출자에게 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val failure = IllegalStateException("back failure")
        every { backCache.getAndPut("key", "new") } throws failure
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        assertFailsWith<IllegalStateException> { nearCache.getAndPut("key", "new") }

        verify(exactly = 0) { frontCache.put(any(), any()) }
    }

    @Test
    fun `동기 listener를 호출하는 back compound 연산도 deadlock 없이 front를 갱신한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val listenerConfiguration = slot<CacheEntryListenerConfiguration<String, String>>()
        val event = mockk<CacheEntryEvent<String, String>>(relaxed = true)
        every { event.key } returns "key"
        every { event.value } returns "new"
        every { backCache.registerCacheEntryListener(capture(listenerConfiguration)) } just runs
        every { backCache.getAndPut("key", "new") } answers {
            val listener = listenerConfiguration.captured.cacheEntryListenerFactory!!.create()
            (listener as CacheEntryCreatedListener<String, String>).onCreated(listOf(event))
            "old"
        }
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )
        nearCache.registerBackCacheListener()

        nearCache.getAndPut("key", "new") shouldBeEqualTo "old"

        verify(exactly = 1) { backCache.getAndPut("key", "new") }
        verify(atLeast = 1) { frontCache.put("key", "new") }
    }

    @Test
    fun `동일 wrapper의 replace와 remove는 front reconciliation 순서를 직렬화한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val replaceStarted = CountDownLatch(1)
        val releaseReplace = CountDownLatch(1)
        val removeStarted = CountDownLatch(1)
        every { backCache.getAndReplace("key", "new") } answers {
            replaceStarted.countDown()
            releaseReplace.await(2, TimeUnit.SECONDS).shouldBeTrue()
            "old"
        }
        every { backCache.getAndRemove("key") } answers {
            removeStarted.countDown()
            "new"
        }
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )
        val replaceResult = AtomicReference<String?>()
        val removeResult = AtomicReference<String?>()
        val replaceThread = virtualThread(start = false, name = "near-jcache-compound-replace") {
            replaceResult.set(nearCache.getAndReplace("key", "new"))
        }
        val removeThread = virtualThread(start = false, name = "near-jcache-compound-remove") {
            removeResult.set(nearCache.getAndRemove("key"))
        }

        replaceThread.start()
        replaceStarted.await(2, TimeUnit.SECONDS).shouldBeTrue()
        removeThread.start()
        removeStarted.await(200, TimeUnit.MILLISECONDS).shouldBeFalse()

        releaseReplace.countDown()
        replaceThread.join(2_000)
        removeThread.join(2_000)

        replaceResult.get() shouldBeEqualTo "old"
        removeResult.get() shouldBeEqualTo "new"
        verifyOrder {
            frontCache.put("key", "new")
            frontCache.remove("key")
        }
    }

    @Test
    fun `suspend getAndPut은 back 원자 연산 결과를 반환하고 front를 새 값으로 갱신한다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.getAndPut("key", "new") } returns "old"
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        nearCache.getAndPut("key", "new") shouldBeEqualTo "old"

        coVerify(exactly = 1) { backCache.getAndPut("key", "new") }
        coVerify(exactly = 1) { frontCache.put("key", "new") }
    }

    @Test
    fun `suspend compound back 실패는 front를 변경하지 않고 호출자에게 전달한다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val failure = IllegalStateException("back failure")
        coEvery { backCache.getAndPut("key", "new") } throws failure
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.getAndPut("key", "new") }

        coVerify(exactly = 0) { frontCache.put(any(), any()) }
    }

    @Test
    fun `suspend getAndRemove는 front miss에서도 back 원자 연산 결과를 반환하고 front를 제거한다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.getAndRemove("key") } returns "old"
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        nearCache.getAndRemove("key") shouldBeEqualTo "old"

        coVerify(exactly = 1) { backCache.getAndRemove("key") }
        coVerify(exactly = 1) { frontCache.remove("key") }
    }

    @Test
    fun `suspend getAndReplace는 front miss에서도 back 원자 연산 결과를 반환하고 front를 새 값으로 갱신한다`() = runSuspendIO {
        val frontCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        val backCache = mockk<SuspendJCache<String, String>>(relaxed = true)
        coEvery { backCache.getAndReplace("key", "new") } returns "old"
        val nearCache = SuspendNearJCache.withoutListener(frontCache, backCache)

        nearCache.getAndReplace("key", "new") shouldBeEqualTo "old"

        coVerify(exactly = 1) { backCache.getAndReplace("key", "new") }
        coVerify(exactly = 1) { frontCache.put("key", "new") }
    }
}
