package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.UUID
import javax.cache.configuration.MutableConfiguration

class NearJCacheConditionalMutationContractTest {

    @Test
    fun `putIfAbsent는 back 원자 결과를 반환하고 실패 시 stale front를 제거한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.putIfAbsent("key", "new") } returns true
        every { backCache.putIfAbsent("key", "new") } returns false
        every { frontCache.remove("key") } returns true
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        nearCache.putIfAbsent("key", "new").shouldBeFalse()

        verifyOrder {
            backCache.putIfAbsent("key", "new")
            frontCache.remove("key")
        }
    }

    @Test
    fun `remove와 replace는 front miss에서도 back 원자 결과와 front 보정을 사용한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.remove("remove") } returns true
        every { backCache.remove("remove-old", "old") } returns true
        every { backCache.replace("replace-old", "old", "new") } returns true
        every { backCache.replace("replace", "new") } returns true
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        nearCache.remove("remove").shouldBeTrue()
        nearCache.remove("remove-old", "old").shouldBeTrue()
        nearCache.replace("replace-old", "old", "new").shouldBeTrue()
        nearCache.replace("replace", "new").shouldBeTrue()

        verifyOrder {
            backCache.remove("remove")
            frontCache.remove("remove")
        }
        verifyOrder {
            backCache.remove("remove-old", "old")
            frontCache.remove("remove-old")
        }
        verifyOrder {
            backCache.replace("replace-old", "old", "new")
            frontCache.put("replace-old", "new")
        }
        verifyOrder {
            backCache.replace("replace", "new")
            frontCache.put("replace", "new")
        }
    }

    @Test
    fun `front miss와 back hit 조건부 mutation은 최종 두 tier 상태를 back 기준으로 맞춘다`() {
        val configuration = MutableConfiguration<String, String>()
            .setTypes(String::class.java, String::class.java)
            .setStoreByValue(false)
        val cacheName = "issue-1447-${UUID.randomUUID()}"
        val frontCache = JCaching.Caffeine.getOrCreate("$cacheName-front", configuration)
        val backCache = JCaching.Caffeine.getOrCreate("$cacheName-back", configuration)
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(
                frontCacheConfiguration = configuration,
                isSynchronous = true,
            ),
        )

        try {
            backCache.put("put-if-absent", "old")
            nearCache.putIfAbsent("put-if-absent", "new").shouldBeFalse()
            backCache.get("put-if-absent") shouldBeEqualTo "old"
            frontCache.containsKey("put-if-absent").shouldBeFalse()

            backCache.put("remove", "old")
            nearCache.remove("remove").shouldBeTrue()
            backCache.containsKey("remove").shouldBeFalse()
            frontCache.containsKey("remove").shouldBeFalse()

            backCache.put("remove-old", "old")
            nearCache.remove("remove-old", "old").shouldBeTrue()
            backCache.containsKey("remove-old").shouldBeFalse()
            frontCache.containsKey("remove-old").shouldBeFalse()

            backCache.put("replace-old", "old")
            nearCache.replace("replace-old", "old", "new").shouldBeTrue()
            backCache.get("replace-old") shouldBeEqualTo "new"
            frontCache.get("replace-old") shouldBeEqualTo "new"

            backCache.put("replace", "old")
            nearCache.replace("replace", "new").shouldBeTrue()
            backCache.get("replace") shouldBeEqualTo "new"
            frontCache.get("replace") shouldBeEqualTo "new"
        } finally {
            nearCache.close()
            backCache.close()
        }
    }

    @Test
    fun `back 조건부 mutation 실패는 front를 변경하지 않고 원래 예외를 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val failure = IllegalStateException("back cache is unavailable")
        every { backCache.replace("key", "new") } throws failure
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        val error = assertFailsWith<IllegalStateException> {
            nearCache.replace("key", "new")
        }

        error shouldBeEqualTo failure
        verify(exactly = 0) { frontCache.put(any(), any()) }
        verify(exactly = 0) { frontCache.remove(any()) }
    }

    @Test
    fun `back cancellation은 front 변경 없이 호출자에게 재전파된다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val cancellation = CancellationException("back cache cancelled")
        every { backCache.putIfAbsent("key", "new") } throws cancellation
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        val error = assertFailsWith<CancellationException> {
            nearCache.putIfAbsent("key", "new")
        }

        error shouldBeEqualTo cancellation
        verify(exactly = 0) { frontCache.put(any(), any()) }
        verify(exactly = 0) { frontCache.remove(any()) }
    }

    @Test
    fun `back commit 후 front 보정 실패는 front를 invalidate하고 원래 예외를 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val frontFailure = IllegalStateException("front cache is unavailable")
        every { backCache.putIfAbsent("key", "new") } returns true
        every { frontCache.put("key", "new") } throws frontFailure
        every { frontCache.remove("key") } returns true
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(isSynchronous = true),
        )

        val error = assertFailsWith<IllegalStateException> {
            nearCache.putIfAbsent("key", "new")
        }

        error shouldBeEqualTo frontFailure
        verify(exactly = 1) { frontCache.remove("key") }
    }

    @Test
    fun `동시 putIfAbsent는 back 원자 결과 하나만 성공시키고 최종 값을 보존한다`() {
        val configuration = MutableConfiguration<String, String>()
            .setTypes(String::class.java, String::class.java)
            .setStoreByValue(false)
        val cacheName = "issue-1447-concurrent-${UUID.randomUUID()}"
        val frontCache = JCaching.Caffeine.getOrCreate("$cacheName-front", configuration)
        val backCache = JCaching.Caffeine.getOrCreate("$cacheName-back", configuration)
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(
                frontCacheConfiguration = configuration,
                isSynchronous = true,
            ),
        )
        val outcomes = ConcurrentLinkedQueue<Boolean>()

        try {
            MultithreadingTester()
                .workers(8)
                .rounds(4)
                .add { outcomes.add(nearCache.putIfAbsent("key", "value")) }
                .run()

            outcomes.count { it }.shouldBeEqualTo(1)
            backCache.get("key") shouldBeEqualTo "value"
            nearCache.get("key") shouldBeEqualTo "value"
            frontCache.get("key") shouldBeEqualTo "value"
        } finally {
            nearCache.close()
            backCache.close()
        }
    }
}
