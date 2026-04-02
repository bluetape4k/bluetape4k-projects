package io.bluetape4k.exposed.trino

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Trino에서 suspend 트랜잭션을 실행합니다.
 *
 * ```kotlin
 * val db = TrinoDatabase.connect("jdbc:trino://host:8080/catalog/schema")
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
 * ⚠️ Trino autocommit 주의사항:
 * - 원자성 보장 없음 — 블록 중간 실패 시 앞선 DML 롤백 안 됨
 * - rollback() no-op
 * - nested transaction 호출 허용되나 원자성 없음
 * - multi-statement 쓰기 시 부분 반영 위험
 *
 * @param db Trino 데이터베이스 연결
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
 * Trino 쿼리 결과를 [Flow]로 반환합니다.
 *
 * 구현상 JDBC `ResultSet` 수명과 Exposed 트랜잭션 경계를 안전하게 유지하기 위해
 * 트랜잭션 내부에서 결과를 `List`로 materialize 한 뒤 순차적으로 emit 합니다.
 * 따라서 소비 API는 [Flow]이지만, 엄밀한 의미의 row-by-row 스트리밍은 아닙니다.
 * 중간 규모 결과를 코루틴 파이프라인으로 연결할 때 적합하며,
 * 매우 큰 결과셋은 페이지네이션 또는 전용 배치 전략을 별도로 고려해야 합니다.
 *
 * ```kotlin
 * val db = TrinoDatabase.connect("jdbc:trino://host:8080/catalog/schema")
 *
 * queryFlow(db) {
 *     Events.selectAll().where { Events.region eq "kr" }
 * }.collect { row ->
 *     println(row[Events.eventId])
 * }
 * ```
 *
 * ⚠️ Trino autocommit 주의사항:
 * - 원자성 보장 없음 — 블록 중간 실패 시 앞선 DML 롤백 안 됨
 * - rollback() no-op
 * - nested transaction 호출 허용되나 원자성 없음
 * - multi-statement 쓰기 시 부분 반영 위험
 *
 * @param db Trino 데이터베이스 연결
 * @param dispatcher 블로킹 JDBC 호출을 실행할 디스패처 (기본값: [Dispatchers.IO])
 * @param block 조회 결과를 반환하는 트랜잭션 블록. 반환된 [Iterable]은 트랜잭션 안에서 즉시 materialize 됩니다.
 */
fun <T> queryFlow(
    db: Database,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: Transaction.() -> Iterable<T>,
): Flow<T> = flow {
    val items = withContext(dispatcher) { transaction(db) { block().toList() } }
    items.forEach { emit(it) }
}
