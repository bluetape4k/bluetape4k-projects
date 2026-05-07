package io.bluetape4k.r2dbc.connection.init

import io.bluetape4k.logging.KLogging
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource
import org.springframework.r2dbc.connection.init.CompositeDatabasePopulator
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator

/**
 * [CompositeDatabasePopulator], [ResourceDatabasePopulator], [ConnectionFactoryInitializer] DSL 헬퍼를 검증합니다.
 */
class ConnectionInitTest {

    companion object: KLogging()

    private fun h2Factory() = ConnectionFactories.get(
        ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "h2")
            .option(ConnectionFactoryOptions.PROTOCOL, "mem")
            .option(ConnectionFactoryOptions.DATABASE, "init_test_${System.nanoTime()}")
            .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            .build()
    )

    /**
     * [resourceDatabasePopulatorOf]는 단일 리소스로 [ResourceDatabasePopulator]를 생성해야 합니다.
     */
    @Test
    fun `resourceDatabasePopulatorOf - 단일 Resource로 생성한다`() {
        val resource = ClassPathResource("schema/h2/person.sql")
        val populator = resourceDatabasePopulatorOf(resource)

        populator.shouldNotBeNull()
        populator.shouldBeInstanceOf<ResourceDatabasePopulator>()
    }

    /**
     * [resourceDatabasePopulatorOf]는 여러 리소스로 [ResourceDatabasePopulator]를 생성해야 합니다.
     */
    @Test
    fun `resourceDatabasePopulatorOf - 여러 Resource로 생성한다`() {
        val r1 = ClassPathResource("schema/h2/person.sql")
        val r2 = ClassPathResource("schema/h2/groupby.sql")
        val populator = resourceDatabasePopulatorOf(r1, r2)

        populator.shouldNotBeNull()
        populator.shouldBeInstanceOf<ResourceDatabasePopulator>()
    }

    /**
     * [compositeDatabasePopulatorOf]는 여러 populator를 묶는 [CompositeDatabasePopulator]를 생성해야 합니다.
     */
    @Test
    fun `compositeDatabasePopulatorOf - vararg populator 로 생성한다`() {
        val p1 = resourceDatabasePopulatorOf(ClassPathResource("schema/h2/person.sql"))
        val p2 = resourceDatabasePopulatorOf(ClassPathResource("schema/h2/groupby.sql"))

        val composite = compositeDatabasePopulatorOf(p1, p2)

        composite.shouldNotBeNull()
        composite.shouldBeInstanceOf<CompositeDatabasePopulator>()
    }

    /**
     * [compositeDatabasePopulatorOf]는 Collection으로도 생성할 수 있어야 합니다.
     */
    @Test
    fun `compositeDatabasePopulatorOf - Collection으로 생성한다`() {
        val populators = listOf(
            resourceDatabasePopulatorOf(ClassPathResource("schema/h2/person.sql")),
            resourceDatabasePopulatorOf(ClassPathResource("schema/h2/groupby.sql"))
        )

        val composite = compositeDatabasePopulatorOf(populators)

        composite.shouldNotBeNull()
        composite.shouldBeInstanceOf<CompositeDatabasePopulator>()
    }

    /**
     * [connectionFactoryInitializer]는 [ConnectionFactoryInitializer]를 생성하고 설정을 적용해야 합니다.
     */
    @Test
    fun `connectionFactoryInitializer - ConnectionFactory와 함께 생성한다`() {
        val factory = h2Factory()

        val initializer = connectionFactoryInitializer(factory) {
            setDatabasePopulator(
                compositeDatabasePopulatorOf(
                    resourceDatabasePopulatorOf(ClassPathResource("schema/h2/person.sql"))
                )
            )
        }

        initializer.shouldNotBeNull()
    }
}
