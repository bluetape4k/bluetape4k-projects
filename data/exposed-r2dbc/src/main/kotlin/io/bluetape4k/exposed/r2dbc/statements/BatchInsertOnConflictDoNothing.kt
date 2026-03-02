package io.bluetape4k.exposed.r2dbc.statements

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.statements.BatchInsertSuspendExecutable

/**
 * Batch Insert 시, 충돌이 발생하면 아무 동작도 하지 않는 Executable 클래스입니다.
 *
 * ## 동작/계약
 * - [BatchInsertOnConflictDoNothing]를 감싸 R2DBC suspend 실행 경로에 연결합니다.
 * - SQL 생성 규칙은 statement 구현을 그대로 따릅니다.
 *
 * ```kotlin
 * val stmt = BatchInsertOnConflictDoNothing(UserTable)
 * val executable = BatchInsertOnConflictDoNothingExecutable(stmt)
 * // executable.statement === stmt
 * ```
 *
 * @property statement 실행할 BatchInsertOnConflictDoNothing 문장입니다.
 */
class BatchInsertOnConflictDoNothingExecutable(
    override val statement: BatchInsertOnConflictDoNothing,
): BatchInsertSuspendExecutable<BatchInsertOnConflictDoNothing>(statement)

/**
 * Batch Insert 시, 충돌이 발생하면 아무 동작도 하지 않는 BatchInsertStatement 구현체입니다.
 *
 * ## 동작/계약
 * - MySQL 계열은 `INSERT IGNORE`로 SQL을 변환합니다.
 * - 그 외 dialect는 `ON CONFLICT ... DO NOTHING`을 뒤에 추가합니다.
 * - PostgreSQL은 충돌 타깃을 `(id)`로 고정합니다.
 *
 * ```kotlin
 * val stmt = BatchInsertOnConflictDoNothing(UserTable)
 * val sql = stmt.prepareSQL(transaction, prepared = false)
 * // sql.contains("DO NOTHING") == true
 * ```
 *
 * @param table 데이터를 삽입할 테이블입니다.
 */
class BatchInsertOnConflictDoNothing(
    table: Table,
): BatchInsertStatement(table) {
    /** DB Dialect에 맞는 `ON CONFLICT DO NOTHING` SQL을 생성합니다. */
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
