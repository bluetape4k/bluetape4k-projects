package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.error
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager


fun withTables(
    testDB: TestDB,
    vararg tables: Table,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},  // @PrameterizedTest 시 db가 캐시됨. withDb에서 매번 database를 생성하도록 수정함
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
            try {
                if (tables.isNotEmpty()) {
                    SchemaUtils.drop(*tables)
                    commit()
                }
            } catch (ex: Throwable) {
                logger.error(ex) { "Drop Tables 에서 예외가 발생했습니다. 삭제할 테이블: ${tables.joinToString { it.tableName }}" }
                val database = testDB.db!!
                inTopLevelTransaction(database.transactionManager.defaultIsolationLevel, db = database) {
                    maxAttempts = 1
                    SchemaUtils.drop(*tables)
                }
            }
        }
    }
}
