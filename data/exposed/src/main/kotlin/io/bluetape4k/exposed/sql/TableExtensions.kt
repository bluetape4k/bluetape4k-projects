package io.bluetape4k.exposed.sql

import org.jetbrains.exposed.sql.Index
import org.jetbrains.exposed.sql.Sequence
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.vendors.ColumnMetadata
import org.jetbrains.exposed.sql.vendors.PrimaryKeyMetadata
import org.jetbrains.exposed.sql.vendors.currentDialect

fun Table.getColumnMetadata(): List<ColumnMetadata> {
    TransactionManager.current().db.dialect.resetCaches()
    return currentDialect.tableColumns(this)[this].orEmpty()
}

fun Table.getIndices(): List<Index> {
    TransactionManager.current().db.dialect.resetCaches()
    return currentDialect.existingIndices(this)[this].orEmpty()
}

fun Table.getPrimaryKeyMetadata(): PrimaryKeyMetadata? {
    TransactionManager.current().db.dialect.resetCaches()
    return currentDialect.existingPrimaryKeys(this)[this]
}

fun Table.getSequences(): List<Sequence> {
    TransactionManager.current().db.dialect.resetCaches()
    return currentDialect.existingSequences(this)[this].orEmpty()
}
