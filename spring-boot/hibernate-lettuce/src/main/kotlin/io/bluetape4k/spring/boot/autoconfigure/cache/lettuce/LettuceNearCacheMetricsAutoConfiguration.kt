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
 * root `bluetape4k.cache.lettuce-near.enabled`와
 * `bluetape4k.cache.lettuce-near.metrics.enabled`가 모두 활성화되고
 * [MeterRegistry]가 있을 때 [LettuceNearCacheMetricsBinder]를 등록한다.
 * root를 끄면 metrics.enabled가 true여도 binder가 등록되지 않는다.
 * Actuator endpoint는 [LettuceNearCacheActuatorAutoConfiguration]에서
 * 동일한 root/metrics 조건으로 별도 등록된다.
 *
 * ```yaml
 * # application.yml — Metrics 활성화 설정
 * bluetape4k:
 *   cache:
 *     lettuce-near:
 *       metrics:
 *         enabled: true
 *         enable-caffeine-stats: true
 * ```
 *
 * ```kotlin
 * // Micrometer에 등록되는 메트릭 예
 * // lettuce.nearcache.active.regions   → 활성 region 수
 * // lettuce.nearcache.total.local.size → 전체 로컬 캐시 항목 수
 * meterRegistry.find("lettuce.nearcache.active.regions").gauge()?.value() // 예: 3.0
 * ```
 */
@AutoConfiguration(
    after = [LettuceNearCacheHibernateAutoConfiguration::class],
    afterName = [
        "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration",
        "org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration",
    ]
)
@ConditionalOnClass(
    name = [
        "io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory",
        "jakarta.persistence.EntityManagerFactory",
        "io.micrometer.core.instrument.MeterRegistry",
    ]
)
@ConditionalOnBean(type = ["jakarta.persistence.EntityManagerFactory", "io.micrometer.core.instrument.MeterRegistry"])
@ConditionalOnProperty(
    prefix = "bluetape4k.cache.lettuce-near",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
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
