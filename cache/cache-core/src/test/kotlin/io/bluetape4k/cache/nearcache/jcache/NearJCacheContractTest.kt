package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.cache.Cache
import javax.cache.configuration.MutableConfiguration

class NearJCacheContractTest {

    @Test
    fun `표준 Cache get은 back miss를 fallback하고 front를 채운다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.get("remote") } returns null
        every { backCache.get("remote") } returns "back-value"
        val standardCache: Cache<String, String> = newNearCache(frontCache, backCache)

        standardCache.get("remote") shouldBeEqualTo "back-value"

        verify(exactly = 1) { frontCache.put("remote", "back-value") }
    }

    @Test
    fun `표준 Cache containsKey는 front miss를 back에서 확인한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.containsKey("remote") } returns false
        every { backCache.containsKey("remote") } returns true
        val standardCache: Cache<String, String> = newNearCache(frontCache, backCache)

        standardCache.containsKey("remote").shouldBeTrue()

        verify(exactly = 1) { backCache.containsKey("remote") }
        verify(exactly = 0) { backCache.get("remote") }
    }

    @Test
    fun `표준 Cache getAll은 front와 back 결과를 한 번씩 병합하고 populate한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.getAll(setOf("front", "remote")) } returns
                mutableMapOf("front" to "front-value")
        every { backCache.getAll(setOf("remote")) } returns
                mutableMapOf("remote" to "back-value")
        val standardCache: Cache<String, String> = newNearCache(frontCache, backCache)

        val result = standardCache.getAll(setOf("front", "remote"))

        result shouldBeEqualTo mapOf("front" to "front-value", "remote" to "back-value")
        verify(exactly = 1) { backCache.getAll(setOf("remote")) }
        verify(exactly = 1) { frontCache.putAll(mapOf("remote" to "back-value")) }
    }

    @Test
    fun `표준 Cache getAll은 front hit만 있으면 back을 조회하지 않는다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.getAll(setOf("front")) } returns mutableMapOf("front" to "front-value")
        val standardCache: Cache<String, String> = newNearCache(frontCache, backCache)

        standardCache.getAll(setOf("front")) shouldBeEqualTo mapOf("front" to "front-value")

        verify(exactly = 0) { backCache.getAll(any()) }
        verify(exactly = 0) { frontCache.putAll(any()) }
    }

    @Test
    fun `표준 Cache clear는 front와 back을 함께 지운다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val standardCache: Cache<String, String> = newNearCache(frontCache, backCache)

        standardCache.clear()

        verify(exactly = 1) { frontCache.clear() }
        verify(exactly = 1) { backCache.clear() }
    }

    @Test
    fun `기존 getDeeply와 clearAllCache는 표준 get과 clear alias다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.get("remote") } returns null
        every { backCache.get("remote") } returns "back-value"
        val nearCache = newNearCache(frontCache, backCache)

        nearCache.getDeeply("remote") shouldBeEqualTo "back-value"
        nearCache.clearAllCache()

        verify(exactly = 1) { frontCache.put("remote", "back-value") }
        verify(exactly = 1) { frontCache.clear() }
        verify(exactly = 1) { backCache.clear() }
    }

    @Test
    fun `read-through가 mutation 이후 완료되면 stale 값을 front에 넣지 않는다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val readStarted = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        every { frontCache.get("key") } returns null
        every { backCache.get("key") } answers {
            readStarted.countDown()
            releaseRead.await(2, TimeUnit.SECONDS)
            "stale"
        }
        val nearCache = newNearCache(frontCache, backCache)
        val result = arrayOfNulls<String>(1)
        val reader = virtualThread(start = false, name = "near-jcache-stale-read") {
            result[0] = nearCache.get("key")
        }

        reader.start()
        readStarted.await(2, TimeUnit.SECONDS).shouldBeTrue()
        nearCache.put("key", "fresh")
        releaseRead.countDown()
        reader.join(2_000)

        result[0] shouldBeEqualTo "stale"
        verify(exactly = 1) { frontCache.put("key", "fresh") }
        verify(exactly = 0) { frontCache.put("key", "stale") }
    }

    @Test
    fun `getAll read-through가 mutation 이후 완료되면 stale 값을 front에 넣지 않는다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val readStarted = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        every { frontCache.getAll(setOf("key")) } returns mutableMapOf()
        every { backCache.getAll(setOf("key")) } answers {
            readStarted.countDown()
            releaseRead.await(2, TimeUnit.SECONDS)
            mutableMapOf("key" to "stale")
        }
        val nearCache = newNearCache(frontCache, backCache)
        val result = arrayOfNulls<MutableMap<String, String>>(1)
        val reader = virtualThread(start = false, name = "near-jcache-stale-get-all") {
            result[0] = nearCache.getAll(setOf("key"))
        }

        reader.start()
        readStarted.await(2, TimeUnit.SECONDS).shouldBeTrue()
        nearCache.put("key", "fresh")
        releaseRead.countDown()
        reader.join(2_000)

        result[0] shouldBeEqualTo mapOf("key" to "stale")
        verify(exactly = 1) { frontCache.put("key", "fresh") }
        verify(exactly = 0) { frontCache.putAll(mapOf("key" to "stale")) }
    }

    @Test
    fun `read-through가 clear 이후 완료되면 stale 값을 front에 넣지 않는다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val readStarted = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        every { frontCache.get("key") } returns null
        every { backCache.get("key") } answers {
            readStarted.countDown()
            releaseRead.await(2, TimeUnit.SECONDS)
            "stale"
        }
        val nearCache = newNearCache(frontCache, backCache)
        val result = arrayOfNulls<String>(1)
        val reader = virtualThread(start = false, name = "near-jcache-clear-read") {
            result[0] = nearCache.get("key")
        }

        reader.start()
        readStarted.await(2, TimeUnit.SECONDS).shouldBeTrue()
        nearCache.clear()
        releaseRead.countDown()
        reader.join(2_000)

        result[0] shouldBeEqualTo "stale"
        verify(exactly = 0) { frontCache.put("key", "stale") }
    }

    @Test
    fun `clear는 timeout 이후에도 실행 중인 back write가 끝날 때까지 기다린다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val writeStarted = CountDownLatch(1)
        val releaseWrite = CountDownLatch(1)
        every { backCache.put("key", "value") } answers {
            writeStarted.countDown()
            releaseWrite.await(2, TimeUnit.SECONDS)
        }
        val nearCache =
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(isSynchronous = false, syncRemoteTimeout = 1L),
            )
        nearCache.put("key", "value")
        writeStarted.await(2, TimeUnit.SECONDS).shouldBeTrue()

        val clearFinished = CountDownLatch(1)
        val clearer = virtualThread(start = false, name = "near-jcache-clear-barrier") {
            nearCache.clear()
            clearFinished.countDown()
        }
        clearer.start()
        clearFinished.await(100, TimeUnit.MILLISECONDS).shouldBeFalse()

        releaseWrite.countDown()
        clearFinished.await(2, TimeUnit.SECONDS).shouldBeTrue()
        verify(exactly = 1) { backCache.clear() }
    }

    @Test
    fun `front populate RuntimeException은 back read 결과를 숨기지 않는다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.get("key") } returns null
        every { backCache.get("key") } returns "value"
        every { frontCache.put("key", "value") } throws IllegalStateException("front unavailable")
        val nearCache = newNearCache(frontCache, backCache)

        nearCache.get("key") shouldBeEqualTo "value"
    }

    @Test
    fun `front populate Error는 숨기지 않고 재전파한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val failure = AssertionError("front linkage failure")
        every { frontCache.get("key") } returns null
        every { backCache.get("key") } returns "value"
        every { frontCache.put("key", "value") } throws failure
        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<AssertionError> { nearCache.get("key") }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `front clear 실패는 back clear를 실행하지 않고 호출자에게 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.clear() } throws IllegalStateException("front unavailable")
        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.clear() }

        verify(exactly = 0) { backCache.clear() }
    }

    @Test
    fun `기본 front 설정은 store by reference다`() {
        NearJCacheConfig.getDefaultFrontCacheConfiguration<String, String>().isStoreByValue.shouldBeFalse()
    }

    @Test
    fun `store by value front 설정은 생성 단계에서 거부한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val configuration = MutableConfiguration<String, String>().setStoreByValue(true)

        assertFailsWith<IllegalArgumentException> {
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(frontCacheConfiguration = configuration, isSynchronous = true),
            )
        }
    }

    @Test
    fun `compound getAndRemove와 getAndReplace는 front-only read 왕복을 유지한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.containsKey("remove") } returns true
        every { frontCache.get("remove") } returns "remove-value"
        every { frontCache.remove("remove") } returns true
        every { frontCache.containsKey("replace") } returns true
        every { frontCache.get("replace") } returns "old-value"
        every { frontCache.put("replace", "new-value") } returns Unit
        val nearCache = newNearCache(frontCache, backCache)

        nearCache.getAndRemove("remove") shouldBeEqualTo "remove-value"
        nearCache.getAndReplace("replace", "new-value") shouldBeEqualTo "old-value"

        verify(exactly = 0) { backCache.containsKey(any()) }
        verify(exactly = 0) { backCache.get(any()) }
    }

    private fun newNearCache(
        frontCache: JCache<String, String>,
        backCache: JCache<String, String>,
    ): NearJCache<String, String> =
        NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )
}
