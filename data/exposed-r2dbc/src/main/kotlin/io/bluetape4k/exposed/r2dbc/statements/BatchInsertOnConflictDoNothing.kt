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
 * @property statement 실행할 BatchInsertOnConflictDoNothing 문장
 */
class BatchInsertOnConflictDoNothingExecutable(
    override val statement: BatchInsertOnConflictDoNothing,
): BatchInsertSuspendExecutable<BatchInsertOnConflictDoNothing>(statement)

/**
 * Batch Insert 시, 충돌이 발생하면 아무 동작도 하지 않는 BatchInsertStatement 구현체입니다.
 *
 * MySQL의 경우 `INSERT IGNORE`를, PostgreSQL의 경우 `ON CONFLICT DO NOTHING`을 사용합니다.
 *
 * @param table 데이터를 삽입할 테이블
 */
class BatchInsertOnConflictDoNothing(
    table: Table,
): BatchInsertStatement(table) {
    /**
     * DB Dialect에 따라 적절한 SQL을 생성합니다.
     *
     * @param transaction 현재 트랜잭션
     * @param prepared PreparedStatement 사용 여부
     * @return 생성된 SQL 문자열
     */
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
