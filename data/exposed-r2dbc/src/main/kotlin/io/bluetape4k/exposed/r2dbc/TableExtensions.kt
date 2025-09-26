package io.bluetape4k.exposed.r2dbc

import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Sequence
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.ColumnMetadata
import org.jetbrains.exposed.v1.core.vendors.PrimaryKeyMetadata
import org.jetbrains.exposed.v1.r2dbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.r2dbc.vendors.currentDialectMetadata

/**
 * 테이블의 컬럼 메타데이터를 비동기적으로 조회합니다.
 *
 * @receiver [Table] 메타데이터를 조회할 테이블
 * @return 컬럼 메타데이터 리스트
 */
suspend fun Table.suspendColumnMetadata(): List<ColumnMetadata> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.tableColumns(this)[this].orEmpty()
}

/**
 * 테이블의 인덱스 정보를 비동기적으로 조회합니다.
 *
 * @receiver [Table] 인덱스를 조회할 테이블
 * @return 인덱스 리스트
 */
suspend fun Table.suspendIndexes(): List<Index> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingIndices(this)[this].orEmpty()
}

/**
 * 테이블의 기본키 메타데이터를 비동기적으로 조회합니다.
 *
 * @receiver [Table] 기본키를 조회할 테이블
 * @return 기본키 메타데이터, 없으면 null
 */
suspend fun Table.suspendPrimaryKeyMetadata(): PrimaryKeyMetadata? {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingPrimaryKeys(this)[this]
}

/**
 * 테이블의 시퀀스 정보를 비동기적으로 조회합니다.
 *
 * @receiver [Table] 시퀀스를 조회할 테이블
 * @return 시퀀스 리스트
 */
suspend fun Table.suspendSequences(): List<Sequence> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingSequences(this)[this].orEmpty()
}
