package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
import jakarta.persistence.EntityManagerFactory
import org.hibernate.cache.spi.RegionFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.stat.Statistics
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
import org.springframework.boot.actuate.endpoint.annotation.Selector

/**
 * Lettuce Near Cache 통계를 제공하는 Actuator Endpoint.
 *
 * - `GET /actuator/nearcache` : 모든 region 통계
 * - `GET /actuator/nearcache/{regionName}` : 특정 region 상세 통계
 *
 * ```kotlin
 * // Spring Boot가 자동 등록합니다.
 * // 직접 생성 예 (테스트 등):
 * val endpoint = LettuceNearCacheActuatorEndpoint(entityManagerFactory)
 * val allStats = endpoint.getAllRegionStats()
 * // 예: {"io.example.User" -> RegionStats(localSize=42, localHitRate=0.95, ...)}
 * val regionStats = endpoint.getRegionStats("io.example.User")
 * // 예: RegionStats(regionName="io.example.User", localSize=42, ...)
 * ```
 */
@Endpoint(id = "nearcache")
class LettuceNearCacheActuatorEndpoint(
    private val entityManagerFactory: EntityManagerFactory,
) {

    /**
     * 모든 region의 통계 정보.
     */
    @ReadOperation
    fun getAllRegionStats(): Map<String, RegionStats> {
        val (factory, statistics) = getFactoryAndStats() ?: return emptyMap()
        return factory.getCaches().keys.associateWith { regionName ->
            buildRegionStats(regionName, factory, statistics)
        }
    }

    /**
     * 특정 region의 통계 정보.
     */
    @ReadOperation
    fun getRegionStats(@Selector regionName: String): RegionStats? {
        val (factory, statistics) = getFactoryAndStats() ?: return null
        if (!factory.getCaches().containsKey(regionName)) return null
        return buildRegionStats(regionName, factory, statistics)
    }

    private fun getFactoryAndStats(): Pair<LettuceNearCacheRegionFactory, Statistics?>? {
        return runCatching {
            val sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor::class.java)
            val regionFactory = sessionFactory.serviceRegistry
                .getService(RegionFactory::class.java) ?: return null

            if (regionFactory !is LettuceNearCacheRegionFactory) return null

            val stats = if (sessionFactory.sessionFactoryOptions.isStatisticsEnabled) {
                sessionFactory.statistics
            } else null

            regionFactory to stats
        }.getOrNull()
    }

    private fun buildRegionStats(
        regionName: String,
        factory: LettuceNearCacheRegionFactory,
        statistics: Statistics?,
    ): RegionStats {
        val cache = factory.getCaches()[regionName]
        val localStats = cache?.localStats()
        val l2Stats = runCatching { statistics?.getDomainDataRegionStatistics(regionName) }.getOrNull()

        return RegionStats(
            regionName = regionName,
            localSize = cache?.localCacheSize() ?: 0L,
            localHitRate = localStats?.hitRate(),
            localHitCount = localStats?.hitCount(),
            localMissCount = localStats?.missCount(),
            localEvictionCount = localStats?.evictionCount(),
            l2HitCount = l2Stats?.hitCount,
            l2MissCount = l2Stats?.missCount,
            l2PutCount = l2Stats?.putCount,
        )
    }

    data class RegionStats(
        val regionName: String,
        val localSize: Long,
        val localHitRate: Double?,
        val localHitCount: Long?,
        val localMissCount: Long?,
        val localEvictionCount: Long?,
        val l2HitCount: Long?,
        val l2MissCount: Long?,
        val l2PutCount: Long?,
    )
}
