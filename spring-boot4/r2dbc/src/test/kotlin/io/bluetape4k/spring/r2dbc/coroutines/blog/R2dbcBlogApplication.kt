package io.bluetape4k.spring.r2dbc.coroutines.blog

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.r2dbc.connection.init.connectionFactoryInitializer
import io.bluetape4k.r2dbc.connection.init.resourceDatabasePopulatorOf
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.WebApplicationType
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer

@SpringBootApplication
class R2dbcBlogApplication: AbstractR2dbcConfiguration() {

    companion object: KLoggingChannel()

    override fun connectionFactory(): ConnectionFactory {
        val url = "r2dbc:h2:mem:///test?options=DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        return ConnectionFactories.get(url)
    }

    @Bean
    fun initializer(connectionFactory: ConnectionFactory): ConnectionFactoryInitializer {
        val populator = CompositeDatabasePopulator().apply {
            addPopulators(resourceDatabasePopulatorOf(ClassPathResource("data/schema.sql")))
        }

        return connectionFactoryInitializer(connectionFactory) {
            setDatabasePopulator(populator)
        }
    }
}

fun main(vararg args: String) {
    runApplication<R2dbcBlogApplication>(*args) {
        webApplicationType = WebApplicationType.REACTIVE
    }
}
