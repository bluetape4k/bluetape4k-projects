package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

/**
 * Hibernate 2nd Level Cache를 Lettuce Near Cache로 자동 설정한다.
 *
 * `application.yml`에서 `bluetape4k.cache.lettuce-near.*` 설정만으로
 * [LettuceNearCacheRegionFactory]가 Hibernate의 Region Factory로 등록된다.
 */
@AutoConfiguration
@ConditionalOnClass(LettuceNearCacheRegionFactory::class, EntityManagerFactory::class)
@ConditionalOnProperty(
    prefix = "bluetape4k.cache.lettuce-near",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
@EnableConfigurationProperties(LettuceNearCacheSpringProperties::class)
class LettuceNearCacheHibernateAutoConfiguration {

    companion object {
        private const val HIBERNATE_LETTUCE_PREFIX = "hibernate.cache.lettuce."
    }

    @Bean
    fun lettuceNearCacheHibernatePropertiesCustomizer(
        props: LettuceNearCacheSpringProperties,
    ): HibernatePropertiesCustomizer = HibernatePropertiesCustomizer { hibernateProperties ->
        // 2nd Level Cache 활성화
        hibernateProperties["hibernate.cache.region.factory_class"] =
            LettuceNearCacheRegionFactory::class.java.name
        hibernateProperties["hibernate.cache.use_second_level_cache"] = "true"

        // Redis 연결 설정
        hibernateProperties["${HIBERNATE_LETTUCE_PREFIX}redis_uri"] = props.redisUri
        hibernateProperties["${HIBERNATE_LETTUCE_PREFIX}codec"] = props.codec
        hibernateProperties["${HIBERNATE_LETTUCE_PREFIX}use_resp3"] = props.useResp3.toString()

        // 로컬(Caffeine) 캐시 설정
        hibernateProperties["${HIBERNATE_LETTUCE_PREFIX}local.max_size"] = props.local.maxSize.toString()
        hibernateProperties["${HIBERNATE_LETTUCE_PREFIX}local.expire_after_write"] =
            toHibernateDuration(props.local.expireAfterWrite)

        // Redis TTL 설정
        hibernateProperties["${HIBERNATE_LETTUCE_PREFIX}redis_ttl.default"] =
            toHibernateDuration(props.redisTtl.default)
        props.redisTtl.regions.forEach { (region, ttl) ->
            hibernateProperties["${HIBERNATE_LETTUCE_PREFIX}redis_ttl.$region"] = toHibernateDuration(ttl)
        }

        // Metrics/통계 설정
        if (props.metrics.enabled) {
            hibernateProperties["hibernate.generate_statistics"] = "true"
            if (props.metrics.enableCaffeineStats) {
                hibernateProperties["${HIBERNATE_LETTUCE_PREFIX}local.record_stats"] = "true"
            }
        }
    }

    private fun toHibernateDuration(duration: java.time.Duration): String =
        if (duration.toMillis() % 1000L == 0L) {
            "${duration.seconds}s"
        } else {
            "${duration.toMillis()}ms"
        }
}
