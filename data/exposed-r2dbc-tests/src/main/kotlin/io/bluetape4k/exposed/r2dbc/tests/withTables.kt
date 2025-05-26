package io.bluetape4k.exposed.r2dbc.tests

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.r2dbc.ExposedR2dbcException
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager

suspend fun withTables(
    testDB: TestDB,
    vararg tables: Table,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend R2dbcTransaction.(TestDB) -> Unit,
) {
    withDb(testDB, configure = configure) {
        runCatching { SchemaUtils.drop(*tables) }
        SchemaUtils.create(*tables)
        try {
            statement(testDB)
            commit()
        } catch (ex: ExposedR2dbcException) {
            println("Failed to execute statement: ${ex.message}")
        } finally {
            try {
                SchemaUtils.drop(*tables)
                commit()
            } catch (ex: Exception) {
                val database = testDB.db!!
                suspendTransaction(
                    db = database,
                    transactionIsolation = database.transactionManager.defaultIsolationLevel,
                ) {
                    maxAttempts = 1
                    SchemaUtils.drop(*tables)
                }
            }
        }
    }
}
