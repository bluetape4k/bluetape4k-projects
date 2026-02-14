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
 */
suspend fun withTables(
    testDB: TestDB,
    vararg tables: Table,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend R2dbcTransaction.(TestDB) -> Unit,
) {
    withDb(testDB, configure = configure) {
        runCatching { SchemaUtils.drop(*tables) }

        if (tables.isNotEmpty()) {
            SchemaUtils.create(*tables)
        }

        try {
            statement(testDB)
            commit()  // Need commit to persist data before drop tables
        } finally {
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
