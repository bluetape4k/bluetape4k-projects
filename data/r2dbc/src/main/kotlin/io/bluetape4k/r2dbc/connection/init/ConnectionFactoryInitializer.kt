package io.bluetape4k.r2dbc.connection.init

import io.r2dbc.spi.ConnectionFactory
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer

/**
 * [connectionFactory]를 초기화하는 [ConnectionFactoryInitializer]를 생성한다.
 *
 * ```
 * val initializer = connectionFactoryInitializer(connectionFactory) {
 *    setDatabasePopulator(compositeDatabasePopulatorOf(
 *      resourceDatabasePopulatorOf("schema.sql"),
 *      resourceDatabasePopulatorOf("data.sql")
 *    )
 * }
 * ```
 *
 * @param connectionFactory 초기화할 [ConnectionFactory]
 * @param initializer [ConnectionFactoryInitializer]를 초기화하는 람다
 * @return 초기화된 [ConnectionFactoryInitializer]
 */
inline fun connectionFactoryInitializer(
    connectionFactory: ConnectionFactory,
    initializer: ConnectionFactoryInitializer.() -> Unit = {},
): ConnectionFactoryInitializer {
    return ConnectionFactoryInitializer().apply {
        setConnectionFactory(connectionFactory)
        initializer()
    }
}
