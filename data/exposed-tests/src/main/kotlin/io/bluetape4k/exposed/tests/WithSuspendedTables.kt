package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.error
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.inTopLevelTransaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import kotlin.coroutines.CoroutineContext

suspend fun withSuspendedTables(
    testDB: TestDB,
    vararg tables: Table,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend Transaction.(TestDB) -> Unit,
) {
    withSuspendedDb(testDB, context, configure) {
        try {
            SchemaUtils.drop(*tables)
        } catch (_: Throwable) {
        }

        SchemaUtils.create(*tables)
        try {
            statement(testDB)
            commit()
        } finally {
            try {
                SchemaUtils.drop(*tables)
                commit()
            } catch (ex: Throwable) {
                logger.error(ex) { "Fail to drop tables, ${tables.joinToString { it.tableName }}" }
                val database = testDB.db!!
                inTopLevelTransaction(database.transactionManager.defaultIsolationLevel, db = database) {
                    maxAttempts = 1
                    SchemaUtils.drop(*tables)
                }
            }
        }
    }
}
