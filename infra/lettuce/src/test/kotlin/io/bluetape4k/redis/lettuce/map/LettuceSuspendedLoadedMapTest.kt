package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class LettuceSuspendedLoadedMapTest: AbstractLettuceTest() {

    companion object: KLoggingChannel()

    private fun newMap(
        loader: SuspendedMapLoader<String, String>? = null,
        writer: SuspendedMapWriter<String, String>? = null,
        config: LettuceCacheConfig = LettuceCacheConfig.READ_WRITE_THROUGH,
    ): LettuceSuspendedLoadedMap<String, String> =
        LettuceSuspendedLoadedMap(
            client = client,
            loader = loader,
            writer = writer,
            config = config.copy(keyPrefix = "suspend-loaded-test:${randomName()}")
        )

    @Test
    fun `get - loader가 null이면 캐시 미스 시 null 반환`() = runSuspendIO {
        newMap().use { map ->
            map.get("nonexistent").shouldBeNull()
        }
    }

    @Test
    fun `get - Read-through - 캐시 미스 시 loader 호출 후 캐싱`() = runSuspendIO {
        val callCount = AtomicInteger(0)
        val loader = object: SuspendedMapLoader<String, String> {
            override suspend fun load(key: String): String {
                callCount.incrementAndGet()
                return "loaded-$key"
            }

            override suspend fun loadAllKeys(): List<String> = emptyList()
        }

        newMap(loader = loader).use { map ->
            val value = map.get("key1")
            value shouldBeEqualTo "loaded-key1"
            callCount.get() shouldBeEqualTo 1

            // 두 번째 조회는 캐시 히트 → loader 미호출
            map.get("key1") shouldBeEqualTo "loaded-key1"
            callCount.get() shouldBeEqualTo 1
        }
    }

    @Test
    fun `set - Write-through - writer 호출`() = runSuspendIO {
        val written = mutableMapOf<String, String>()
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) {
                written.putAll(map)
            }

            override suspend fun delete(keys: Collection<String>) {
                keys.forEach { written.remove(it) }
            }
        }

        newMap(writer = writer).use { map ->
            map.set("key1", "value1")
            written["key1"] shouldBeEqualTo "value1"
        }
    }

    @Test
    fun `get - NONE 모드 - loader 없이 Redis만 사용`() = runSuspendIO {
        newMap(config = LettuceCacheConfig.READ_ONLY).use { map ->
            map.get("key1").shouldBeNull()
            map.set("key1", "direct")
            map.get("key1") shouldBeEqualTo "direct"
        }
    }

    @Test
    fun `close - 리소스 정리`() = runSuspendIO {
        val map = newMap()
        map.shouldNotBeNull()
        map.close()
    }

    @Test
    fun `close - 공유 scope를 취소하지 않는다`() = runSuspendIO {
        val sharedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val map = LettuceSuspendedLoadedMap<String, String>(
            client = client,
            scope = sharedScope,
            config = LettuceCacheConfig.READ_ONLY.copy(keyPrefix = "scope-test:${randomName()}")
        )
        map.close()
        // Shared scope must still be active — close() must not cancel a caller-provided scope
        sharedScope.isActive.shouldBeTrue()
        sharedScope.cancel()
    }

    @Test
    fun `write-behind - writer 실패 후 재시도 소진 시 dead-letter에 기록된다`() = runSuspendIO {
        val prefix = "dead-letter-test:${randomName()}"
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindDelay = Duration.ofMillis(20),
            writeBehindBatchSize = 1,
        )
        val failingWriter = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) {
                throw RuntimeException("simulated write failure")
            }

            override suspend fun delete(keys: Collection<String>) {}
        }

        LettuceSuspendedLoadedMap<String, String>(client = client, writer = failingWriter, config = config).use { map ->
            map.set("deadkey", "deadvalue")
            // Allow 3 retry cycles: each cycle ≈ writeBehindDelay (20ms) → ~60ms minimum
            delay(400L)
        }

        // Verify dead-letter list contains the key
        val strConn = client.connect(StringCodec.UTF8)
        try {
            val deadLetterKey = "$prefix:dead-letter"
            val keys = strConn.async().lrange(deadLetterKey, 0L, -1L).await()
            keys.contains("deadkey").shouldBeTrue()
        } finally {
            strConn.close()
        }
    }

    @Test
    fun `suspendClose - caller cancellation is propagated before internal shutdown timeout`() = runSuspendIO {
        val writerStarted = CompletableDeferred<Unit>()
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = "suspend-close-cancel:${randomName()}",
            writeBehindDelay = Duration.ofSeconds(5),
            writeBehindBatchSize = 1,
            writeBehindShutdownTimeout = Duration.ofSeconds(2),
        )
        val slowWriter = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) {
                writerStarted.complete(Unit)
                delay(5_000L)
            }

            override suspend fun delete(keys: Collection<String>) {}
        }

        val map = LettuceSuspendedLoadedMap<String, String>(
            client = client,
            writer = slowWriter,
            config = config,
        )

        map.set("key1", "value1")
        writerStarted.await()

        val elapsedMillis = measureTimeMillis {
            assertFailsWith<TimeoutCancellationException> {
                withTimeout(150L) {
                    map.suspendClose()
                }
            }
        }

        elapsedMillis shouldBeLessThan config.writeBehindShutdownTimeout.toMillis()
    }

    @Test
    fun `getAll - 일부 캐시 미스 키는 loader로 Read-through한다`() = runSuspendIO {
        val loaderCallCount = AtomicInteger(0)
        val loader = object: SuspendedMapLoader<String, String> {
            override suspend fun load(key: String): String {
                loaderCallCount.incrementAndGet()
                return "from-db-$key"
            }

            override suspend fun loadAllKeys(): List<String> = emptyList()
        }

        newMap(loader = loader).use { map ->
            map.set("k1", "cached-v1")
            val result = map.getAll(setOf("k1", "k2", "k3"))
            result["k1"] shouldBeEqualTo "cached-v1"
            result["k2"] shouldBeEqualTo "from-db-k2"
            result["k3"] shouldBeEqualTo "from-db-k3"
            loaderCallCount.get() shouldBeEqualTo 2
        }
    }

    @Test
    fun `getAll - 모든 키가 캐시 미스인 경우 loader로 모두 처리한다`() = runSuspendIO {
        val loaderCallCount = AtomicInteger(0)
        val loader = object: SuspendedMapLoader<String, String> {
            override suspend fun load(key: String): String {
                loaderCallCount.incrementAndGet()
                return "fallback-$key"
            }

            override suspend fun loadAllKeys(): List<String> = emptyList()
        }

        newMap(loader = loader).use { map ->
            val result = map.getAll(setOf("k1", "k2", "k3"))
            result["k1"] shouldBeEqualTo "fallback-k1"
            result["k2"] shouldBeEqualTo "fallback-k2"
            result["k3"] shouldBeEqualTo "fallback-k3"
            loaderCallCount.get() shouldBeEqualTo 3
        }
    }
}
