package io.bluetape4k.hibernate.cache.lettuce

import io.bluetape4k.cache.nearcache.LettuceNearCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.redis.lettuce.codec.LettuceBinaryCodec
import io.bluetape4k.utils.ShutdownQueue
import io.lettuce.core.ClientOptions
import io.lettuce.core.RedisClient
import io.lettuce.core.protocol.ProtocolVersion
import org.hibernate.boot.spi.SessionFactoryOptions
import org.hibernate.cache.cfg.spi.DomainDataRegionBuildingContext
import org.hibernate.cache.cfg.spi.DomainDataRegionConfig
import org.hibernate.cache.spi.access.AccessType
import org.hibernate.cache.spi.support.DomainDataStorageAccess
import org.hibernate.cache.spi.support.RegionFactoryTemplate
import org.hibernate.cache.spi.support.StorageAccess
import org.hibernate.engine.spi.SessionFactoryImplementor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Hibernate 2nd Level Cache RegionFactory 구현체.
 *
 * [RegionFactoryTemplate]을 상속하여 Lettuce Near Cache(Caffeine L1 + Redis L2)를
 * Hibernate 2nd level cache로 사용한다.
 *
 * ## 설정
 * ```properties
 * hibernate.cache.region.factory_class=io.bluetape4k.hibernate.lettuce.LettuceNearCacheRegionFactory
 * hibernate.cache.use_second_level_cache=true
 * hibernate.cache.lettuce.redis_uri=redis://localhost:6379
 * hibernate.cache.lettuce.codec=lz4fory
 * hibernate.cache.lettuce.local.max_size=10000
 * hibernate.cache.lettuce.redis_ttl.default=120s
 * hibernate.cache.lettuce.use_resp3=true
 * ```
 *
 * ## 기본 AccessType
 * [AccessType.NONSTRICT_READ_WRITE]: Redis 기반 분산 캐시는 soft-lock이 없으므로
 * 업데이트 시 캐시 항목을 제거하고 다음 읽기 시 DB에서 재로드.
 */
class LettuceNearCacheRegionFactory: RegionFactoryTemplate() {

    companion object: KLogging()

    private lateinit var properties: LettuceNearCacheProperties
    private lateinit var codec: LettuceBinaryCodec<Any>

    /**
     * [releaseFromUse]에서 nullable로 처리하여 prepareForUse 실패 시에도 안전하게 정리한다.
     * lateinit 대신 nullable var를 사용해 [UninitializedPropertyAccessException]을 방지한다.
     */
    private var redisClient: RedisClient? = null
    private val caches = ConcurrentHashMap<String, LettuceNearCache<Any>>()

    override fun prepareForUse(
        settings: SessionFactoryOptions,
        configValues: Map<String, Any>,
    ) {
        properties = LettuceNearCacheProperties.from(configValues)
        codec = properties.createCodec()

        val client = RedisClient.create(properties.redisUri)
        if (properties.useResp3) {
            client.options = ClientOptions.builder()
                .protocolVersion(ProtocolVersion.RESP3)
                .build()
        }
        redisClient = client
        ShutdownQueue.register { runCatching { client.shutdown() } }
    }

    override fun releaseFromUse() {
        caches.values.forEach { cache ->
            runCatching { cache.close() }.onFailure { e ->
                log.warn(e) { "캐시 close 중 오류 무시: ${cache.cacheName}" }
            }
        }
        caches.clear()
        redisClient?.let { client ->
            runCatching { client.shutdown() }.onFailure { e ->
                log.warn(e) { "RedisClient shutdown 중 오류 무시" }
            }
            redisClient = null
        }
    }

    override fun getDefaultAccessType(): AccessType = AccessType.NONSTRICT_READ_WRITE

    /**
     * 현재 관리 중인 모든 region의 [LettuceNearCache] 인스턴스 맵을 반환한다.
     * Spring Boot Auto-Configuration에서 Metrics/Actuator 연동 시 사용된다.
     *
     * ```kotlin
     * val factory = LettuceNearCacheRegionFactory()
     * // factory 초기화 후
     * val caches = factory.getCaches()
     * // caches.keys.contains("io.bluetape4k.domain.MyEntity") == true
     * ```
     */
    fun getCaches(): Map<String, LettuceNearCache<Any>> = Collections.unmodifiableMap(caches)

    override fun createDomainDataStorageAccess(
        regionConfig: DomainDataRegionConfig,
        buildingContext: DomainDataRegionBuildingContext,
    ): DomainDataStorageAccess = createStorageAccess(regionConfig.regionName)

    override fun createQueryResultsRegionStorageAccess(
        regionName: String,
        sessionFactory: SessionFactoryImplementor,
    ): StorageAccess = createStorageAccess(regionName)

    override fun createTimestampsRegionStorageAccess(
        regionName: String,
        sessionFactory: SessionFactoryImplementor,
    ): StorageAccess = createStorageAccess(regionName)

    private fun createStorageAccess(regionName: String): LettuceNearCacheStorageAccess {
        val client = checkNotNull(redisClient) {
            "RedisClient가 초기화되지 않았습니다. prepareForUse()가 완료된 후에 호출해야 합니다."
        }
        val nearCache =
            caches.computeIfAbsent(regionName) {
                LettuceNearCache(client, codec, properties.buildNearCacheConfig(regionName))
            }
        return LettuceNearCacheStorageAccess(regionName, nearCache)
    }
}
