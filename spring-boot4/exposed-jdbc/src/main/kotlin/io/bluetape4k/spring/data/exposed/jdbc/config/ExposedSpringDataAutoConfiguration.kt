package io.bluetape4k.spring.data.exposed.jdbc.config

import io.bluetape4k.spring.data.exposed.jdbc.mapping.ExposedMappingContext
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * Spring Data Exposed 자동 설정입니다.
 * Exposed `EntityClass`가 classpath에 있을 때 활성화됩니다.
 *
 * ```kotlin
 * // Spring Boot 자동 등록 — 별도 설정 불필요
 * // @EnableExposedJdbcRepositories 어노테이션과 함께 사용됩니다.
 * @SpringBootApplication
 * @EnableExposedJdbcRepositories(basePackages = ["io.example.repository"])
 * class Application
 * ```
 */
@AutoConfiguration
@ConditionalOnClass(EntityClass::class)
@Configuration(proxyBeanMethods = false)
class ExposedSpringDataAutoConfiguration {

    @Bean
    fun exposedMappingContext(): ExposedMappingContext = ExposedMappingContext()

    @Bean("springTransactionManager")
    @ConditionalOnBean(DataSource::class)
    @ConditionalOnMissingBean(name = ["springTransactionManager"])
    fun springTransactionManager(dataSource: DataSource): PlatformTransactionManager {
        Database.connect(dataSource)
        return SpringTransactionManager(dataSource, DatabaseConfig {}, false)
    }
}
