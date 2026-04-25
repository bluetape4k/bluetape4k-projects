package io.bluetape4k.exposed.clickhouse

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * ClickHouse에서 suspend 트랜잭션을 실행합니다.
 *
 * ```kotlin
 * val db = ClickHouseDatabase.connect("jdbc:clickhouse://host:8123/default")
 *
 * // suspend 트랜잭션
 * val result = suspendTransaction(db) {
 *     exec("SELECT count() FROM events") { rs -> rs.next(); rs.getLong(1) }
 * }
 *
 * // Virtual Thread 사용
 * val vtDispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
 * val result = suspendTransaction(db, vtDispatcher) {
 *     exec("SELECT 1") { rs -> rs.next(); rs.getInt(1) }
 * }
 * ```
 *
 * ClickHouse 트랜잭션 원자성 없음:
 * - ClickHouse는 autocommit 모드로 동작하며 원자성 보장이 없습니다.
 * - 블록 중간 실패 시 앞선 DML은 롤백되지 않습니다.
 * - rollback() 호출은 no-op입니다.
 * - nested transaction 호출 허용되나 원자성 없음
 * - multi-statement 쓰기 시 부분 반영 위험
 *
 * @param db ClickHouse 데이터베이스 연결
 * @param dispatcher 블로킹 JDBC 호출을 실행할 디스패처 (기본값: [Dispatchers.IO])
 * @param block 트랜잭션 블록
 */
suspend fun <T> suspendTransaction(
    db: Database,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: JdbcTransaction.() -> T,
    // ClickHouse JDBC 호출은 블로킹 I/O이므로, 코루틴 기본 디스패처(Main/Default)를 점유하지 않도록
    // Dispatchers.IO(또는 Virtual Thread 전용 디스패처)로 컨텍스트를 전환합니다.
): T = withContext(dispatcher) {
    try {
        transaction(db) { block() }
    } catch (e: CancellationException) {
        // 코루틴 취소는 반드시 재전파해야 합니다 — 삼키면 구조적 동시성이 깨집니다.
        throw e
    }
}

/**
 * ClickHouse 쿼리 결과를 [Flow]로 반환합니다.
 *
 * 구현상 JDBC `ResultSet` 수명과 Exposed 트랜잭션 경계를 안전하게 유지하기 위해
 * 트랜잭션 내부에서 결과를 `List`로 materialize 한 뒤 순차적으로 emit 합니다.
 * 따라서 소비 API는 [Flow]이지만, 엄밀한 의미의 row-by-row 스트리밍은 아닙니다.
 * 중간 규모 결과를 코루틴 파이프라인으로 연결할 때 적합하며,
 * 매우 큰 결과셋은 페이지네이션 또는 전용 배치 전략을 별도로 고려해야 합니다.
 *
 * ```kotlin
 * val db = ClickHouseDatabase.connect("jdbc:clickhouse://host:8123/default")
 *
 * queryFlow(db) {
 *     Events.selectAll().where { Events.region eq "kr" }
 * }.collect { row ->
 *     println(row[Events.eventId])
 * }
 * ```
 *
 * ClickHouse 트랜잭션 원자성 없음:
 * - ClickHouse는 autocommit 모드로 동작하며 원자성 보장이 없습니다.
 * - 블록 중간 실패 시 앞선 DML은 롤백되지 않습니다.
 * - rollback() 호출은 no-op입니다.
 * - nested transaction 호출 허용되나 원자성 없음
 * - multi-statement 쓰기 시 부분 반영 위험
 *
 * @param db ClickHouse 데이터베이스 연결
 * @param dispatcher 블로킹 JDBC 호출을 실행할 디스패처 (기본값: [Dispatchers.IO])
 * @param block 조회 결과를 반환하는 트랜잭션 블록. 반환된 [Iterable]은 트랜잭션 안에서 즉시 materialize 됩니다.
 */
fun <T> queryFlow(
    db: Database,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    block: JdbcTransaction.() -> Iterable<T>,
): Flow<T> = flow {
    // ClickHouse JDBC 호출은 블로킹 I/O이므로 Dispatchers.IO로 전환하고,
    // ResultSet 수명(트랜잭션 경계 내)과 Flow emit 경계가 겹치지 않도록
    // 트랜잭션 내에서 List로 완전히 materialize한 뒤 방출합니다.
    val items = try {
        withContext(dispatcher) { transaction(db) { block().toList() } }
    } catch (e: CancellationException) {
        // 코루틴 취소는 반드시 재전파해야 합니다 — 삼키면 구조적 동시성이 깨집니다.
        throw e
    }
    items.forEach { emit(it) }
}
