package io.bluetape4k.spring.data.exposed.r2dbc

import io.bluetape4k.exposed.r2dbc.tests.AbstractExposedR2dbcTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.spring.data.exposed.r2dbc.repository.config.EnableExposedR2dbcRepositories
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Configuration

@SpringBootTest(classes = [AbstractExposedR2dbcRepositoryTest.TestConfig::class])
abstract class AbstractExposedR2dbcRepositoryTest: AbstractExposedR2dbcTest() {

    companion object: KLoggingChannel()

    @Configuration
    @EnableAutoConfiguration
    @EnableExposedR2dbcRepositories(
        basePackages = ["io.bluetape4k.spring.data.exposed.r2dbc.repository"]
    )
    class TestConfig

}
