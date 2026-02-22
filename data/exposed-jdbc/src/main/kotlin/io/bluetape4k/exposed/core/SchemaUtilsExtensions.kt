package io.bluetape4k.exposed.core

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.warn
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.migration.jdbc.MigrationUtils

private val log = KotlinLogging.logger { }

/**
 * [SchemaUtils.createMissingTablesAndColumns] 의 대안으로,
 * 누락 테이블 생성과 스키마 변경 SQL 실행을 분리하여 수행합니다.
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
