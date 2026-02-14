package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils

/**
 * 테스트 실행 전후에 [schemas]를 생성/삭제하고 [statement]를 실행합니다.
 *
 * 현재 방언이 스키마 생성(`CREATE SCHEMA`)을 지원하지 않으면 [statement]는 실행하지 않습니다.
 */
suspend fun withSchemas(
    dialect: TestDB,
    vararg schemas: Schema,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},
    statement: suspend R2dbcTransaction.() -> Unit,
) {
    withDb(dialect, configure = configure) {
        if (currentDialectTest.supportsCreateSchema) {
            SchemaUtils.createSchema(*schemas)
            var statementFailure: Throwable? = null
            try {
                statement()
                commit()     // Need commit to persist data before drop schemas
            } catch (ex: Throwable) {
                statementFailure = ex
                throw ex
            } finally {
                val dropFailure = runCatching {
                    SchemaUtils.dropSchema(*schemas, cascade = true)
                    commit()
                }.exceptionOrNull()

                when {
                    statementFailure != null && dropFailure != null -> statementFailure.addSuppressed(dropFailure)
                    statementFailure == null && dropFailure != null -> throw dropFailure
                }
            }
        }
    }
}
