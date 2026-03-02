package io.bluetape4k.exposed.tests

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils


/**
 * 스키마를 생성한 뒤 테스트 블록을 실행하고 종료 시 스키마를 정리합니다.
 *
 * ## 동작/계약
 * - 현재 dialect가 `supportsCreateSchema`일 때만 스키마 생성/삭제를 수행합니다.
 * - 블록 완료 후 `commit()`으로 변경을 반영한 뒤 `dropSchema(..., cascade = true)`를 호출합니다.
 * - [configure]를 [withDb]에 전달해 DB 설정을 임시 변경할 수 있습니다.
 *
 * ```kotlin
 * withSchemas(TestDB.POSTGRESQL, Schema("s1")) {
 *     // schema s1 안에서 테스트 실행
 * }
 * // 종료 시 schema s1 정리
 * ```
 */
fun withSchemas(
    dialect: TestDB,
    vararg schemas: Schema,
    configure: (DatabaseConfig.Builder.() -> Unit)? = { },
    statement: JdbcTransaction.() -> Unit,
) {
    withDb(dialect, configure) {
        if (currentDialectTest.supportsCreateSchema) {
            SchemaUtils.createSchema(*schemas)
            try {
                statement()
                commit()     // Need commit to persist data before drop schemas
            } finally {
                SchemaUtils.dropSchema(*schemas, cascade = true)
                commit()
            }
        }
    }
}
