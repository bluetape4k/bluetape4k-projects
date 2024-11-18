package io.bluetape4k.r2dbc

import io.r2dbc.spi.ConnectionFactory
import org.springframework.data.r2dbc.convert.MappingR2dbcConverter
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient

/**
 * R2DBC 주요 인스턴스를 묵어서 사용하기 위해 만든 클래스입니다.
 *
 * @property databaseClient
 * @property entityTemplate
 * @property mappingConverter
 */
class R2dbcClient(
    val databaseClient: DatabaseClient,
    val entityTemplate: R2dbcEntityTemplate,
    @PublishedApi
    internal val mappingConverter: MappingR2dbcConverter,
)

/**
 * [ConnectionFactory] 인스턴스
 */
val R2dbcClient.connectionFactory: ConnectionFactory
    get() = databaseClient.connectionFactory
