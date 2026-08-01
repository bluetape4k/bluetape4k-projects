package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.cache.nearcache.LettuceNearCacheConfig
import io.mockk.clearMocks
import io.mockk.mockk
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.StringCodec
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

/**
 * Hibernate L2 eviction 실패 전파를 검증한다.
 *
 * Near Cache 연결을 먼저 닫아 Redis backend failure를 재현한다.
 * eviction 호출은 정상 반환하지 않아야 하며, Redis에 남아 있는 stale entry도
 * 성공적으로 제거된 것으로 취급하지 않아야 한다.
 */
class HibernateEvictionFailureTest {

    private val session = mockk<SharedSessionContractImplementor>(relaxed = true)

    private lateinit var redisClient: RedisClient
    private lateinit var cache: LettuceNearCache<Any>
    private lateinit var cacheName: String
    private lateinit var storageAccess: LettuceNearCacheStorageAccess

    @BeforeEach
    fun setUp() {
        clearMocks(session)
        cacheName = "issue-1273-${UUID.randomUUID()}"
        redisClient = RedisClient.create(RedisServers.redis.url)

        @Suppress("UNCHECKED_CAST")
        val stringCache = LettuceNearCache(
            redisClient,
            StringCodec.UTF8,
            LettuceNearCacheConfig<String, String>(
                cacheName = cacheName,
                useRespProtocol3 = false,
            )
        ) as LettuceNearCache<Any>

        cache = stringCache
        storageAccess = LettuceNearCacheStorageAccess("region", cache)
    }

    @AfterEach
    fun tearDown() {
        runCatching {
            if (::redisClient.isInitialized) {
                redisClient.connect(StringCodec.UTF8).use { connection ->
                    val commands = connection.sync()
                    val keys = commands.keys("$cacheName:*")
                    if (keys.isNotEmpty()) commands.unlink(*keys.toTypedArray())
                }
            }
        }
        runCatching { if (::cache.isInitialized) cache.close() }
        runCatching { if (::redisClient.isInitialized) redisClient.shutdown() }
    }

    @Test
    fun `key eviction backend failure is propagated and stale Redis entry is not treated as evicted`() {
        storageAccess.putIntoCache("key", "value", session)
        redisKeys().shouldNotBeEmpty()

        // Close the Near Cache connection while the Redis entry remains.
        cache.close()

        assertFailsWith<Exception> {
            storageAccess.evictData("key")
        }

        redisKeys().shouldNotBeEmpty()
    }

    @Test
    fun `region eviction backend failure is propagated and stale Redis entries remain observable`() {
        listOf("first", "second").forEach { key ->
            storageAccess.putIntoCache(key, key, session)
        }
        redisKeys().size shouldBeGreaterThan 1

        // Close the Near Cache connection while the Redis entries remain.
        cache.close()

        assertFailsWith<Exception> {
            storageAccess.evictData()
        }

        redisKeys().size shouldBeGreaterThan 1
    }

    private fun redisKeys(): List<String> =
        redisClient.connect(StringCodec.UTF8).use { connection ->
            connection.sync().keys("$cacheName:*")
        }
}
