package io.bluetape4k.cache.nearcache.jcache

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCacheEntryEventListener
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryUpdatedListener

class NearJCacheContractTest {

    @Test
    fun `명시적 close는 front cleanup failure를 호출자에게 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val failure = IllegalStateException("front close failed")
        every { frontCache.close() } throws failure
        val nearCache = newNearCache(frontCache, backCache)

        val error = assertFailsWith<IllegalStateException> { nearCache.close() }

        (error === failure).shouldBeTrue()
        verify(exactly = 1) { frontCache.close() }
    }

    @Test
    fun `front cleanup failure 후 close는 다음 호출에서 front 정리를 재시도한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val failure = IllegalStateException("front close failed")
        val closeAttempts = AtomicInteger()
        every { frontCache.close() } answers {
            if (closeAttempts.incrementAndGet() == 1) {
                throw failure
            }
        }
        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<IllegalStateException> { nearCache.close() }
        nearCache.close()

        verify(exactly = 2) { frontCache.close() }
    }

    @Test
    fun `close cleanup 로그는 operation cache provider metadata를 기록한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val cacheName = "near-jcache-cleanup-log"
        every { frontCache.close() } throws IllegalStateException("front unavailable")
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(cacheName = cacheName, isSynchronous = true),
        )
        val logger = NearJCache.log as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.TRACE
        logger.addAppender(appender)
        try {
            assertFailsWith<IllegalStateException> { nearCache.close() }
            appender.list.map { it.formattedMessage }.any { message ->
                message.contains("operation=close") &&
                        message.contains("cache=$cacheName") &&
                        message.contains("provider=")
            }.shouldBeTrue()
        } finally {
            logger.detachAppender(appender)
            appender.stop()
            logger.level = previousLevel
        }
    }

    @Test
    fun `listener registration failure 로그는 operation cache provider metadata를 기록한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val cacheName = "near-jcache-registration-log"
        val failure = IllegalStateException("listener unavailable")
        every { backCache.registerCacheEntryListener(any()) } throws failure
        val nearCache = NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = NearJCacheConfig(cacheName = cacheName, isSynchronous = true),
        )
        val logger = NearJCache.log as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.TRACE
        logger.addAppender(appender)
        try {
            assertFailsWith<IllegalStateException> { nearCache.registerBackCacheListener() }
            appender.list.map { it.formattedMessage }.any { message ->
                message.contains("operation=register") &&
                        message.contains("cache=$cacheName") &&
                        message.contains("provider=")
            }.shouldBeTrue()
        } finally {
            logger.detachAppender(appender)
            appender.stop()
            logger.level = previousLevel
        }
    }

    @Test
    fun `명시적 close는 listener와 front cleanup failure를 모두 보존한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val listenerFailure = IllegalStateException("listener close failed")
        val frontFailure = IllegalArgumentException("front close failed")
        every { backCache.registerCacheEntryListener(any()) } returns Unit
        every { backCache.deregisterCacheEntryListener(any()) } throws listenerFailure
        every { frontCache.close() } throws frontFailure
        val nearCache = newNearCache(frontCache, backCache)
        nearCache.registerBackCacheListener()

        val error = assertFailsWith<IllegalStateException> { nearCache.close() }

        (error === listenerFailure).shouldBeTrue()
        error.suppressed.single() shouldBeEqualTo frontFailure
        verify(exactly = 1) { frontCache.close() }
    }

    @Test
    fun `성공한 close는 중복 호출해도 front와 listener를 한 번만 정리한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.registerCacheEntryListener(any()) } returns Unit
        every { backCache.deregisterCacheEntryListener(any()) } returns Unit
        every { frontCache.close() } returns Unit
        val nearCache = newNearCache(frontCache, backCache)
        nearCache.registerBackCacheListener()

        nearCache.close()
        nearCache.close()

        verify(exactly = 1) { backCache.deregisterCacheEntryListener(any()) }
        verify(exactly = 1) { frontCache.close() }
    }

    @Test
    fun `listener cleanup failure 후 close는 다음 호출에서 listener 정리를 재시도한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val failure = IllegalStateException("listener close failed")
        val deregisterAttempts = AtomicInteger()
        every { backCache.registerCacheEntryListener(any()) } returns Unit
        every { backCache.deregisterCacheEntryListener(any()) } answers {
            if (deregisterAttempts.incrementAndGet() == 1) {
                throw failure
            }
        }
        every { frontCache.close() } returns Unit
        val nearCache = newNearCache(frontCache, backCache)
        nearCache.registerBackCacheListener()

        assertFailsWith<IllegalStateException> { nearCache.close() }
        nearCache.close()

        verify(exactly = 2) { backCache.deregisterCacheEntryListener(any()) }
        verify(exactly = 1) { frontCache.close() }
    }

    @Test
    fun `listener와 front cleanup이 함께 실패해도 다음 close에서 둘 다 재시도한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val listenerFailure = IllegalStateException("listener close failed")
        val frontFailure = IllegalArgumentException("front close failed")
        val deregisterAttempts = AtomicInteger()
        val closeAttempts = AtomicInteger()
        every { backCache.registerCacheEntryListener(any()) } returns Unit
        every { backCache.deregisterCacheEntryListener(any()) } answers {
            if (deregisterAttempts.incrementAndGet() == 1) {
                throw listenerFailure
            }
        }
        every { frontCache.close() } answers {
            if (closeAttempts.incrementAndGet() == 1) {
                throw frontFailure
            }
        }
        val nearCache = newNearCache(frontCache, backCache)
        nearCache.registerBackCacheListener()

        val error = assertFailsWith<IllegalStateException> { nearCache.close() }
        (error === listenerFailure).shouldBeTrue()
        error.suppressed.single() shouldBeEqualTo frontFailure

        nearCache.close()

        verify(exactly = 2) { backCache.deregisterCacheEntryListener(any()) }
        verify(exactly = 2) { frontCache.close() }
    }

    @Test
    fun `close 이후 listener 재등록은 거부한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val nearCache = newNearCache(frontCache, backCache)

        nearCache.close()

        assertFailsWith<IllegalStateException> { nearCache.registerBackCacheListener() }
        verify(exactly = 0) { backCache.registerCacheEntryListener(any()) }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `생성 rollback은 listener primary failure와 front cleanup failure를 함께 보존한다`() {
        val cacheName = "near-jcache-construction-failure"
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val frontCacheManager = mockk<CacheManager>()
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val listenerFailure = IllegalStateException("listener registration failed")
        val cleanupFailure = IllegalArgumentException("front close failed")
        val configuration = MutableConfiguration<String, String>().setStoreByValue(false)
        val configurationClass = Configuration::class.java as Class<Configuration<String, String>>
        every { frontCache.getConfiguration(configurationClass) } returns configuration
        every {
            frontCacheManager.createCache<String, String, MutableConfiguration<String, String>>(cacheName, any())
        } returns frontCache
        every { backCache.registerCacheEntryListener(any()) } throws listenerFailure
        every { frontCache.close() } throws cleanupFailure

        val nearConfig = NearJCacheConfig<String, String>(
            cacheManagerFactory = Factory { frontCacheManager },
            cacheName = cacheName,
            isSynchronous = true,
        )

        val error = assertFailsWith<IllegalStateException> { NearJCache(nearConfig, backCache) }

        (error === listenerFailure).shouldBeTrue()
        error.suppressed.single() shouldBeEqualTo cleanupFailure
        verify(exactly = 1) { frontCache.close() }
    }

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
    fun `표준 Cache getAll은 bounded opt-in에서 front와 back 결과를 병합하고 populate한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.getAll(setOf("front", "remote")) } returns
                mutableMapOf("front" to "front-value")
        every { backCache.getAll(setOf("remote")) } returns
                mutableMapOf("remote" to "back-value")
        val standardCache: Cache<String, String> = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(1),
            ),
        )

        val result = standardCache.getAll(setOf("front", "remote"))

        result shouldBeEqualTo mapOf("front" to "front-value", "remote" to "back-value")
        verify(exactly = 1) { backCache.getAll(setOf("remote")) }
        verify(exactly = 1) { frontCache.putAll(mapOf("remote" to "back-value")) }
    }

    @Test
    fun `getAll 기본 정책은 back hit를 반환하고 front populate를 우회한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.getAll(setOf("front", "remote")) } returns
                mutableMapOf("front" to "front-value")
        every { backCache.getAll(setOf("remote")) } returns
                mutableMapOf("remote" to "back-value")
        val cache = newNearCache(frontCache, backCache)

        cache.getAll(setOf("front", "remote")) shouldBeEqualTo
                mapOf("front" to "front-value", "remote" to "back-value")

        verify(exactly = 1) { backCache.getAll(setOf("remote")) }
        verify(exactly = 0) { frontCache.putAll(any()) }
    }

    @Test
    fun `bounded policy는 back hit 수가 상한 이하이면 batch 전체를 populate한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val backValues = mutableMapOf("a" to "1", "b" to "2")
        every { frontCache.getAll(backValues.keys) } returns mutableMapOf()
        every { backCache.getAll(backValues.keys) } returns backValues
        val cache = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2),
            ),
        )

        cache.getAll(backValues.keys) shouldBeEqualTo backValues

        verify(exactly = 1) { frontCache.putAll(backValues) }
    }

    @Test
    fun `bounded policy는 back hit 수가 상한을 넘으면 batch 전체 populate를 우회한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val backValues = mutableMapOf("a" to "1", "b" to "2", "c" to "3")
        every { frontCache.getAll(backValues.keys) } returns mutableMapOf()
        every { backCache.getAll(backValues.keys) } returns backValues
        val cache = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2),
            ),
        )

        cache.getAll(backValues.keys) shouldBeEqualTo backValues

        verify(exactly = 0) { frontCache.putAll(any()) }
        verify(exactly = 0) { frontCache.put(any(), any()) }
    }

    @Test
    fun `bounded policy는 요청 수가 아니라 실제 back hit 수를 기준으로 populate한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val keys = (1..100).map { "key-$it" }.toSet()
        val backValues = mutableMapOf("key-1" to "one", "key-100" to "hundred")
        every { frontCache.getAll(keys) } returns mutableMapOf()
        every { backCache.getAll(keys) } returns backValues
        val cache = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2),
            ),
        )

        cache.getAll(keys) shouldBeEqualTo backValues

        verify(exactly = 1) { frontCache.putAll(backValues) }
    }

    @Test
    fun `bounded policy는 value byte 크기를 추정하지 않고 entry 수만 사용한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val keys = setOf("large")
        val largeValue = "x".repeat(1_000_000)
        val backValues = mutableMapOf("large" to largeValue)
        every { frontCache.getAll(keys) } returns mutableMapOf()
        every { backCache.getAll(keys) } returns backValues
        val cache = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(1),
            ),
        )

        cache.getAll(keys) shouldBeEqualTo backValues

        verify(exactly = 1) { frontCache.putAll(backValues) }
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
    fun `실제 Caffeine cache에서 기본 bypass는 반복 read마다 back을 조회한다`() {
        val configuration = MutableConfiguration<String, String>()
            .setTypes(String::class.java, String::class.java)
            .setStoreByValue(false)
        val frontCache = JCaching.Caffeine.getOrCreate<String, String>(
            "issue-1369-bypass-front-${UUID.randomUUID()}",
            configuration,
        )
        val backCache = spyk(
            JCaching.Caffeine.getOrCreate<String, String>(
                "issue-1369-bypass-back-${UUID.randomUUID()}",
                configuration,
            ),
        )
        val keys = setOf("one", "two")
        val values = mapOf("one" to "1", "two" to "2")
        backCache.putAll(values)
        val cache = newNearCache(frontCache, backCache)

        try {
            cache.getAll(keys) shouldBeEqualTo values
            cache.getAll(keys) shouldBeEqualTo values

            verify(exactly = 2) { backCache.getAll(keys) }
            frontCache.getAll(keys) shouldBeEqualTo emptyMap()
        } finally {
            cache.close()
            backCache.close()
        }
    }

    @Test
    fun `실제 Caffeine cache에서 bounded in-range는 첫 read 이후 front hit를 사용한다`() {
        val configuration = MutableConfiguration<String, String>()
            .setTypes(String::class.java, String::class.java)
            .setStoreByValue(false)
        val frontCache = JCaching.Caffeine.getOrCreate<String, String>(
            "issue-1369-bounded-front-${UUID.randomUUID()}",
            configuration,
        )
        val backCache = spyk(
            JCaching.Caffeine.getOrCreate<String, String>(
                "issue-1369-bounded-back-${UUID.randomUUID()}",
                configuration,
            ),
        )
        val keys = setOf("one", "two")
        val values = mapOf("one" to "1", "two" to "2")
        backCache.putAll(values)
        val cache = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                frontCacheConfiguration = configuration,
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(2),
            ),
        )

        try {
            cache.getAll(keys) shouldBeEqualTo values
            cache.getAll(keys) shouldBeEqualTo values

            verify(exactly = 1) { backCache.getAll(keys) }
            frontCache.getAll(keys) shouldBeEqualTo values
        } finally {
            cache.close()
            backCache.close()
        }
    }

    @Test
    fun `empty와 전체 back miss getAll은 빈 결과와 front no-op을 유지한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val keys = setOf("missing")
        every { frontCache.getAll(keys) } returns mutableMapOf()
        every { backCache.getAll(keys) } returns mutableMapOf()
        val cache = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(1),
            ),
        )

        cache.getAll(emptySet()) shouldBeEqualTo emptyMap()
        cache.getAll(keys) shouldBeEqualTo emptyMap()

        verify(exactly = 1) { frontCache.getAll(keys) }
        verify(exactly = 1) { backCache.getAll(keys) }
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
        val nearCache = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(10),
            ),
        )
        val result = arrayOfNulls<MutableMap<String, String>>(1)
        val reader = virtualThread(start = false, name = "near-jcache-stale-get-all") {
            result[0] = nearCache.getAll(setOf("key"))
        }

        try {
            reader.start()
            readStarted.await(2, TimeUnit.SECONDS).shouldBeTrue()
            nearCache.put("key", "fresh")
        } finally {
            releaseRead.countDown()
            reader.join(2_000)
        }

        reader.isAlive.shouldBeFalse()
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
    fun `listener update가 read-through 중간에 완료되면 stale 값을 front에 넣지 않는다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val readStarted = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val listenerConfiguration = slot<CacheEntryListenerConfiguration<String, String>>()
        every { frontCache.get("key") } returns null
        every { backCache.get("key") } answers {
            readStarted.countDown()
            releaseRead.await(2, TimeUnit.SECONDS)
            "stale"
        }
        every { backCache.registerCacheEntryListener(capture(listenerConfiguration)) } returns Unit
        val nearCache = newNearCache(frontCache, backCache)
        nearCache.registerBackCacheListener()
        val listener = listenerConfiguration.captured.cacheEntryListenerFactory.create()
                as CacheEntryUpdatedListener<String, String>
        val event = mockk<CacheEntryEvent<String, String>>(relaxed = true)
        every { event.key } returns "key"
        every { event.value } returns "fresh-from-listener"
        val result = arrayOfNulls<String>(1)
        val reader = virtualThread(start = false, name = "near-jcache-listener-read") {
            result[0] = nearCache.get("key")
        }

        reader.start()
        readStarted.await(2, TimeUnit.SECONDS).shouldBeTrue()
        listener.onUpdated(listOf(event))
        releaseRead.countDown()
        reader.join(2_000)

        result[0] shouldBeEqualTo "stale"
        verify(exactly = 1) { frontCache.putAll(mapOf("key" to "fresh-from-listener")) }
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
    fun `front populate CancellationException은 호출자에게 재전파한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val failure = CancellationException("caller cancelled")
        every { frontCache.get("key") } returns null
        every { backCache.get("key") } returns "value"
        every { frontCache.put("key", "value") } throws failure
        val nearCache = newNearCache(frontCache, backCache)

        assertFailsWith<CancellationException> { nearCache.get("key") }.message shouldBeEqualTo failure.message
    }

    @Test
    fun `getAll front RuntimeException은 결과를 유지하고 retry 없이 gate를 해제하며 로그를 정제한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val keys = setOf("secret-key")
        val backValues = mutableMapOf("secret-key" to "secret-value")
        val cacheName = "tenant-a\nsecret-cache"
        val attempts = AtomicInteger()
        every { frontCache.getAll(keys) } returns mutableMapOf()
        every { backCache.getAll(keys) } returns backValues
        every { frontCache.putAll(backValues) } answers {
            if (attempts.incrementAndGet() == 1) {
                throw IllegalStateException("secret-key secret-value")
            }
        }
        val nearCache = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                cacheName = cacheName,
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(1),
            ),
        )
        val logger = NearJCache.log as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.WARN
        logger.addAppender(appender)
        try {
            nearCache.getAll(keys) shouldBeEqualTo backValues
            verify(exactly = 1) { frontCache.putAll(backValues) }

            val secondResult = arrayOfNulls<MutableMap<String, String>>(1)
            val secondReader = virtualThread(start = false, name = "near-jcache-get-all-runtime-recovery") {
                secondResult[0] = nearCache.getAll(keys)
            }
            secondReader.start()
            secondReader.join(2_000)

            secondReader.isAlive.shouldBeFalse()
            secondResult[0] shouldBeEqualTo backValues
            verify(exactly = 2) { frontCache.putAll(backValues) }
            val warning = appender.list.single { it.formattedMessage.contains("operation=getAll") }
            warning.formattedMessage.contains(cacheName).shouldBeFalse()
            warning.formattedMessage.contains("tenant-a").shouldBeFalse()
            warning.formattedMessage.contains("secret-key").shouldBeFalse()
            warning.formattedMessage.contains("secret-value").shouldBeFalse()
            (warning.throwableProxy == null).shouldBeTrue()
        } finally {
            logger.detachAppender(appender)
            appender.stop()
            logger.level = previousLevel
        }
    }

    @Test
    fun `getAll front populate CancellationException은 identity를 보존하고 gate를 해제한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val failure = CancellationException("caller cancelled")
        val attempts = AtomicInteger()
        every { frontCache.getAll(setOf("key")) } returns mutableMapOf()
        every { backCache.getAll(setOf("key")) } returns mutableMapOf("key" to "value")
        every { frontCache.putAll(mapOf("key" to "value")) } answers {
            if (attempts.incrementAndGet() == 1) throw failure
        }
        val nearCache = newNearCache(
            frontCache,
            backCache,
            NearJCacheConfig(
                isSynchronous = true,
                bulkFrontPopulationPolicy = BulkFrontPopulationPolicy.PopulateIfAtMost(1),
            ),
        )

        val thrown = assertFailsWith<CancellationException> {
            nearCache.getAll(setOf("key"))
        }
        assertSame(failure, thrown)

        val secondResult = arrayOfNulls<MutableMap<String, String>>(1)
        val secondReader = virtualThread(start = false, name = "near-jcache-get-all-cancellation-recovery") {
            secondResult[0] = nearCache.getAll(setOf("key"))
        }
        secondReader.start()
        secondReader.join(2_000)

        secondReader.isAlive.shouldBeFalse()
        secondResult[0] shouldBeEqualTo mapOf("key" to "value")
        verify(exactly = 2) { frontCache.putAll(mapOf("key" to "value")) }
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
    fun `front populate 로그에는 raw key와 value를 포함하지 않는다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        every { frontCache.name } returns "front-cache"
        val secretKey = "secret-key-should-not-log"
        val secretValue = "secret-value-should-not-log"
        val event = mockk<CacheEntryEvent<String, String>>(relaxed = true)
        every { event.key } returns secretKey
        every { event.value } returns secretValue
        val logger = JCacheEntryEventListener.log as Logger
        val previousLevel = logger.level
        val appender = ListAppender<ILoggingEvent>().apply { start() }
        logger.level = Level.TRACE
        logger.addAppender(appender)
        try {
            JCacheEntryEventListener(frontCache).onCreated(listOf(event))
            appender.list.map { it.formattedMessage }.all { message ->
                !message.contains(secretKey) && !message.contains(secretValue)
            }.shouldBeTrue()
        } finally {
            logger.detachAppender(appender)
            appender.stop()
            logger.level = previousLevel
        }
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
    fun `clear primary failure는 listener 재등록 failure를 suppressed로 보존한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val primaryFailure = IllegalStateException("front unavailable")
        val registrationFailure = IllegalArgumentException("listener unavailable")
        val registrationCount = AtomicInteger()
        every { backCache.registerCacheEntryListener(any()) } answers {
            if (registrationCount.incrementAndGet() == 2) throw registrationFailure
        }
        every { frontCache.clear() } throws primaryFailure
        val nearCache = newNearCache(frontCache, backCache)
        nearCache.registerBackCacheListener()

        val error = assertFailsWith<IllegalStateException> { nearCache.clear() }

        (error === primaryFailure).shouldBeTrue()
        error.suppressed.single() shouldBeEqualTo registrationFailure
        verify(exactly = 0) { backCache.clear() }
    }

    @Test
    fun `clear 성공 후 listener 재등록 failure는 호출자에게 전달한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val registrationFailure = IllegalArgumentException("listener unavailable")
        val registrationCount = AtomicInteger()
        every { backCache.registerCacheEntryListener(any()) } answers {
            if (registrationCount.incrementAndGet() == 2) throw registrationFailure
        }
        val nearCache = newNearCache(frontCache, backCache)
        nearCache.registerBackCacheListener()

        val error = assertFailsWith<IllegalArgumentException> { nearCache.clear() }

        (error === registrationFailure).shouldBeTrue()
        verify(exactly = 1) { frontCache.clear() }
        verify(exactly = 1) { backCache.clear() }
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
    fun `실제 front cache가 store by value면 설정과 무관하게 생성 단계에서 거부한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val actualConfiguration = MutableConfiguration<String, String>().setStoreByValue(true)
        @Suppress("UNCHECKED_CAST")
        val configurationType = Configuration::class.java as Class<Configuration<String, String>>
        every { frontCache.getConfiguration(configurationType) } returns actualConfiguration

        assertFailsWith<IllegalArgumentException> {
            NearJCache(
                frontCache = frontCache,
                backCache = backCache,
                config = NearJCacheConfig(isSynchronous = true),
            )
        }
    }

    @Test
    fun `clear 이후 지연된 이전 listener event는 front를 재점유하지 않는다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        val listenerConfiguration = slot<CacheEntryListenerConfiguration<String, String>>()
        every { backCache.registerCacheEntryListener(capture(listenerConfiguration)) } returns Unit
        val nearCache = newNearCache(frontCache, backCache)
        nearCache.registerBackCacheListener()
        val oldListener = listenerConfiguration.captured.cacheEntryListenerFactory.create()
                as CacheEntryCreatedListener<String, String>
        val event = mockk<CacheEntryEvent<String, String>>(relaxed = true)
        every { event.key } returns "stale-key"
        every { event.value } returns "stale-value"

        nearCache.clear()
        oldListener.onCreated(listOf(event))

        verify(exactly = 0) { frontCache.putAll(any()) }
        verify(exactly = 1) { backCache.deregisterCacheEntryListener(any()) }
        verify(exactly = 2) { backCache.registerCacheEntryListener(any()) }
    }

    @Test
    fun `compound operation은 back 원자 연산 후 front를 동기화한다`() {
        val frontCache = mockk<JCache<String, String>>(relaxed = true)
        val backCache = mockk<JCache<String, String>>(relaxed = true)
        every { backCache.getAndRemove("remove") } returns "remove-value"
        every { backCache.getAndReplace("replace", "new-value") } returns "old-value"
        val nearCache = newNearCache(frontCache, backCache)

        nearCache.getAndRemove("remove") shouldBeEqualTo "remove-value"
        nearCache.getAndReplace("replace", "new-value") shouldBeEqualTo "old-value"

        verify(exactly = 1) { backCache.getAndRemove("remove") }
        verify(exactly = 1) { backCache.getAndReplace("replace", "new-value") }
        verify(exactly = 1) { frontCache.remove("remove") }
        verify(exactly = 1) { frontCache.put("replace", "new-value") }
    }

    private fun newNearCache(
        frontCache: JCache<String, String>,
        backCache: JCache<String, String>,
        config: NearJCacheConfig<String, String> = NearJCacheConfig(isSynchronous = true),
    ): NearJCache<String, String> =
        NearJCache(
            frontCache = frontCache,
            backCache = backCache,
            config = config,
        )
}
