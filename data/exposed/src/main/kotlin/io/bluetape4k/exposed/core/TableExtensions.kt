package io.bluetape4k.exposed.core

import org.jetbrains.exposed.v1.core.Index
import org.jetbrains.exposed.v1.core.Sequence
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.vendors.ColumnMetadata
import org.jetbrains.exposed.v1.core.vendors.PrimaryKeyMetadata
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager
import org.jetbrains.exposed.v1.jdbc.vendors.currentDialectMetadata

/**
 * 테이블의 컬럼 메타데이터 목록을 반환합니다.
 *
 * @receiver [Table] Exposed 테이블 객체
 * @return 컬럼 메타데이터 리스트
 */
fun Table.getColumnMetadata(): List<ColumnMetadata> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.tableColumns(this)[this].orEmpty()
}

/**
 * 테이블에 정의된 인덱스 목록을 반환합니다.
 *
 * @receiver [Table] Exposed 테이블 객체
 * @return 인덱스 리스트
 */
fun Table.getIndices(): List<Index> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingIndices(this)[this].orEmpty()
}

/**
 * 테이블의 기본키 메타데이터를 반환합니다.
 *
 * @receiver [Table] Exposed 테이블 객체
 * @return 기본키 메타데이터, 없으면 null
 */
fun Table.getPrimaryKeyMetadata(): PrimaryKeyMetadata? {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingPrimaryKeys(this)[this]
}

/**
 * 테이블에 연결된 시퀀스 목록을 반환합니다.
 *
 * @receiver [Table] Exposed 테이블 객체
 * @return 시퀀스 리스트
 */
fun Table.getSequences(): List<Sequence> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingSequences(this)[this].orEmpty()
}
