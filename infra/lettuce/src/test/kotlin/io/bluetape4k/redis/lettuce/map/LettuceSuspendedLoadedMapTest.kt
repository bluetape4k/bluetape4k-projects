package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.isActive
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

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
}
