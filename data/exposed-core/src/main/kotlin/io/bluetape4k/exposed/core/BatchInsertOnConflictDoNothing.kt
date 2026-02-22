package io.bluetape4k.exposed.core

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect

/**
 * Batch Insert with ON CONFLICT DO NOTHING - 예외 발생 시 해당 예외를 무시하고,
 * 다음 작업을 수행하도록 하는 [BatchInsertStatement] 구현체입니다.
 *
 * ```kotlin
 * val tester = object: Table("tester") {
 *     val id = varchar("id", 10).uniqueIndex()
 * }
 * withTables(testDB, tester) {
 *     tester.insert { it[id] = "foo" }
 *
 *     val numInserted = BatchInsertOnConflictIgnore(tester).run {
 *         addBatch()
 *         this[tester.id] = "foo"        // 중복되므로 insert 되지 않음
 *
 *         addBatch()
 *         this[tester.id] = "bar"        // 중복되지 않으므로 추가됨
 *
 *         execute(this@withTables)
 *     }
 *     numInserted shouldBeEqualTo 1
 * }
 * ```
 */
open class BatchInsertOnConflictDoNothing(table: Table): BatchInsertStatement(table) {
    override fun prepareSQL(transaction: Transaction, prepared: Boolean): String = buildString {
        val insertStatement = super.prepareSQL(transaction, prepared)

        when (val dialect = transaction.db.dialect) {
            is MysqlDialect -> {
                append("INSERT IGNORE ")
                append(insertStatement.substringAfter("INSERT "))
            }

            else            -> {
                append(insertStatement)
                if (dialect is PostgreSQLDialect) {
                    // 실제 PK 컬럼들을 사용 (복합 키 지원)
                    val pkColumns = table.primaryKey?.columns
                        ?.joinToString(", ") { transaction.identity(it) }
                    val identifier = if (pkColumns != null) "($pkColumns)" else ""
                    append(" ON CONFLICT $identifier DO NOTHING")
                } else {
                    append(" ON CONFLICT DO NOTHING")
                }
            }
        }
    }
}
