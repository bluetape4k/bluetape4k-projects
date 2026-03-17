package io.bluetape4k.exposed.jdbc

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
 *
 * ```kotlin
 * withTables(testDB, tester) {
 *     val metadatas = tester.getColumnMetadata()
 *     // metadatas.size == 3
 * }
 * ```
 */
fun Table.getColumnMetadata(): List<ColumnMetadata> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.tableColumns(this)[this].orEmpty()
}

/**
 * 현재 트랜잭션에서 테이블 인덱스 메타데이터를 조회합니다.
 *
 * ## 동작/계약
 * - dialect metadata cache를 초기화한 뒤 `existingIndices` 결과를 반환합니다.
 * - 인덱스가 없으면 빈 리스트를 반환합니다.
 * - 활성 트랜잭션이 없으면 예외가 발생합니다.
 *
 * ```kotlin
 * withTables(testDB, tester) {
 *     val indices = tester.getIndices()
 *     // indices.size == 1
 * }
 * ```
 */
fun Table.getIndices(): List<Index> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingIndices(this)[this].orEmpty()
}

/**
 * 현재 트랜잭션에서 테이블 기본키 메타데이터를 조회합니다.
 *
 * ## 동작/계약
 * - dialect metadata cache 초기화 후 기본키 메타데이터를 조회합니다.
 * - 기본키가 없으면 `null`을 반환합니다.
 * - 트랜잭션 컨텍스트가 없으면 예외가 발생합니다.
 *
 * ```kotlin
 * withTables(testDB, tester) {
 *     val pk = tester.getPrimaryKeyMetadata()
 *     // pk?.columnNames?.contains("ID") == true
 * }
 * ```
 */
fun Table.getPrimaryKeyMetadata(): PrimaryKeyMetadata? {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingPrimaryKeys(this)[this]
}

/**
 * 현재 트랜잭션에서 테이블 관련 시퀀스 메타데이터를 조회합니다.
 *
 * ## 동작/계약
 * - dialect metadata cache를 초기화한 뒤 `existingSequences`를 조회합니다.
 * - 시퀀스가 없으면 빈 리스트를 반환합니다.
 * - PostgreSQL 테스트 기준 identity 테이블에서는 비어 있지 않은 결과를 반환합니다.
 *
 * ```kotlin
 * withDb(testDB) {
 *     val sequences = identityTable.getSequences()
 *     // sequences.isNotEmpty() == true
 * }
 * ```
 */
fun Table.getSequences(): List<Sequence> {
    TransactionManager.current().db.dialectMetadata.resetCaches()
    return currentDialectMetadata.existingSequences(this)[this].orEmpty()
}
