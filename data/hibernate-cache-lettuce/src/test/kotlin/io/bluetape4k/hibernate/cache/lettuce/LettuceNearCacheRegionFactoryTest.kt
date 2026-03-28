package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.testcontainers.storage.RedisServer
import io.lettuce.core.RedisClient
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cache.spi.RegionFactory
import org.hibernate.cache.spi.access.AccessType
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class LettuceNearCacheRegionFactoryTest {

    companion object {
        val redis: RedisServer by lazy { RedisServer.Launcher.redis }
    }

    @Test
    fun `RegionFactory가 정상적으로 시작하고 종료된다`() {
        val redisUri = "redis://${redis.host}:${redis.port}"
        val registry = StandardServiceRegistryBuilder()
            .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
            .applySetting("hibernate.connection.url", "jdbc:h2:mem:rftest;DB_CLOSE_DELAY=-1")
            .applySetting("hibernate.connection.username", "sa")
            .applySetting("hibernate.connection.password", "")
            .applySetting("hibernate.hbm2ddl.auto", "create-drop")
            .applySetting("hibernate.cache.use_second_level_cache", "true")
            .applySetting(
                "hibernate.cache.region.factory_class",
                LettuceNearCacheRegionFactory::class.java.name
            )
            .applySetting("hibernate.cache.lettuce.redis_uri", redisUri)
            .build()

        val sessionFactory = MetadataSources(registry)
            .buildMetadata()
            .buildSessionFactory()

        sessionFactory.shouldNotBeNull()
        sessionFactory.isOpen.shouldBeTrue()
        sessionFactory.close()
        sessionFactory.isOpen.shouldBeFalse()
    }

    @Test
    fun `기본 AccessType이 NONSTRICT_READ_WRITE이다`() {
        val factory = LettuceNearCacheRegionFactory()
        factory.getDefaultAccessType() shouldBeEqualTo AccessType.NONSTRICT_READ_WRITE
    }

    @Test
    fun `LettuceNearCacheProperties가 올바르게 파싱된다`() {
        val configValues = mapOf(
            "hibernate.cache.lettuce.redis_uri" to "redis://myhost:6380",
            "hibernate.cache.lettuce.codec" to "fory",
            "hibernate.cache.lettuce.local.max_size" to "5000",
            "hibernate.cache.lettuce.local.expire_after_write" to "10m",
            "hibernate.cache.lettuce.redis_ttl.default" to "300s",
            "hibernate.cache.lettuce.redis_ttl.myRegion" to "600s",
            "hibernate.cache.lettuce.use_resp3" to "false",
        )

        val props = LettuceNearCacheProperties.from(configValues)

        props.redisUri shouldBeEqualTo "redis://myhost:6380"
        props.codec shouldBeEqualTo "fory"
        props.localMaxSize shouldBeEqualTo 5000L
        props.useResp3 shouldBeEqualTo false
        props.regionTtls.containsKey("myRegion").shouldBeTrue()
    }

    @Test
    fun `timestamps region은 Redis TTL을 강제 비활성화한다`() {
        val props = LettuceNearCacheProperties.from(
            mapOf("hibernate.cache.lettuce.redis_ttl.default" to "300s")
        )

        val config = props.buildNearCacheConfig(RegionFactory.DEFAULT_UPDATE_TIMESTAMPS_REGION_UNQUALIFIED_NAME)

        config.redisTtl shouldBeEqualTo null
    }

    @Test
    fun `잘못된 local max size 설정은 즉시 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheProperties.from(
                mapOf("hibernate.cache.lettuce.local.max_size" to "0")
            )
        }
    }

    @Test
    fun `잘못된 duration 설정은 즉시 실패한다`() {
        assertFailsWith<IllegalArgumentException> {
            LettuceNearCacheProperties.from(
                mapOf("hibernate.cache.lettuce.redis_ttl.default" to "nonsense")
            )
        }
    }

    @Test
    fun `StorageAccess release는 공유 near cache를 닫지 않는다`() {
        val redisClient = RedisClient.create("redis://${redis.host}:${redis.port}")

        @Suppress("UNCHECKED_CAST")
        val nearCache = LettuceNearCache(redisClient, StringCodec.UTF8) as LettuceNearCache<Any>

        redisClient.use {
            nearCache.use { cache ->
                val storageAccess = LettuceNearCacheStorageAccess("region", cache)

                storageAccess.release()

                cache.isClosed.shouldBeFalse()
                cache.put("key", "value")
                cache.get("key") shouldBeEqualTo "value"
            }
        }
    }
}
