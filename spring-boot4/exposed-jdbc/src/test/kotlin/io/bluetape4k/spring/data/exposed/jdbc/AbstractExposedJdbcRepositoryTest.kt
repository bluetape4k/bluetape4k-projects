package io.bluetape4k.spring.data.exposed.jdbc

import io.bluetape4k.logging.KLogging
import io.bluetape4k.spring.data.exposed.jdbc.domain.Users
import io.bluetape4k.spring.data.exposed.jdbc.repository.config.EnableExposedJdbcRepositories
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils
import org.jetbrains.exposed.v1.spring7.transaction.SpringTransactionManager
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

@SpringBootTest(classes = [AbstractExposedJdbcRepositoryTest.TestConfig::class])
abstract class AbstractExposedJdbcRepositoryTest {

    companion object: KLogging()

    @Autowired
    private lateinit var dataSource: DataSource

    @Volatile
    private var connected: Boolean = false

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

    @BeforeEach
    fun setUp() {
        if (!connected) {
            synchronized(this) {
                if (!connected) {
                    Database.connect(dataSource)
                    connected = true
                }
            }
        }
        transaction {
            MigrationUtils.statementsRequiredForDatabaseMigration(Users, withLogs = false)
                .asSequence()
                .filterNot { it.contains("DROP ", ignoreCase = true) }
                .distinct()
                .forEach { stmt ->
                    runCatching { exec(stmt) }
                        .onFailure { ex ->
                            val message = ex.message.orEmpty()
                            val duplicateObject = message.contains("already exists", ignoreCase = true) ||
                                    message.contains("90045")
                            if (!duplicateObject) throw ex
                        }
                }
            Users.deleteAll()
        }
    }
}
