package io.bluetape4k.exposed.r2dbc.statements

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.statements.BatchInsertSuspendExecutable

class BatchInsertOnConflictDoNothingExecutable(
    override val statement: BatchInsertOnConflictDoNothing,
): BatchInsertSuspendExecutable<BatchInsertOnConflictDoNothing>(statement)

class BatchInsertOnConflictDoNothing(
    table: Table,
): BatchInsertStatement(table) {
    override fun prepareSQL(transaction: Transaction, prepared: Boolean) = buildString {
        val insertStatement = super.prepareSQL(transaction, prepared)

        when (val dialect = transaction.db.dialect) {
            is MysqlDialect -> {
                append("INSERT IGNORE ")
                append(insertStatement.substringAfter("INSERT "))
            }

            else -> {
                append(insertStatement)
                val identifier = if (dialect is PostgreSQLDialect) "(id)" else ""
                append(" ON CONFLICT $identifier DO NOTHING")
            }
        }
    }
}
