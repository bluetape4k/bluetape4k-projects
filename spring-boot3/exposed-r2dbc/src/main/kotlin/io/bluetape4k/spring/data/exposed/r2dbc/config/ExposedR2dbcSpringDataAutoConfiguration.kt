package io.bluetape4k.spring.data.exposed.r2dbc.config

import io.bluetape4k.spring.data.exposed.jdbc.mapping.ExposedMappingContext
import org.jetbrains.exposed.v1.dao.EntityClass
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.bluetape4k.spring.data.exposed.jdbc.config.ExposedSpringDataAutoConfiguration as JdbcExposedSpringDataAutoConfiguration

/**
 * 코루틴 기반 Spring Data Exposed 자동 설정입니다.
 * Phase 1의 [JdbcExposedSpringDataAutoConfiguration] 이후에 실행됩니다.
 */
@AutoConfiguration(after = [JdbcExposedSpringDataAutoConfiguration::class])
@ConditionalOnClass(EntityClass::class)
@Configuration(proxyBeanMethods = false)
class ExposedR2dbcSpringDataAutoConfiguration {

    /**
     * Phase 1 AutoConfiguration에 의해 이미 등록된 경우 생략합니다.
     */
    @Bean
    @ConditionalOnMissingBean
    fun exposedMappingContext(): ExposedMappingContext = ExposedMappingContext()
}
