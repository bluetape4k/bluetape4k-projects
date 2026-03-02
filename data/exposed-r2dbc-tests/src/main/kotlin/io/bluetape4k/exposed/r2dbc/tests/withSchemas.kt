package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils

/**
 * 테스트 실행 전후에 [schemas]를 생성/삭제하고 [statement]를 실행합니다.
 *
 * 현재 방언이 스키마 생성(`CREATE SCHEMA`)을 지원하지 않으면 [statement]는 실행하지 않습니다.
 *
 * ## 동작/계약
 * - 지원 dialect에서만 `createSchema`/`dropSchema(cascade = true)`를 수행합니다.
 * - statement 실패와 drop 실패가 모두 발생하면 drop 예외를 suppressed로 추가합니다.
 * - statement가 성공했지만 drop이 실패하면 drop 예외를 호출자에게 전파합니다.
 *
 * ```kotlin
 * withSchemas(TestDB.POSTGRESQL, Schema("s1")) {
 *     // 스키마 s1 기준 테스트 실행
 * }
 * // 종료 시 s1 정리
 * ```
 */
suspend fun withSchemas(
    dialect: TestDB,
    vararg schemas: Schema,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
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
