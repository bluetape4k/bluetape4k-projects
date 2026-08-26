package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisFuture
import io.lettuce.core.SetArgs
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/**
 * MockK-based unit tests for [LettuceSuspendedLoadedMap] failure paths.
 *
 * These tests exercise:
 * - Redis GET failure → loader fallback (CancellationException not swallowed)
 * - Redis SET failure after load (logged, not thrown)
 * - CancellationException propagation (never swallowed by try/catch)
 *
 * No real Redis connection is needed; all Lettuce async commands are mocked.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LettuceSuspendedLoadedMapMockTest {

    companion object : KLoggingChannel()

    /** Minimal [RedisFuture] implementation backed by [CompletableFuture]. */
    private class TestRedisFuture<T> : CompletableFuture<T>(), RedisFuture<T> {
        override fun getError(): String? =
            if (isCompletedExceptionally) "completed exceptionally" else null

        override fun await(timeout: Long, unit: TimeUnit): Boolean =
            try {
                get(timeout, unit)
                true
            } catch (_: TimeoutException) {
                false
            }
    }

    private fun <T> completedRedisFuture(value: T): RedisFuture<T> =
        TestRedisFuture<T>().apply { complete(value) }

    private fun <T> failedRedisFuture(error: Throwable): RedisFuture<T> =
        TestRedisFuture<T>().apply { completeExceptionally(error) }

    private fun buildMap(
        asyncCommands: RedisAsyncCommands<String, String>,
        loader: SuspendedMapLoader<String, String>? = null,
        writer: SuspendedMapWriter<String, String>? = null,
        config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH.copy(keyPrefix = "mock-test"),
    ): LettuceSuspendedLoadedMap<String, String> {
        val connection = mockk<StatefulRedisConnection<String, String>>(relaxed = true)
        every { connection.async() } returns asyncCommands

        val client = mockk<RedisClient>(relaxed = true)
        every { client.connect<String, String>(any()) } returns connection

        return LettuceSuspendedLoadedMap(
            client = client,
            loader = loader,
            writer = writer,
            config = config,
        )
    }

    @Test
    fun `get - Redis GET failure falls back to loader`() = runSuspendIO {
        val asyncCommands = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        val loader = mockk<SuspendedMapLoader<String, String>>()

        // Redis GET throws a runtime exception (simulates connection error)
        every { asyncCommands.get(any<String>()) } returns
            failedRedisFuture(RuntimeException("Redis down"))

        // Loader returns a value from the DB
        coEvery { loader.load("key1") } returns "db-value"
        coEvery { loader.loadAllKeys() } returns emptyList()

        // Redis SET after load also fails — should be silently logged, not thrown
        every { asyncCommands.set(any<String>(), any(), any<SetArgs>()) } returns
            failedRedisFuture(RuntimeException("Redis SET down"))

        val map = buildMap(asyncCommands, loader = loader)
        val result = map.get("key1")

        result shouldBeEqualTo "db-value"
        coVerify(exactly = 1) { loader.load("key1") }
    }

    @Test
    fun `get - Redis GET failure with no loader returns null`() = runSuspendIO {
        val asyncCommands = mockk<RedisAsyncCommands<String, String>>(relaxed = true)

        every { asyncCommands.get(any<String>()) } returns
            failedRedisFuture(RuntimeException("Redis down"))

        val map = buildMap(asyncCommands, loader = null)
        val result = map.get("missing")

        result.shouldBeNull()
    }

    @Test
    fun `get - loader returns null when key not found in DB`() = runSuspendIO {
        val asyncCommands = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        val loader = mockk<SuspendedMapLoader<String, String>>()

        every { asyncCommands.get(any<String>()) } returns
            failedRedisFuture(RuntimeException("Redis down"))

        coEvery { loader.load("missing") } returns null
        coEvery { loader.loadAllKeys() } returns emptyList()

        val map = buildMap(asyncCommands, loader = loader)
        val result = map.get("missing")

        result.shouldBeNull()
        coVerify(exactly = 1) { loader.load("missing") }
    }

    @Test
    fun `get - CancellationException from loader is rethrown, not swallowed`() = runSuspendIO {
        val asyncCommands = mockk<RedisAsyncCommands<String, String>>(relaxed = true)

        // Redis GET fails → loader fallback path
        every { asyncCommands.get(any<String>()) } returns
            failedRedisFuture(RuntimeException("Redis down"))

        // Loader itself throws CancellationException (e.g. caller cancelled during load)
        val loader = mockk<SuspendedMapLoader<String, String>>()
        coEvery { loader.load(any()) } throws CancellationException("cancelled during load")
        coEvery { loader.loadAllKeys() } returns emptyList()

        val map = buildMap(asyncCommands, loader = loader)

        assertFailsWith<CancellationException> {
            map.get("key1")
        }
    }

    @Test
    fun `get - CancellationException from asyncCommands await is rethrown, not swallowed`() = runSuspendIO {
        // This test verifies the actual fix: runCatching { await() } → try/catch that rethrows CE.
        // A future completed with CancellationException must escape get(), not be silently swallowed.
        val asyncCommands = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        every { asyncCommands.get(any<String>()) } returns
            failedRedisFuture(CancellationException("redis command cancelled"))

        val map = buildMap(asyncCommands, loader = null)

        assertFailsWith<CancellationException> {
            map.get("key1")
        }
    }

    @Test
    fun `get - CancellationException from asyncCommands SET await is rethrown`() = runSuspendIO {
        // Verifies the SETEX path: CE from set().await() must not be swallowed.
        val asyncCommands = mockk<RedisAsyncCommands<String, String>>(relaxed = true)

        // GET misses (null) → loader returns value → SET fails with CE
        every { asyncCommands.get(any<String>()) } returns completedRedisFuture(null)
        every { asyncCommands.set(any(), any(), any<SetArgs>()) } returns
            failedRedisFuture(CancellationException("set cancelled"))

        val loader = mockk<SuspendedMapLoader<String, String>>()
        coEvery { loader.load("key1") } returns "value1"
        coEvery { loader.loadAllKeys() } returns emptyList()

        val map = buildMap(asyncCommands, loader = loader)

        assertFailsWith<CancellationException> {
            map.get("key1")
        }
    }

    @Test
    fun `close - interrupted blocking caller restores status and closes owned resources`() {
        val asyncCommands = mockk<RedisAsyncCommands<String, String>>(relaxed = true)
        val connection = mockk<StatefulRedisConnection<String, String>>(relaxed = true)
        every { connection.async() } returns asyncCommands

        val client = mockk<RedisClient>(relaxed = true)
        every { client.connect<String, String>(any()) } returns connection

        val map = LettuceSuspendedLoadedMap(
            client = client,
            writer = object: SuspendedMapWriter<String, String> {
                override suspend fun write(map: Map<String, String>) = Unit
                override suspend fun delete(keys: Collection<String>) = Unit
            },
            config = LettuceCacheConfig.WRITE_BEHIND.copy(keyPrefix = "mock-close-interrupt"),
        )
        val interruptedStatusRestored = AtomicBoolean(false)
        val completed = CountDownLatch(1)

        thread(start = true, isDaemon = true, name = "loaded-map-close-interrupt-test") {
            try {
                Thread.currentThread().interrupt()
                map.close()
                interruptedStatusRestored.set(Thread.currentThread().isInterrupted)
            } finally {
                completed.countDown()
            }
        }

        completed.await(5, TimeUnit.SECONDS).shouldBeTrue()
        interruptedStatusRestored.get().shouldBeTrue()
        verify(exactly = 1) { connection.close() }
    }
}
