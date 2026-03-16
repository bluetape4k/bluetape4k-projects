package io.bluetape4k.r2dbc.core

import io.r2dbc.spi.ConnectionFactory
import org.springframework.r2dbc.core.DatabaseClient

/**
 * [DatabaseClient.Builder] DSL로 [DatabaseClient]를 생성합니다.
 *
 * ## 동작/계약
 * - `DatabaseClient.builder()`를 생성한 뒤 [builder]를 적용하고 `build()`를 호출합니다.
 * - [builder] 안에서 설정한 옵션만 반영되며 기본값은 Spring 기본 설정을 따릅니다.
 * - builder 블록 예외는 그대로 전파됩니다.
 *
 * ```kotlin
 * val client = databaseClient {
 *   connectionFactory(factory)
 *   namedParameters(true)
 * }
 * // client != null
 * ```
 */
inline fun databaseClient(
    builder: DatabaseClient.Builder.() -> Unit,
): DatabaseClient {
    return DatabaseClient.builder().also(builder).build()
}

/**
 * [ConnectionFactory]를 기본으로 설정한 [DatabaseClient]를 생성합니다.
 *
 * ## 동작/계약
 * - 내부적으로 `connectionFactory(factory)`를 먼저 적용한 뒤 [builder]를 실행합니다.
 * - [factory]는 null 허용 타입이 아니며 별도 검증 없이 그대로 전달됩니다.
 * - [builder]에서 `connectionFactory`를 다시 설정하면 마지막 설정값이 적용됩니다.
 *
 * ```kotlin
 * val client = databaseClient(factory) {
 *   namedParameters(true)
 * }
 * // client.connectionFactory == factory
 * ```
 */
inline fun databaseClient(
    factory: ConnectionFactory,
    builder: DatabaseClient.Builder.() -> Unit = {},
): DatabaseClient = databaseClient {
    connectionFactory(factory)
    builder()
}
