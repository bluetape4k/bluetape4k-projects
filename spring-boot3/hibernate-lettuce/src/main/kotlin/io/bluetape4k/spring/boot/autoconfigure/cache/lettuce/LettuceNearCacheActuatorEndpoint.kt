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
 * // REST 응답 예시 (GET /actuator/nearcache)
 * // {
 * //   "io.example.domain.User": {
 * //     "regionName": "io.example.domain.User",
 * //     "localSize": 250,
 * //     "localHitRate": 0.95,
 * //     "l2HitCount": 1800,
 * //     "l2MissCount": 200,
 * //     "l2PutCount": 400
 * //   }
 * // }
 * ```
 */
@Endpoint(id = "nearcache")
class LettuceNearCacheActuatorEndpoint(
    private val entityManagerFactory: EntityManagerFactory,
) {

    /**
     * 모든 region의 통계 정보.
     *
     * ```kotlin
     * // GET /actuator/nearcache
     * val allStats: Map<String, RegionStats> = endpoint.getAllRegionStats()
     * // allStats["io.example.domain.User"]?.localSize → 250
     * ```
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
     *
     * ```kotlin
     * // GET /actuator/nearcache/io.example.domain.User
     * val stats: RegionStats? = endpoint.getRegionStats("io.example.domain.User")
     * // stats?.localHitRate → 0.95
     * // stats?.l2HitCount   → 1800
     * ```
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

    /**
     * 단일 region의 캐시 통계 정보.
     *
     * @property regionName Hibernate 2nd Level Cache region 이름
     * @property localSize Caffeine L1 캐시의 현재 항목 수
     * @property localHitRate Caffeine L1 캐시 히트율 (0.0~1.0, 통계 비활성화 시 null)
     * @property localHitCount Caffeine L1 캐시 히트 수 (통계 비활성화 시 null)
     * @property localMissCount Caffeine L1 캐시 미스 수 (통계 비활성화 시 null)
     * @property localEvictionCount Caffeine L1 캐시에서 제거된 항목 수 (통계 비활성화 시 null)
     * @property l2HitCount Hibernate L2 캐시 히트 수 (statistics 비활성화 시 null)
     * @property l2MissCount Hibernate L2 캐시 미스 수 (statistics 비활성화 시 null)
     * @property l2PutCount Hibernate L2 캐시 PUT 수 (statistics 비활성화 시 null)
     */
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
