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
 * ## 동작/계약
 * - 현재 R2DBC 트랜잭션의 dialect metadata cache를 초기화한 뒤 컬럼 메타데이터를 조회합니다.
 * - 트랜잭션이 없으면 `TransactionManager.current()`에서 예외가 발생합니다.
 *
 * ```kotlin
 * val metadatas = tester.suspendColumnMetadata()
 * // metadatas.isNotEmpty() == true
 * ```
 */
suspend fun Table.suspendColumnMetadata(): List<ColumnMetadata> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.tableColumns(this)[this].orEmpty()
}

/**
 * 테이블의 인덱스 정보를 비동기적으로 조회합니다.
 *
 * ## 동작/계약
 * - dialect metadata cache 초기화 후 인덱스 목록을 조회합니다.
 * - 인덱스가 없으면 빈 리스트를 반환합니다.
 *
 * ```kotlin
 * val indices = tester.suspendIndexes()
 * // indices.size >= 0
 * ```
 */
suspend fun Table.suspendIndexes(): List<Index> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingIndices(this)[this].orEmpty()
}

/**
 * 테이블의 기본키 메타데이터를 비동기적으로 조회합니다.
 *
 * ## 동작/계약
 * - dialect metadata cache 초기화 후 기본키 메타데이터를 조회합니다.
 * - 기본키가 정의되지 않은 테이블이면 `null`을 반환합니다.
 *
 * ```kotlin
 * val pk = tester.suspendPrimaryKeyMetadata()
 * // pk != null
 * ```
 */
suspend fun Table.suspendPrimaryKeyMetadata(): PrimaryKeyMetadata? {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingPrimaryKeys(this)[this]
}

/**
 * 테이블의 시퀀스 정보를 비동기적으로 조회합니다.
 *
 * ## 동작/계약
 * - dialect metadata cache 초기화 후 시퀀스 목록을 조회합니다.
 * - 시퀀스가 없으면 빈 리스트를 반환합니다.
 *
 * ```kotlin
 * val sequences = tester.suspendSequences()
 * // sequences.size >= 0
 * ```
 */
suspend fun Table.suspendSequences(): List<Sequence> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingSequences(this)[this].orEmpty()
}
