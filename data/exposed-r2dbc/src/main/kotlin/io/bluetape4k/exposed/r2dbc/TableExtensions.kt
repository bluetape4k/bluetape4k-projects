package io.bluetape4k.exposed.r2dbc

import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Sequence
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.ColumnMetadata
import org.jetbrains.exposed.v1.core.vendors.PrimaryKeyMetadata
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.vendors.currentDialectMetadata

suspend fun Table.suspendColumnMetadata(): List<ColumnMetadata> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.tableColumns(this)[this].orEmpty()
}

suspend fun Table.suspendIndexes(): List<Index> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingIndices(this)[this].orEmpty()
}

suspend fun Table.suspendPrimaryKeyMetadata(): PrimaryKeyMetadata? {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingPrimaryKeys(this)[this]
}

suspend fun Table.suspendSequences(): List<Sequence> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingSequences(this)[this].orEmpty()
}
