package io.bluetape4k.exposed.core

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.vendors.MysqlDialect
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect

/**
 * 배치 INSERT SQL을 충돌 무시(`ON CONFLICT DO NOTHING`/`INSERT IGNORE`) 형태로 변환하는 구문 클래스입니다.
 *
 * ## 동작/계약
 * - MySQL 계열은 `INSERT IGNORE`로 변환하고, PostgreSQL은 `ON CONFLICT (...) DO NOTHING`을 붙입니다.
 * - PostgreSQL에서 테이블 PK가 있으면 PK 컬럼 목록을 conflict target으로 사용하고, 없으면 target 없이 처리합니다.
 * - SQL 문자열만 재작성하며 배치 데이터/트랜잭션 상태는 직접 mutate 하지 않습니다.
 *
 * ```kotlin
 * val stmt = BatchInsertOnConflictDoNothing(table)
 * val sql = stmt.prepareSQL(transaction, prepared = true)
 * // sql.contains("DO NOTHING") || sql.contains("INSERT IGNORE")
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
