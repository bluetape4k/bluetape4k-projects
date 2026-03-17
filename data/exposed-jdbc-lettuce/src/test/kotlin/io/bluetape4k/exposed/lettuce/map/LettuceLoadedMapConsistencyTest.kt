package io.bluetape4k.exposed.lettuce.map

import io.bluetape4k.exposed.lettuce.AbstractJdbcLettuceTest
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.LettuceLoadedMap
import io.bluetape4k.redis.lettuce.map.MapWriter
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import java.util.*

class LettuceLoadedMapConsistencyTest: AbstractJdbcLettuceTest() {
    @Test
    fun `WRITE_THROUGH save 실패 시 Redis는 갱신되지 않는다`() {
        val map =
            newMap(
                config =
                    LettuceCacheConfig.READ_WRITE_THROUGH.copy(
                        keyPrefix = "write-through-fail-${UUID.randomUUID()}"
                    ),
                writer =
                    object : MapWriter<String, String> {
                        override fun write(map: Map<String, String>) {
                            error("write failure")
                        }

                        override fun delete(keys: Collection<String>) = Unit
                    }
            )

        map.use {
            runCatching { it["k"] = "v" }.exceptionOrNull()?.message shouldBeEqualTo "write failure"
            it["k"].shouldBeNull()
        }
    }

    @Test
    fun `WRITE_THROUGH delete 실패 시 Redis 값은 유지된다`() {
        val keyPrefix = "delete-through-fail-${UUID.randomUUID()}"
        newMap(config = LettuceCacheConfig.READ_ONLY.copy(keyPrefix = keyPrefix)).use { seed ->
            seed["k"] = "v"
        }

        val map =
            newMap(
                config = LettuceCacheConfig.READ_WRITE_THROUGH.copy(keyPrefix = keyPrefix),
                writer =
                    object : MapWriter<String, String> {
                        override fun write(map: Map<String, String>) = Unit

                        override fun delete(keys: Collection<String>) {
                            error("delete failure")
                        }
                    }
            )

        map.use {
            runCatching { it.delete("k") }.exceptionOrNull()?.message shouldBeEqualTo "delete failure"
        }

        newMap(config = LettuceCacheConfig.READ_ONLY.copy(keyPrefix = keyPrefix)).use { reader ->
            reader["k"] shouldBeEqualTo "v"
        }
    }

    private fun newMap(
        config: LettuceCacheConfig,
        writer: MapWriter<String, String>? = null,
    ): LettuceLoadedMap<String, String> {
        val client =
            RedisClient.create(
                RedisServer.Launcher.LettuceLib.getRedisURI(redis.host, redis.port)
            )
        return LettuceLoadedMap(
            client = client,
            writer = writer,
            config = config
        )
    }
}
