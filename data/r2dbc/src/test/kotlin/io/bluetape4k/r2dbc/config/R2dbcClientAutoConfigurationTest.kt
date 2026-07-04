package io.bluetape4k.r2dbc.config

import io.bluetape4k.assertions.shouldBeEmpty
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeSameInstanceAs
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.r2dbc.R2dbcClient
import io.r2dbc.spi.ConnectionFactories
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.getBeansOfType
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.FilteredClassLoader
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import java.util.function.Supplier

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class R2dbcClientAutoConfigurationTest {

    private val entityTemplate = R2dbcEntityTemplate(
        ConnectionFactories.get("r2dbc:h2:mem:///r2dbc_auto_config_${System.nanoTime()}")
    )

    private val databaseClient = entityTemplate.databaseClient

    private val mappingR2dbcConverter = entityTemplate.converter as MappingR2dbcConverter

    private val autoConfigurationRunner = ApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(R2dbcClientAutoConfiguration::class.java)
        )

    private val r2dbcClientRunner = autoConfigurationRunner
        .withBean(R2dbcEntityTemplate::class.java, Supplier { entityTemplate })
        .withBean(MappingR2dbcConverter::class.java, Supplier { mappingR2dbcConverter })
        .withBean(DatabaseClient::class.java, Supplier { databaseClient })

    @Test
    fun `registers R2dbcClient when required Spring R2DBC infrastructure beans exist`() {
        r2dbcClientRunner.run { context ->
            context.getStartupFailure() shouldBeEqualTo null
            context.getBeansOfType<R2dbcClient>().shouldHaveSize(1)
        }
    }

    @Test
    fun `backs off when application provides custom R2dbcClient bean`() {
        val customClient = R2dbcClient(databaseClient, entityTemplate, mappingR2dbcConverter)

        r2dbcClientRunner
            .withBean(R2dbcClient::class.java, Supplier { customClient })
            .run { context ->
                context.getStartupFailure() shouldBeEqualTo null
                val clients = context.getBeansOfType<R2dbcClient>()

                clients.shouldHaveSize(1)
                clients.values.single() shouldBeSameInstanceAs customClient
            }
    }

    @Test
    fun `does not activate when Spring Data R2DBC signature types are absent`() {
        autoConfigurationRunner
            .withClassLoader(
                FilteredClassLoader(
                    "org.springframework.data.r2dbc.core.R2dbcEntityTemplate",
                    "org.springframework.data.r2dbc.convert.MappingR2dbcConverter",
                )
            )
            .run { context ->
                context.getStartupFailure() shouldBeEqualTo null
                context.beanFactory.getBeanNamesForType(R2dbcClient::class.java).asList().shouldBeEmpty()
            }
    }
}
