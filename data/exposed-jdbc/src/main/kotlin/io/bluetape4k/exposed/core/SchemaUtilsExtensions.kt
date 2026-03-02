package io.bluetape4k.exposed.core

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

private val log = KotlinLogging.logger { }

/**
 * 누락 테이블 생성과 마이그레이션 SQL 적용을 단계별로 수행합니다.
 *
 * ## 동작/계약
 * - `createMissingTablesAndColumns`를 한 번에 호출하지 않고, 생성/마이그레이션/누락 컬럼 추가를 분리 실행합니다.
 * - 테이블 생성 및 마이그레이션 SQL 생성/실행 예외는 경고 로그만 남기고 진행합니다.
 * - 마지막 `addMissingColumnsStatements` 실행은 `runCatching`으로 감싸지지 않으므로 예외가 호출자에게 전파됩니다.
 *
 * ```kotlin
 * transaction {
 *   execCreateMissingTablesAndColumns(Users, Orders)
 *   // Users, Orders 스키마가 현재 DB 상태에 맞게 보정됨
 * }
 * ```
 */
fun JdbcTransaction.execCreateMissingTablesAndColumns(vararg tables: Table) {
    val self = this
    runCatching {
        val existingTables = SchemaUtils.listTables()
        val missingTables = tables.filterNot { table ->
            existingTables.any { it.equals(table.tableName, ignoreCase = true) }
        }
        SchemaUtils.create(*missingTables.toTypedArray())
    }.onFailure { ex ->
        log.warn(ex) { "누락 테이블 생성 중 예외가 발생했습니다. tables=${tables.joinToString { it.tableName }}" }
    }
    runCatching {
        MigrationUtils.statementsRequiredForDatabaseMigration(*tables).apply {
            if (isNotEmpty()) {
                self.exec(joinToString(";"))
            }
        }
    }.onFailure { ex ->
        log.warn(ex) { "마이그레이션 SQL 생성/실행 중 예외가 발생했습니다. tables=${tables.joinToString { it.tableName }}" }
    }

    SchemaUtils.addMissingColumnsStatements(*tables).apply {
        if (isNotEmpty()) {
            self.exec(joinToString(";"))
        }
    }
}
