package io.bluetape4k.r2dbc.core

import io.bluetape4k.logging.KLogging
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient

/**
 * [databaseClient] DSL 빌더 함수들을 검증합니다.
 *
 * - 람다 블록으로 [DatabaseClient]를 올바르게 생성하는지 확인합니다.
 * - [ConnectionFactory]를 주입할 때 connectionFactory 설정이 반영되는지 확인합니다.
 */
class DatabaseClientBuilderTest {

    companion object: KLogging()

    private fun createH2Factory(): ConnectionFactory {
        val options = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "h2")
            .option(ConnectionFactoryOptions.PROTOCOL, "mem")
            .option(ConnectionFactoryOptions.DATABASE, "builder_test_${System.nanoTime()}")
            .option(Option.valueOf("DB_CLOSE_DELAY"), "-1")
            .build()
        return ConnectionFactories.get(options)
    }

    /**
     * [databaseClient] 블록 빌더로 [DatabaseClient]를 생성할 수 있어야 합니다.
     */
    @Test
    fun `databaseClient 블록으로 DatabaseClient 를 생성한다`() {
        val factory = createH2Factory()

        val client = databaseClient {
            connectionFactory(factory)
            namedParameters(true)
        }

        client.shouldNotBeNull()
        client.shouldBeInstanceOf<DatabaseClient>()
    }

    /**
     * [ConnectionFactory]를 직접 전달하면 올바른 [DatabaseClient]가 생성되어야 합니다.
     */
    @Test
    fun `databaseClient(factory) 로 DatabaseClient 를 생성한다`() {
        val factory = createH2Factory()

        val client = databaseClient(factory)

        client.shouldNotBeNull()
        client.shouldBeInstanceOf<DatabaseClient>()
        client.connectionFactory shouldBeInstanceOf factory::class
    }

    /**
     * [ConnectionFactory]와 추가 블록 설정이 함께 적용되어야 합니다.
     */
    @Test
    fun `databaseClient(factory, block) 로 추가 설정이 적용된다`() {
        val factory = createH2Factory()

        val client = databaseClient(factory) {
            namedParameters(true)
        }

        client.shouldNotBeNull()
        client.shouldBeInstanceOf<DatabaseClient>()
    }
}
