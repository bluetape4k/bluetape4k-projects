package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.jcacheConfiguration
import io.bluetape4k.concurrent.virtualthread.virtualThread
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.idgenerators.uuid.Uuid
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.logging.KLogging
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.cache.Cache
import javax.cache.expiry.EternalExpiryPolicy
import kotlin.time.Duration.Companion.seconds

/**
 * [ResilientNearJCache] (JCache 기반 back cache) 동기(Blocking) 구현 테스트.
 *
 * write-behind + retry + graceful degradation 패턴을 검증한다.
 * back cache 반영은 비동기이므로 awaitility로 폴링한다.
 */
class ResilientNearJCacheTest {
    companion object: KLogging() {
        const val REPEAT_SIZE = 3

        private fun randomKey(): String = Uuid.V7.nextIdAsString()
    }

    private val backCache =
        JCaching.Caffeine.getOrCreate<String, String>(
            name = "resilient-near-back-" + randomKey(),
            configuration =
                jcacheConfiguration {
                    setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
                }
        )

    private lateinit var cache: ResilientNearJCache<String, String>

    @BeforeEach
    fun createCache() {
        cache =
            ResilientNearJCache(
                backCache = backCache,
                config =
                    ResilientNearJCacheConfig(
                        retryMaxAttempts = 2,
                        retryWaitDuration = java.time.Duration.ofMillis(100)
                    )
            )
    }

    @AfterEach
    fun tearDown() {
        runCatching { cache.close() }
    }

    @Test
    fun `get - 존재하지 않는 키는 null 반환`() {
        cache.get("missing-key").shouldBeNull()
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `put and get - front cache 즉시 반영`() {
        cache.put("key1", "value1")
        cache.get("key1") shouldBeEqualTo "value1"
    }

    @Test
    fun `put - write-behind - 잠시 후 back cache에도 반영됨`() {
        cache.put("wb-key", "wb-val")
        // front cache 즉시 확인
        cache.get("wb-key") shouldBeEqualTo "wb-val"
        // back cache는 write-behind로 비동기 반영 → awaitility 폴링
        await atMost 5.seconds until { backCache.get("wb-key") != null }
        backCache.get("wb-key") shouldBeEqualTo "wb-val"
    }

    @Test
    fun `put - full write queue fails before front update`() {
        val putStarted = CountDownLatch(1)
        val releasePut = CountDownLatch(1)
        val blockingBackCache = mockk<Cache<String, String>>(relaxed = true)
        every { blockingBackCache.put(any(), any()) } answers {
            putStarted.countDown()
            releasePut.await(5, TimeUnit.SECONDS).shouldBeTrue()
        }
        every { blockingBackCache.get("third") } returns null
        val smallQueueCache = ResilientNearJCache(
            backCache = blockingBackCache,
            config = ResilientNearJCacheConfig(
                writeQueueCapacity = 1,
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(10),
            )
        )

        try {
            smallQueueCache.put("first", "1")
            putStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            smallQueueCache.put("second", "2")

            assertFailsWith<IllegalStateException> {
                smallQueueCache.put("third", "3")
            }
            smallQueueCache.get("third").shouldBeNull()
        } finally {
            releasePut.countDown()
            smallQueueCache.close()
        }
    }

    @Test
    fun `put - retry exhaustion invalidates uncommitted front value`() {
        val failingCache = resilientCacheWithFailingBackCache { backCache ->
            every { backCache.get("put-key") } returns "back-value"
            every { backCache.put("put-key", "front-value") } throws IllegalStateException("put failed")
        }

        try {
            failingCache.get("put-key") shouldBeEqualTo "back-value"
            failingCache.put("put-key", "front-value")

            await atMost 5.seconds until { failingCache.get("put-key") == "back-value" }
        } finally {
            failingCache.close()
        }
    }

    @Test
    fun `get - front miss 시 back cache에서 읽어 front populate`() {
        backCache.put("remote-key", "remote-val")
        cache.get("remote-key") shouldBeEqualTo "remote-val"
    }

    @Test
    fun `get - concurrent put prevents stale read-through population`() {
        val readStarted = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val staleBackCache = mockk<Cache<String, String>>(relaxed = true)
        every { staleBackCache.get("shared") } answers {
            readStarted.countDown()
            releaseRead.await(5, TimeUnit.SECONDS).shouldBeTrue()
            "stale"
        }
        val staleReadCache = ResilientNearJCache(
            backCache = staleBackCache,
            config = ResilientNearJCacheConfig(
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(10),
            )
        )
        val readResult = AtomicReference<String?>()

        try {
            val reader = virtualThread(name = "near-cache-stale-read") {
                readResult.set(staleReadCache.get("shared"))
            }
            readStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            staleReadCache.put("shared", "latest")
            releaseRead.countDown()
            reader.join(5_000)

            readResult.get() shouldBeEqualTo "stale"
            staleReadCache.get("shared") shouldBeEqualTo "latest"
        } finally {
            releaseRead.countDown()
            staleReadCache.close()
        }
    }

    @Test
    fun `get - concurrent replace prevents stale read-through population`() {
        val readStarted = CountDownLatch(1)
        val releaseRead = CountDownLatch(1)
        val staleBackCache = mockk<Cache<String, String>>(relaxed = true)
        every { staleBackCache.get("shared") } answers {
            readStarted.countDown()
            releaseRead.await(5, TimeUnit.SECONDS).shouldBeTrue()
            "stale"
        }
        every { staleBackCache.containsKey("shared") } returns true
        every { staleBackCache.replace("shared", "latest") } returns true
        val staleReadCache = ResilientNearJCache(
            backCache = staleBackCache,
            config = ResilientNearJCacheConfig(
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(10),
            )
        )
        val readResult = AtomicReference<String?>()

        try {
            val reader = virtualThread(name = "near-cache-stale-replace-read") {
                readResult.set(staleReadCache.get("shared"))
            }
            readStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            staleReadCache.replace("shared", "latest").shouldBeTrue()
            releaseRead.countDown()
            reader.join(5_000)

            readResult.get() shouldBeEqualTo "stale"
            staleReadCache.get("shared") shouldBeEqualTo "latest"
        } finally {
            releaseRead.countDown()
            staleReadCache.close()
        }
    }

    @Test
    fun `putAll and getAll`() {
        val data = mapOf("a" to "1", "b" to "2", "c" to "3")
        cache.putAll(data)
        val result = cache.getAll(setOf("a", "b", "c", "x"))
        result["a"] shouldBeEqualTo "1"
        result["b"] shouldBeEqualTo "2"
        result["c"] shouldBeEqualTo "3"
        result["x"].shouldBeNull()
    }

    @Test
    fun `putAll - snapshots mutable entries before enqueue`() {
        val firstPutStarted = CountDownLatch(1)
        val releaseFirstPut = CountDownLatch(1)
        val nextPutStored = CountDownLatch(1)
        val capturedEntries = slot<Map<String, String>>()
        val mutableBackCache = mockk<Cache<String, String>>(relaxed = true)
        every { mutableBackCache.put("block", "value") } answers {
            firstPutStarted.countDown()
            releaseFirstPut.await(5, TimeUnit.SECONDS).shouldBeTrue()
        }
        every { mutableBackCache.putAll(capture(capturedEntries)) } answers { }
        every { mutableBackCache.put("next", "value") } answers { nextPutStored.countDown() }
        val mutableEntriesCache = ResilientNearJCache(
            backCache = mutableBackCache,
            config = ResilientNearJCacheConfig(
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(10),
            )
        )

        try {
            mutableEntriesCache.put("block", "value")
            firstPutStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            val entries = mutableMapOf("initial" to "value")
            mutableEntriesCache.putAll(entries)
            entries["late"] = "value"
            releaseFirstPut.countDown()

            mutableEntriesCache.put("next", "value")
            nextPutStored.await(5, TimeUnit.SECONDS).shouldBeTrue()
            capturedEntries.captured shouldBeEqualTo mapOf("initial" to "value")
        } finally {
            releaseFirstPut.countDown()
            mutableEntriesCache.close()
        }
    }

    @Test
    fun `removeAll - snapshots mutable keys before enqueue`() {
        val firstPutStarted = CountDownLatch(1)
        val releaseFirstPut = CountDownLatch(1)
        val drainCompleted = CountDownLatch(1)
        val removedKeys = ConcurrentHashMap.newKeySet<String>()
        val mutableBackCache = mockk<Cache<String, String>>(relaxed = true)
        every { mutableBackCache.put("block", "value") } answers {
            firstPutStarted.countDown()
            releaseFirstPut.await(5, TimeUnit.SECONDS).shouldBeTrue()
        }
        every { mutableBackCache.put("marker", "done") } answers { drainCompleted.countDown() }
        every { mutableBackCache.remove(any()) } answers {
            removedKeys.add(firstArg())
            true
        }
        val mutableKeysCache = ResilientNearJCache(
            backCache = mutableBackCache,
            config = ResilientNearJCacheConfig(
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(10),
            )
        )

        try {
            mutableKeysCache.put("block", "value")
            firstPutStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            val keys = mutableSetOf("initial")
            mutableKeysCache.removeAll(keys)
            keys.add("late")
            releaseFirstPut.countDown()

            mutableKeysCache.put("marker", "done")
            drainCompleted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            removedKeys shouldBeEqualTo setOf("initial")
        } finally {
            releaseFirstPut.countDown()
            mutableKeysCache.close()
        }
    }

    @Test
    fun `MultithreadingTester - stale completions preserve latest accepted state`() {
        val backValue = AtomicReference<String?>(null)
        val valueSequence = AtomicInteger()
        val operationSequence = AtomicInteger()
        val drainCompleted = CountDownLatch(1)
        val concurrentBackCache = mockk<Cache<String, String>>(relaxed = true)
        every { concurrentBackCache.get("shared") } answers { backValue.get() }
        every { concurrentBackCache.put("marker", "done") } answers { drainCompleted.countDown() }
        every { concurrentBackCache.put("shared", any()) } answers {
            if (operationSequence.incrementAndGet() % 4 == 0) throw IllegalStateException("put failed")
            backValue.set(secondArg())
        }
        every { concurrentBackCache.remove("shared") } answers {
            if (operationSequence.incrementAndGet() % 4 == 0) throw IllegalStateException("remove failed")
            backValue.set(null)
            true
        }
        every { concurrentBackCache.clear() } answers {
            if (operationSequence.incrementAndGet() % 4 == 0) throw IllegalStateException("clear failed")
            backValue.set(null)
        }
        val concurrentCache = ResilientNearJCache(
            backCache = concurrentBackCache,
            config = ResilientNearJCacheConfig(
                writeQueueCapacity = 2_048,
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(1),
            )
        )

        try {
            MultithreadingTester()
                .workers(8)
                .rounds(32)
                .add { concurrentCache.put("shared", "value-${valueSequence.incrementAndGet()}") }
                .add { concurrentCache.remove("shared") }
                .add { concurrentCache.clearAll() }
                .run()

            concurrentCache.put("marker", "done")
            drainCompleted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            concurrentCache.get("shared") shouldBeEqualTo backValue.get()
        } finally {
            concurrentCache.close()
        }
    }

    @Test
    fun `remove - front 즉시 삭제, back write-behind`() {
        backCache.put("rm-key", "rm-val")
        cache.get("rm-key") shouldBeEqualTo "rm-val"

        cache.remove("rm-key")
        cache.get("rm-key").shouldBeNull()

        // back cache에서도 삭제되길 대기
        await atMost 5.seconds until { backCache.get("rm-key") == null }
    }

    @Test
    fun `remove - retry exhaustion releases tombstone`() {
        val failingCache = resilientCacheWithFailingBackCache { backCache ->
            every { backCache.get("remove-key") } returns "back-value"
            every { backCache.remove("remove-key") } throws IllegalStateException("remove failed")
        }

        try {
            failingCache.get("remove-key") shouldBeEqualTo "back-value"
            failingCache.remove("remove-key")

            await atMost 5.seconds until { failingCache.get("remove-key") == "back-value" }
        } finally {
            failingCache.close()
        }
    }

    @Test
    fun `removeAll - 여러 키 삭제`() {
        cache.putAll(mapOf("a" to "1", "b" to "2", "c" to "3"))
        cache.removeAll(setOf("a", "b"))
        cache.get("a").shouldBeNull()
        cache.get("b").shouldBeNull()
        cache.get("c") shouldBeEqualTo "3"
    }

    @Test
    fun `containsKey - 키 존재 여부 확인`() {
        cache.put("keyX", "valX")
        cache.containsKey("keyX").shouldBeTrue()
        cache.containsKey("nonexistent").shouldBeFalse()
        cache.remove("keyX")
        cache.containsKey("keyX").shouldBeFalse()
    }

    @Test
    fun `putIfAbsent - 캐시 값 없으면 추가, 있으면 기존 값 반환`() {
        cache.putIfAbsent("key", "first").shouldBeNull()
        cache.get("key") shouldBeEqualTo "first"
        cache.putIfAbsent("key", "second") shouldBeEqualTo "first"
        cache.get("key") shouldBeEqualTo "first"
    }

    @Test
    fun `putIfAbsent - queued remove preserves mutation order`() {
        val removeStarted = CountDownLatch(1)
        val releaseRemove = CountDownLatch(1)
        val putApplied = CountDownLatch(1)
        val orderedBackCache = mockk<Cache<String, String>>(relaxed = true)
        every { orderedBackCache.get("key") } returns "old"
        every { orderedBackCache.remove("key") } answers {
            removeStarted.countDown()
            releaseRemove.await(5, TimeUnit.SECONDS).shouldBeTrue()
            true
        }
        every { orderedBackCache.put("key", "new") } answers { putApplied.countDown() }
        val orderedCache = ResilientNearJCache(orderedBackCache)

        try {
            orderedCache.get("key") shouldBeEqualTo "old"
            orderedCache.remove("key")
            removeStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()

            orderedCache.putIfAbsent("key", "new").shouldBeNull()
            orderedCache.get("key") shouldBeEqualTo "new"
            releaseRemove.countDown()
            putApplied.await(5, TimeUnit.SECONDS).shouldBeTrue()
            orderedCache.get("key") shouldBeEqualTo "new"
        } finally {
            releaseRemove.countDown()
            orderedCache.close()
        }
    }

    @Test
    fun `putIfAbsent - queued clear preserves mutation order`() {
        val clearStarted = CountDownLatch(1)
        val releaseClear = CountDownLatch(1)
        val putApplied = CountDownLatch(1)
        val orderedBackCache = mockk<Cache<String, String>>(relaxed = true)
        every { orderedBackCache.clear() } answers {
            clearStarted.countDown()
            releaseClear.await(5, TimeUnit.SECONDS).shouldBeTrue()
        }
        every { orderedBackCache.put("key", "new") } answers { putApplied.countDown() }
        val orderedCache = ResilientNearJCache(orderedBackCache)

        try {
            orderedCache.clearAll()
            clearStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()

            orderedCache.putIfAbsent("key", "new").shouldBeNull()
            orderedCache.get("key").shouldBeNull()
            releaseClear.countDown()
            putApplied.await(5, TimeUnit.SECONDS).shouldBeTrue()
            await atMost 5.seconds until { orderedCache.get("key") == "new" }
        } finally {
            releaseClear.countDown()
            orderedCache.close()
        }
    }

    @Test
    fun `putIfAbsent - newer clear supersedes pending put`() {
        val oldPutStarted = CountDownLatch(1)
        val releaseOldPut = CountDownLatch(1)
        val clearApplied = CountDownLatch(1)
        val newPutApplied = CountDownLatch(1)
        val orderedBackCache = mockk<Cache<String, String>>(relaxed = true)
        every { orderedBackCache.put("key", "old") } answers {
            oldPutStarted.countDown()
            releaseOldPut.await(5, TimeUnit.SECONDS).shouldBeTrue()
        }
        every { orderedBackCache.clear() } answers { clearApplied.countDown() }
        every { orderedBackCache.put("key", "new") } answers { newPutApplied.countDown() }
        val orderedCache = ResilientNearJCache(orderedBackCache)

        try {
            orderedCache.put("key", "old")
            oldPutStarted.await(5, TimeUnit.SECONDS).shouldBeTrue()
            orderedCache.clearAll()

            orderedCache.putIfAbsent("key", "new").shouldBeNull()
            releaseOldPut.countDown()
            clearApplied.await(5, TimeUnit.SECONDS).shouldBeTrue()
            newPutApplied.await(5, TimeUnit.SECONDS).shouldBeTrue()
            await atMost 5.seconds until { orderedCache.get("key") == "new" }
        } finally {
            releaseOldPut.countDown()
            orderedCache.close()
        }
    }

    @Test
    fun `replace - 키가 존재할 때만 교체`() {
        cache.replace("noKey", "val").shouldBeFalse()
        cache.put("key", "old")

        // write-behind 완료 대기 (replace는 back cache 직접 호출)
        await atMost 5.seconds until { backCache.get("key") != null }

        cache.replace("key", "new").shouldBeTrue()
        cache.get("key") shouldBeEqualTo "new"
    }

    @Test
    fun `replace - queued remove prevents replacement`() {
        cache.put("key", "old")
        await atMost 5.seconds until { backCache.get("key") == "old" }

        cache.remove("key")

        cache.replace("key", "new").shouldBeFalse()
        await atMost 5.seconds until { backCache.get("key") == null }
        cache.get("key").shouldBeNull()
    }

    @Test
    fun `replace(key, oldValue, newValue) - 값이 일치할 때만 교체`() {
        cache.put("k", "old")
        await atMost 5.seconds until { backCache.get("k") != null }

        cache.replace("k", "wrong", "new").shouldBeFalse()
        cache.replace("k", "old", "new").shouldBeTrue()
        cache.get("k") shouldBeEqualTo "new"
    }

    @Test
    fun `getAndRemove - 캐시 값 조회 및 삭제`() {
        cache.put("key", "value")
        cache.getAndRemove("key") shouldBeEqualTo "value"
        cache.get("key").shouldBeNull()
        cache.getAndRemove("key").shouldBeNull()
    }

    @Test
    fun `getAndReplace - 캐시 값 조회 및 교체`() {
        cache.getAndReplace("missing", "val").shouldBeNull()
        cache.put("key", "old")

        await atMost 5.seconds until { backCache.get("key") != null }

        cache.getAndReplace("key", "new") shouldBeEqualTo "old"
        cache.get("key") shouldBeEqualTo "new"
    }

    @Test
    fun `clearLocal - 로컬만 초기화, back cache 유지`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")
        cache.clearLocal()
        cache.localCacheSize() shouldBeEqualTo 0L

        await atMost 3.seconds until { backCache.get("k1") != null }

        // back cache에서 읽어와서 front에 populate
        cache.containsKey("k1").shouldBeTrue()
    }

    @Test
    fun `clearAll - write-behind - 잠시 후 back cache도 초기화`() {
        cache.put("k1", "v1")
        cache.put("k2", "v2")

        await atMost 5.seconds until { backCache.get("k1") != null }

        cache.clearAll()
        cache.localCacheSize() shouldBeEqualTo 0L

        await atMost 5.seconds until { backCache.get("k1") == null }
    }

    @Test
    fun `clearAll - retry exhaustion restores back reads`() {
        val failingCache = resilientCacheWithFailingBackCache { backCache ->
            every { backCache.get("clear-key") } returns "back-value"
            every { backCache.clear() } throws IllegalStateException("clear failed")
        }

        try {
            failingCache.get("clear-key") shouldBeEqualTo "back-value"
            failingCache.clearAll()

            await atMost 5.seconds until { failingCache.get("clear-key") == "back-value" }
        } finally {
            failingCache.close()
        }
    }

    @Test
    fun `close drains queued write-behind commands`() {
        val closeDrainBackCache =
            JCaching.Caffeine.getOrCreate<String, String>(
                name = "resilient-near-close-drain-" + randomKey(),
                configuration =
                    jcacheConfiguration {
                        setExpiryPolicyFactory(EternalExpiryPolicy.factoryOf())
                    }
            )
        val closeDrainCache = ResilientNearJCache(
            backCache = closeDrainBackCache,
            config = ResilientNearJCacheConfig(
                writeQueueCapacity = 512,
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(10),
            )
        )

        repeat(200) { index ->
            closeDrainCache.put("close-$index", "value-$index")
        }

        closeDrainCache.close()

        repeat(200) { index ->
            closeDrainBackCache.get("close-$index") shouldBeEqualTo "value-$index"
        }
    }

    @Test
    fun `close - 중복 close 시 예외 없음`() {
        val c = ResilientNearJCache(backCache = backCache)
        c.close()
        c.close() // 중복 호출 시에도 예외 없음
        c.isClosed.shouldBeTrue()
    }

    private fun resilientCacheWithFailingBackCache(
        configure: (Cache<String, String>) -> Unit,
    ): ResilientNearJCache<String, String> {
        val failingBackCache = mockk<Cache<String, String>>(relaxed = true)
        configure(failingBackCache)
        return ResilientNearJCache(
            backCache = failingBackCache,
            config = ResilientNearJCacheConfig(
                retryMaxAttempts = 1,
                retryWaitDuration = Duration.ofMillis(10),
            )
        )
    }
}
