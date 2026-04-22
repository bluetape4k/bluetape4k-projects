package io.bluetape4k.spring.data.exposed.jdbc.config

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.mapping.ExposedMappingContext
import io.bluetape4k.spring.data.exposed.jdbc.repository.config.EnableExposedJdbcRepositories
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.spring.transaction.SpringTransactionManager
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@SpringBootTest(classes = [ExposedSpringDataAutoConfigurationTest.TestConfig::class])
class ExposedSpringDataAutoConfigurationTest {

    companion object : KLogging()

    @Configuration
    @EnableAutoConfiguration(
        excludeName = [
            "org.jetbrains.exposed.v1.spring.boot.autoconfigure.ExposedAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.jdbc.JdbcRepositoriesAutoConfiguration",
        ]
    )
    @EnableExposedJdbcRepositories(basePackages = ["io.bluetape4k.spring.data.exposed.jdbc.repository"])
    class TestConfig {
        @Bean("springTransactionManager")
        fun springTransactionManager(dataSource: DataSource): PlatformTransactionManager =
            SpringTransactionManager(dataSource, DatabaseConfig {}, false)
    }

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var exposedMappingContext: ExposedMappingContext

    @Test
    fun `ExposedMappingContext bean is registered in context`() {
        exposedMappingContext.shouldNotBeNull()
    }

    @Test
    fun `ApplicationContext contains ExposedMappingContext bean`() {
        val bean = applicationContext.getBean(ExposedMappingContext::class.java)
        bean.shouldNotBeNull()
    }

    @Test
    fun `ApplicationContext contains PlatformTransactionManager bean`() {
        val txManager = applicationContext.getBean(PlatformTransactionManager::class.java)
        txManager.shouldNotBeNull()
    }

    @Test
    fun `ExposedMappingContext can resolve UserEntity persistent entity`() {
        val entity = exposedMappingContext.getRequiredPersistentEntity(
            io.bluetape4k.spring.data.exposed.jdbc.domain.UserEntity::class.java
        )
        entity.shouldNotBeNull()
        entity.getTable().shouldNotBeNull()
    }
}
