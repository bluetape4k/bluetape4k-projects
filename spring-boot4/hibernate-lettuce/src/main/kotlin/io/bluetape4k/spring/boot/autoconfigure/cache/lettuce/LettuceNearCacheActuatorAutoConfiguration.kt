package io.bluetape4k.spring.boot.autoconfigure.cache.lettuce

import io.bluetape4k.hibernate.cache.lettuce.LettuceNearCacheRegionFactory
import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.actuate.endpoint.annotation.Endpoint
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

/**
 * Lettuce Near Cache Actuator Endpoint 자동 설정.
 *
 * `spring-boot-actuate`가 classpath에 있을 때 [LettuceNearCacheActuatorEndpoint]를 등록한다.
 * `@ConditionalOnClass(Endpoint::class)`이 클래스 레벨에 있어, actuate가 없으면 이 클래스 자체가 로드되지 않는다.
 */
@AutoConfiguration(after = [LettuceNearCacheHibernateAutoConfiguration::class])
@ConditionalOnClass(Endpoint::class, LettuceNearCacheRegionFactory::class, EntityManagerFactory::class)
@ConditionalOnBean(EntityManagerFactory::class)
@ConditionalOnProperty(
    prefix = "bluetape4k.cache.lettuce-near",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class LettuceNearCacheActuatorAutoConfiguration {

    @Bean
    fun lettuceNearCacheActuatorEndpoint(
        entityManagerFactory: EntityManagerFactory,
    ): LettuceNearCacheActuatorEndpoint =
        LettuceNearCacheActuatorEndpoint(entityManagerFactory)
}
