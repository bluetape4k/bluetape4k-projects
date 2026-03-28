package io.bluetape4k.exposed.duckdb

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * DuckDB에서 suspend 트랜잭션을 실행합니다.
 *
 * ```kotlin
 * val db = DuckDBDatabase.file("/tmp/analytics.db")
 *
 * // suspend 트랜잭션
 * val rows = suspendTransaction(db) {
 *     Events.selectAll().where { Events.region eq "kr" }.toList()
 * }
 *
 * // Virtual Thread 사용
 * val vtDispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
 * val rows = suspendTransaction(db, vtDispatcher) {
 *     Events.selectAll().toList()
 * }
 * ```
 *
 * @param db DuckDB 데이터베이스 연결
 * @param dispatcher 블로킹 JDBC 호출을 실행할 디스패처 (기본값: [Dispatchers.IO])
 * @param block 트랜잭션 블록
 */
suspend fun <T> suspendTransaction(
    db: Database,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: Transaction.() -> T,
): T = withContext(dispatcher) {
    transaction(db) { block() }
}

/**
 * DuckDB 쿼리 결과를 [Flow]로 반환합니다.
 *
 * 대용량 결과셋을 메모리에 모두 올리지 않고 처리할 때 적합합니다.
 *
 * ```kotlin
 * val db = DuckDBDatabase.file("/tmp/analytics.db")
 *
 * queryFlow(db) {
 *     Events.selectAll().where { Events.region eq "kr" }
 * }.collect { row ->
 *     println(row[Events.eventId])
 * }
 * ```
 *
 * @param db DuckDB 데이터베이스 연결
 * @param dispatcher 블로킹 JDBC 호출을 실행할 디스패처 (기본값: [Dispatchers.IO])
 * @param block 쿼리를 반환하는 트랜잭션 블록 ([Iterable] 반환)
 */
fun <T> queryFlow(
    db: Database,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: Transaction.() -> Iterable<T>,
): Flow<T> = flow {
    val items = withContext(dispatcher) { transaction(db) { block().toList() } }
    items.forEach { emit(it) }
}
