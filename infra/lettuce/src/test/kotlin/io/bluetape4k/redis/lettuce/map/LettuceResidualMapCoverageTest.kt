package io.bluetape4k.redis.lettuce.map

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.lettuce.core.codec.StringCodec
import org.awaitility.kotlin.atMost
import org.awaitility.kotlin.await
import org.awaitility.kotlin.until
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

/** Loaded map의 write-behind 재시도와 일괄 삭제 경계를 실제 Redis로 검증합니다. */
internal class LettuceResidualMapCoverageTest: AbstractLettuceTest() {

    @Test
    fun `blocking write-behind exhaustion persists failed keys in dead-letter`() {
        val prefix = "residual-loaded-map:${randomName()}"
        val attempts = AtomicInteger(0)
        val writer = object: MapWriter<String, String> {
            override fun write(map: Map<String, String>) {
                attempts.incrementAndGet()
                error("residual write failure")
            }

            override fun delete(keys: Collection<String>) = Unit
        }
        val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            keyPrefix = prefix,
            writeBehindDelay = Duration.ofMillis(20),
            writeBehindBatchSize = 1,
        )

        LettuceLoadedMap(client = client, writer = writer, config = config).use { map ->
            map["dead-key"] = "dead-value"

            val connection = client.connect(StringCodec.UTF8)
            try {
                await atMost Duration.ofSeconds(2) until {
                    connection.sync().lrange("$prefix:dead-letter", 0L, -1L).contains("dead-key")
                }
                val keys = connection.sync().lrange("$prefix:dead-letter", 0L, -1L)
                keys.contains("dead-key").shouldBeTrue()
                attempts.get() shouldBeEqualTo 3
            } finally {
                connection.close()
            }
        }
    }

    @Test
    fun `suspending write-through deleteAll invokes writer and removes every cache key`() = runSuspendIO {
        val deleted = mutableListOf<String>()
        val writer = object: SuspendedMapWriter<String, String> {
            override suspend fun write(map: Map<String, String>) = Unit

            override suspend fun delete(keys: Collection<String>) {
                deleted.addAll(keys)
            }
        }
        val config = LettuceCacheConfig.READ_WRITE_THROUGH.copy(keyPrefix = "residual-suspend-map:${randomName()}")

        LettuceSuspendedLoadedMap(client = client, writer = writer, config = config).use { map ->
            map.set("k1", "v1")
            map.set("k2", "v2")
            map.deleteAll(listOf("k1", "k2"))

            deleted shouldBeEqualTo listOf("k1", "k2")
            map.get("k1").shouldBeNull()
            map.get("k2").shouldBeNull()
        }
    }
}
