package io.bluetape4k.exposed.core

import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Sequence
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.ColumnMetadata
import org.jetbrains.exposed.v1.core.vendors.PrimaryKeyMetadata
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.vendors.currentDialectMetadata

/**
 * 현재 트랜잭션에서 테이블 컬럼 메타데이터를 조회합니다.
 *
 * ## 동작/계약
 * - dialect metadata cache를 먼저 초기화한 뒤 현재 DB 메타데이터를 다시 조회합니다.
 * - 트랜잭션이 없으면 `TransactionManager.current()`에서 예외가 발생합니다.
 */
fun Table.getColumnMetadata(): List<ColumnMetadata> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.tableColumns(this)[this].orEmpty()
}

/** 현재 트랜잭션에서 테이블 인덱스 메타데이터를 조회합니다. */
fun Table.getIndices(): List<Index> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingIndices(this)[this].orEmpty()
}

/** 현재 트랜잭션에서 테이블 기본키 메타데이터를 조회합니다. */
fun Table.getPrimaryKeyMetadata(): PrimaryKeyMetadata? {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingPrimaryKeys(this)[this]
}

/** 현재 트랜잭션에서 테이블 관련 시퀀스 메타데이터를 조회합니다. */
fun Table.getSequences(): List<Sequence> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingSequences(this)[this].orEmpty()
}
