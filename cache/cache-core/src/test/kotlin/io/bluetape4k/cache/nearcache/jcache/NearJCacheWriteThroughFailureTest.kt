package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.cache.jcache.JCache
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import javax.cache.Cache

class NearJCacheWriteThroughFailureTest {

    private val failure = IllegalStateException("back cache is unavailable")

    @Test
    fun `동기 put은 back cache 실패를 호출자에게 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.put("key", "value") } throws failure

        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> {
            nearCache.put("key", "value")
        }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `동기 putAll은 back cache 실패를 호출자에게 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.putAll(mapOf("key" to "value")) } throws failure

        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> {
            nearCache.putAll(mapOf("key" to "value"))
        }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `동기 putIfAbsent는 back cache 실패를 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.putIfAbsent("key", "value") } returns true
        every { backCache.containsKey("key") } returns false
        every { backCache.put("key", "value") } throws failure

        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> {
            nearCache.putIfAbsent("key", "value")
        }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `동기 remove는 back cache 실패를 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.remove("key") } returns true
        every { backCache.remove("key") } throws failure

        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> {
            nearCache.remove("key")
        }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `동기 조건부 remove는 back cache 실패를 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.remove("key", "old") } returns true
        every { backCache.containsKey("key") } returns true
        every { backCache.get("key") } returns "old"
        every { backCache.remove("key") } throws failure

        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> {
            nearCache.remove("key", "old")
        }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `동기 replace는 back cache 실패를 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.replace("key", "value") } returns true
        every { backCache.containsKey("key") } returns true
        every { backCache.put("key", "value") } throws failure

        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> {
            nearCache.replace("key", "value")
        }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `동기 조건부 replace는 back cache 실패를 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.replace("key", "old", "new") } returns true
        every { backCache.containsKey("key") } returns true
        every { backCache.get("key") } returns "old"
        every { backCache.put("key", "new") } throws failure

        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> {
            nearCache.replace("key", "old", "new")
        }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `동기 removeAll은 부분 back cache 실패를 집계해 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.remove("key-1") } throws failure
        every { backCache.remove("key-2") } throws IllegalArgumentException("second failure")

        val nearCache = newNearCache(frontCache, backCache)

        val error = assertFailsWith<IllegalStateException> {
            nearCache.removeAll(setOf("key-1", "key-2"))
        }
        error.message shouldBeEqualTo failure.message
        error.suppressed.size shouldBeEqualTo 1
    }

    @Test
    fun `동기 removeAll은 back cache iterator 삭제 실패를 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val entry = mockk<Cache.Entry<String, String>>()
        every { entry.key } returns "key"
        every { backCache.iterator() } returns mutableListOf(entry).iterator()
        every { backCache.remove("key") } throws failure

        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> {
            nearCache.removeAll()
        }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `동기 removeAll은 동일 예외 인스턴스도 원래 실패를 보존한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val sameFailure = IllegalStateException("shared back cache failure")
        every { backCache.remove(any()) } throws sameFailure

        val nearCache = newNearCache(frontCache, backCache)

        val error = assertFailsWith<IllegalStateException> {
            nearCache.removeAll(setOf("key-1", "key-2"))
        }
        error.message shouldBeEqualTo sameFailure.message
        error.suppressed.size shouldBeEqualTo 1
    }

    @Test
    fun `동기 clearAllCache는 back cache 실패를 호출자에게 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.clear() } throws failure

        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> {
            nearCache.clearAllCache()
        }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `비동기 write-through는 back cache 예외를 completion으로 노출한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.put("key", "value") } throws failure

        val nearCache =
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(isSynchronous = false),
            )

        nearCache.put("key", "value")

        val error = assertFailsWith<ExecutionException> {
            nearCache.lastBackCacheWriteCompletion.get(2, TimeUnit.SECONDS)
        }
        error.cause?.message shouldBeEqualTo failure.message
    }

    @Test
    fun `비동기 write-through는 즉시 실패를 bounded retry 후 성공시킨다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val attempts = AtomicInteger()
        every { backCache.put("key", "value") } answers {
            if (attempts.incrementAndGet() == 1) {
                throw failure
            }
        }

        val nearCache =
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(isSynchronous = false, syncRemoteRetryCount = 1),
            )

        nearCache.put("key", "value")

        nearCache.lastBackCacheWriteCompletion.get(2, TimeUnit.SECONDS)
        attempts.get() shouldBeEqualTo 2
    }

    @Test
    fun `비동기 write-through는 Error를 재시도하지 않는다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val attempts = AtomicInteger()
        val failure = AssertionError("backend linkage failure")
        every { backCache.put("key", "value") } answers {
            attempts.incrementAndGet()
            throw failure
        }
        val nearCache =
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(isSynchronous = false, syncRemoteRetryCount = 3),
            )

        nearCache.put("key", "value")

        val error = assertFailsWith<ExecutionException> {
            nearCache.lastBackCacheWriteCompletion.get(2, TimeUnit.SECONDS)
        }
        check(error.cause is AssertionError) { "Expected Error cause but got ${error.cause}" }
        attempts.get() shouldBeEqualTo 1
    }

    @Test
    fun `비동기 write-through retry 횟수는 3회로 제한된다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val attempts = AtomicInteger()
        every { backCache.put("key", "value") } answers {
            attempts.incrementAndGet()
            throw failure
        }
        val nearCache =
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(isSynchronous = false, syncRemoteRetryCount = 99),
            )

        nearCache.put("key", "value")

        assertFailsWith<ExecutionException> {
            nearCache.lastBackCacheWriteCompletion.get(2, TimeUnit.SECONDS)
        }
        attempts.get() shouldBeEqualTo 4
    }

    @Test
    fun `비동기 write-through는 operation별 completion을 listener로 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.put(any(), any()) } answers {
            if (firstArg<String>() == "failed") {
                throw failure
            }
        }
        val nearCache =
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(isSynchronous = false, syncRemoteRetryCount = 0),
            )
        val completions = CopyOnWriteArrayList<BackCacheWriteCompletion>()
        val completed = CountDownLatch(2)
        val registration = nearCache.addBackCacheWriteListener {
            completions += it
            completed.countDown()
        }

        try {
            nearCache.put("failed", "value")
            nearCache.put("succeeded", "value")

            check(completed.await(2, TimeUnit.SECONDS)) { "write-through completions were not observed" }
            completions.size shouldBeEqualTo 2
            completions.map { it.operationId }.toSet().size shouldBeEqualTo 2
            completions.single { it.completion.toCompletableFuture().isCompletedExceptionally }
                .completion.toCompletableFuture().join()
        } catch (e: java.util.concurrent.CompletionException) {
            e.cause?.message shouldBeEqualTo failure.message
        } finally {
            registration.close()
        }
    }

    @Test
    fun `last completion 복사본을 변경해도 실제 write 결과는 보존된다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val release = CountDownLatch(1)
        every { backCache.put("key", "value") } answers {
            release.await()
            throw failure
        }
        val nearCache =
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(isSynchronous = false, syncRemoteRetryCount = 0),
            )

        try {
            nearCache.put("key", "value")
            nearCache.lastBackCacheWriteCompletion.complete(Unit)
        } finally {
            release.countDown()
        }

        val error = assertFailsWith<ExecutionException> {
            nearCache.lastBackCacheWriteCompletion.get(2, TimeUnit.SECONDS)
        }
        error.cause?.message shouldBeEqualTo failure.message
    }

    @Test
    fun `비동기 write-through timeout은 completion 오류로 노출된다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val release = CountDownLatch(1)
        val attempts = AtomicInteger()
        every { backCache.put("key", "value") } answers {
            attempts.incrementAndGet()
            release.await()
        }

        val nearCache =
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(isSynchronous = false, syncRemoteTimeout = 1L, syncRemoteRetryCount = 2),
            )

        try {
            nearCache.put("key", "value")

            val error = assertFailsWith<ExecutionException> {
                nearCache.lastBackCacheWriteCompletion.get(2, TimeUnit.SECONDS)
            }
            check(error.cause is TimeoutException) {
                "Expected timeout failure but got ${error.cause}"
            }
            attempts.get() shouldBeEqualTo 1
        } finally {
            release.countDown()
        }
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
