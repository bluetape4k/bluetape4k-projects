package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
import io.micrometer.core.instrument.MeterRegistry
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Lettuce Near Cache Micrometer Metrics 자동 설정.
 *
 * [MeterRegistry]가 있을 때 [LettuceNearCacheMetricsBinder]를 등록한다.
 * Actuator endpoint는 [LettuceNearCacheActuatorAutoConfiguration]에서 별도로 등록된다.
 *
 * ```kotlin
 * // application.yml
 * // bluetape4k.cache.lettuce-near.metrics.enabled: true           (기본값)
 * // bluetape4k.cache.lettuce-near.metrics.enable-caffeine-stats: true
 * // 등록 메트릭:
 * // lettuce.nearcache.active.regions   — 활성 region 수 (Gauge)
 * // lettuce.nearcache.total.local.size — 전체 Caffeine 캐시 항목 수 (Gauge)
 * ```
 */
@AutoConfiguration(after = [LettuceNearCacheHibernateAutoConfiguration::class])
@ConditionalOnClass(LettuceNearCacheRegionFactory::class, EntityManagerFactory::class, MeterRegistry::class)
@ConditionalOnBean(EntityManagerFactory::class, MeterRegistry::class)
@ConditionalOnProperty(
    prefix = "bluetape4k.cache.lettuce-near.metrics",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(LettuceNearCacheSpringProperties::class)
class LettuceNearCacheMetricsAutoConfiguration {

    @Bean
    fun lettuceNearCacheMetricsBinder(
        entityManagerFactory: EntityManagerFactory,
        meterRegistry: MeterRegistry,
    ): LettuceNearCacheMetricsBinder =
        LettuceNearCacheMetricsBinder(entityManagerFactory, meterRegistry)
}
