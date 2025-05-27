package io.bluetape4k.exposed.core

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.migration.MigrationUtils


/**
 * [SchemaUtils.createMissingTablesAndColumns] 를 대체하기 위한 함수입니다.
 */
fun JdbcTransaction.execCreateMissingTablesAndColumns(vararg tables: Table) {
    val self = this
    runCatching {
        val existingTables = SchemaUtils.listTables()
        val missingTables = tables.filterNot { existingTables.contains(it.tableName) }
        SchemaUtils.create(*missingTables.toTypedArray())
    }
    runCatching {
        MigrationUtils.statementsRequiredForDatabaseMigration(*tables).apply {
            if (isNotEmpty()) {
                self.exec(joinToString(";"))
            }
        }
    }

    SchemaUtils.addMissingColumnsStatements(*tables).apply {
        if (isNotEmpty()) {
            self.exec(joinToString(";"))
        }
    }
}
