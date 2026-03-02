package io.bluetape4k.r2dbc

import io.r2dbc.spi.ConnectionFactory
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient

/**
 * R2DBC 핵심 컴포넌트를 한 객체로 묶어 전달합니다.
 *
 * ## 동작/계약
 * - [databaseClient], [entityTemplate], [mappingConverter] 참조를 그대로 보관합니다.
 * - 생성자에서 추가 검증/변환을 수행하지 않습니다.
 * - 상태를 따로 관리하지 않는 단순 holder 객체입니다.
 *
 * ```kotlin
 * val client = R2dbcClient(databaseClient, entityTemplate, mappingConverter)
 * val same = client.databaseClient === databaseClient
 * // same == true
 * ```
 */
class R2dbcClient(
    val databaseClient: DatabaseClient,
    val entityTemplate: R2dbcEntityTemplate,
    @PublishedApi
    internal val mappingConverter: MappingR2dbcConverter,
)

/**
 * [R2dbcClient]에서 사용하는 [ConnectionFactory]를 노출합니다.
 *
 * ## 동작/계약
 * - `databaseClient.connectionFactory`를 그대로 반환합니다.
 * - 새 커넥션을 생성하지 않으며 객체 할당이 없습니다.
 * - 수신 객체 상태를 변경하지 않습니다.
 *
 * ```kotlin
 * val cf = client.connectionFactory
 * // cf === client.databaseClient.connectionFactory
 * ```
 */
val R2dbcClient.connectionFactory: ConnectionFactory
    get() = databaseClient.connectionFactory
