package io.bluetape4k.exposed.tests

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import kotlin.coroutines.CoroutineContext

/**
 * 코루틴 환경에서 스키마를 생성하고 테스트 블록 실행 후 스키마를 정리합니다.
 *
 * ## 동작/계약
 * - 현재 dialect가 `supportsCreateSchema`일 때만 스키마 생성/삭제를 수행합니다.
 * - 블록 완료 후 `commit()`으로 변경 반영 후 `dropSchema(..., cascade = true)`를 실행합니다.
 * - [context]를 통해 suspended transaction 실행 컨텍스트를 지정할 수 있습니다.
 *
 * ```kotlin
 * withSchemasSuspending(TestDB.POSTGRESQL, Schema("s1")) {
 *     // schema s1 안에서 suspend 테스트 실행
 * }
 * // 종료 시 schema s1 정리
 * ```
 */
suspend fun withSchemasSuspending(
    dialect: TestDB,
    vararg schemas: Schema,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},
    context: CoroutineContext? = Dispatchers.IO,
    statement: suspend JdbcTransaction.() -> Unit,
) {
    withDbSuspending(dialect, configure = configure, context = context) {
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
