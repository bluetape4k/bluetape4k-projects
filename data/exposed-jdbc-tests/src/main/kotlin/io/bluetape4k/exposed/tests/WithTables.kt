package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.error
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager


/**
 * 테스트 블록 실행 전 테이블을 생성하고 종료 시 정리합니다.
 *
 * ## 동작/계약
 * - 시작 시 기존 테이블을 `runCatching`으로 드롭 시도 후 필요 시 새로 생성합니다.
 * - 블록 수행 후 `commit()`하고, [dropTables]가 `true`면 테이블 삭제를 시도합니다.
 * - 드롭 실패 시 top-level transaction으로 재시도합니다.
 *
 * ```kotlin
 * withTables(TestDB.H2, UtilityTable) {
 *     UtilityTable.exists()
 *     // result == true
 * }
 * ```
 */
fun withTables(
    testDB: TestDB,
    vararg tables: Table,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},  // @PrameterizedTest 시 db가 캐시됨. withDb에서 매번 database를 생성하도록 수정함
    dropTables: Boolean = true,
    statement: JdbcTransaction.(TestDB) -> Unit,
) {
    withDb(testDB, configure) {
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
                } catch (ex: Throwable) {
                    logger.error(ex) { "Drop Tables 에서 예외가 발생했습니다. 삭제할 테이블: ${tables.joinToString { it.tableName }}" }
                    val database = testDB.db!!
                    inTopLevelTransaction(
                        db = database,
                        transactionIsolation = database.transactionManager.defaultIsolationLevel
                    ) {
                        maxAttempts = 1
                        SchemaUtils.drop(*tables)
                    }
                }
            }
        }
    }
}
