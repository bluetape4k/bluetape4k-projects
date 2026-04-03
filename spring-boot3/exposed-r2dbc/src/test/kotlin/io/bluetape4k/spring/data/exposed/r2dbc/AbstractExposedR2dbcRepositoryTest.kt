package io.bluetape4k.spring.data.exposed.r2dbc

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.data.exposed.r2dbc.domain.Users
import io.bluetape4k.spring.data.exposed.r2dbc.repository.config.EnableExposedR2dbcRepositories
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration

@SpringBootTest(classes = [AbstractExposedR2dbcRepositoryTest.TestConfig::class])
abstract class AbstractExposedR2dbcRepositoryTest {

    companion object : KLoggingChannel() {
        val r2dbcDatabase: R2dbcDatabase = R2dbcDatabase.connect(
            url = "r2dbc:h2:mem:///coroutine_exposed_test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=LEGACY",
            driver = "h2",
        )
    }

    @Configuration
    @EnableAutoConfiguration
    @EnableExposedR2dbcRepositories(
        basePackages = ["io.bluetape4k.spring.data.exposed.r2dbc.repository"]
    )
    class TestConfig

    @BeforeEach
    fun setUp(): Unit = runBlocking {
        suspendTransaction(r2dbcDatabase) {
            SchemaUtils.createMissingTablesAndColumns(Users)
            Users.deleteAll()
        }
    }
}
