package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.inTopLevelSuspendTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager

/**
 * 테스트 실행 전후에 [tables]를 생성/삭제하고 [statement]를 실행합니다.
 *
 * [statement]에서 발생한 예외는 호출자에게 그대로 전파되어 테스트 실패로 반영됩니다.
 *
 * ## 동작/계약
 * - 실행 전 기존 테이블 드롭을 시도하고, 지정 테이블을 생성합니다.
 * - [dropTables]가 `true`면 종료 시 테이블 삭제를 시도합니다.
 * - 드롭 실패 시 top-level suspend transaction으로 한 번 더 정리합니다.
 *
 * ```kotlin
 * withTables(TestDB.H2, UtilityTable) {
 *     // 테스트 실행
 * }
 * // 기본값이면 종료 시 UtilityTable 정리
 * ```
 */
suspend fun withTables(
    testDB: TestDB,
    vararg tables: Table,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    dropTables: Boolean = true,
    statement: suspend R2dbcTransaction.(TestDB) -> Unit,
) {
    withDb(testDB, configure = configure) {
        runCatching {
            SchemaUtils.drop(*tables)
        }

        if (tables.isNotEmpty()) {
            SchemaUtils.create(*tables)
        }

        try {
            statement(testDB)
            commit()  // Need commit to persist data before drop tables
        } finally {
            if (dropTables) {
                try {
                    if (tables.isNotEmpty()) {
                        SchemaUtils.drop(*tables)
                        commit()
                    }
                } catch (_: Exception) {
                    val database = testDB.db!!
                    inTopLevelSuspendTransaction(
                        transactionIsolation = database.transactionManager.defaultIsolationLevel!!,
                        db = database,
                    ) {
                        maxAttempts = 1
                        runCatching { SchemaUtils.drop(*tables) }
                    }
                }
            }
        }
    }
}
