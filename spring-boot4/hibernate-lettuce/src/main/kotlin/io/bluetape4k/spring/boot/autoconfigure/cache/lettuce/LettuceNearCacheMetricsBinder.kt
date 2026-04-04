package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.warn
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.persistence.EntityManagerFactory
import org.hibernate.cache.spi.RegionFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.springframework.beans.factory.SmartInitializingSingleton

/**
 * Lettuce Near Cache 메트릭을 Micrometer [MeterRegistry]에 등록한다.
 *
 * 등록 메트릭:
 * - `lettuce.nearcache.active.regions` : 활성 region 수
 * - `lettuce.nearcache.total.local.size` : 전체 로컬(Caffeine) 캐시 항목 수
 *
 * ```kotlin
 * // Spring Boot가 자동 등록합니다.
 * // 직접 생성 예 (테스트 등):
 * val binder = LettuceNearCacheMetricsBinder(entityManagerFactory, meterRegistry)
 * binder.afterSingletonsInstantiated() // 메트릭 등록 완료
 *
 * // 등록된 메트릭 조회
 * val activeRegions = meterRegistry.find("lettuce.nearcache.active.regions").gauge()?.value() // 예: 2.0
 * val totalSize = meterRegistry.find("lettuce.nearcache.total.local.size").gauge()?.value()   // 예: 1500.0
 * ```
 */
class LettuceNearCacheMetricsBinder(
    private val entityManagerFactory: EntityManagerFactory,
    private val meterRegistry: MeterRegistry,
) : SmartInitializingSingleton {
    companion object : KLogging()

    override fun afterSingletonsInstantiated() {
        runCatching {
            val sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor::class.java)
            val regionFactory =
                sessionFactory.serviceRegistry
                    .getService(RegionFactory::class.java) ?: return

            if (regionFactory !is LettuceNearCacheRegionFactory) {
                log.debug { "RegionFactory is not LettuceNearCacheRegionFactory, skipping metrics" }
                return
            }
            registerMetrics(regionFactory)
        }.onFailure { e ->
            log.warn(e) { "Failed to register LettuceNearCache metrics: ${e.message}" }
        }
    }

    private fun registerMetrics(regionFactory: LettuceNearCacheRegionFactory) {
        // 활성 region 수 (동적: 새 region이 생성되면 자동 반영)
        Gauge
            .builder("lettuce.nearcache.active.regions", regionFactory) {
                it.getCaches().size.toDouble()
            }.description("활성 Lettuce Near Cache region 수")
            .register(meterRegistry)

        // 전체 로컬(Caffeine) 캐시 항목 수 합계
        Gauge
            .builder("lettuce.nearcache.total.local.size", regionFactory) {
                it
                    .getCaches()
                    .values
                    .sumOf { cache -> cache.localCacheSize() }
                    .toDouble()
            }.description("전체 로컬(Caffeine) 캐시 항목 수 합계")
            .register(meterRegistry)

        log.info { "LettuceNearCache aggregate metrics registered" }
    }
}
