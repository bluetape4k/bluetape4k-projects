package io.bluetape4k.exposed.jdbc

import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 가상 스레드에서 JDBC 트랜잭션을 실행하고 결과를 동기적으로 반환합니다.
 *
 * 내부적으로 [virtualThreadJdbcTransactionAsync]를 호출하고 결과를 블로킹 대기합니다.
 * 가상 스레드를 사용하므로 플랫폼 스레드 풀을 점유하지 않습니다.
 *
 * @param executor 사용할 [ExecutorService] (기본값: [VirtualThreadExecutor] 싱글톤)
 * @param db 사용할 [Database] (기본값: null — 현재 기본 데이터베이스 사용)
 * @param transactionIsolation 트랜잭션 격리 수준 (기본값: null — 데이터베이스 기본값 사용)
 * @param readOnly 읽기 전용 트랜잭션 여부 (기본값: false)
 * @param statement 트랜잭션 블록 내에서 실행할 람다
 * @return 트랜잭션 블록의 실행 결과 [T]
 *
 * ## 사용 예
 *
 * ```kotlin
 * // 기본 사용 — 가상 스레드에서 단일 트랜잭션 실행
 * val count = newVirtualThreadJdbcTransaction {
 *     UserTable.selectAll().count()
 * }
 *
 * // 여러 트랜잭션을 동시에 실행하여 결과 수집
 * val futures = List(10) { index ->
 *     virtualThreadJdbcTransactionAsync {
 *         UserTable.insert { it[name] = "user-$index" }
 *         index
 *     }
 * }
 * val results: List<Int> = futures.awaitAll()
 * ```
 */
fun <T> newVirtualThreadJdbcTransaction(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean = false,
    statement: JdbcTransaction.() -> T,
): T = virtualThreadJdbcTransactionAsync(
    executor = executor,
    db = db,
    transactionIsolation = transactionIsolation,
    readOnly = readOnly,
    statement = statement
).await()

/**
 * 현재 [JdbcTransaction] 컨텍스트에서 가상 스레드 JDBC 트랜잭션을 실행합니다.
 *
 * 현재 트랜잭션의 `readOnly` 설정을 그대로 이어받아 새 트랜잭션을 생성합니다.
 * 외부 트랜잭션과는 독립적인 커넥션을 사용하므로, 중첩 트랜잭션 격리가 필요한 경우에 유용합니다.
 *
 * @param executor 사용할 [ExecutorService] (기본값: [VirtualThreadExecutor] 싱글톤)
 * @param statement 트랜잭션 블록 내에서 실행할 람다
 * @return 트랜잭션 블록의 실행 결과 [T]
 *
 * ## 사용 예
 *
 * ```kotlin
 * transaction {
 *     val id = UserTable.insertAndGetId { it[name] = "Alice" }
 *     commit()
 *
 *     // 현재 트랜잭션 안에서 별도 가상 스레드 트랜잭션으로 조회
 *     val user = withVirtualThreadJdbcTransaction {
 *         UserTable.selectAll().where { UserTable.id eq id }.single()
 *     }
 * }
 * ```
 */
fun <T> JdbcTransaction.withVirtualThreadJdbcTransaction(
    executor: ExecutorService? = VirtualThreadExecutor,
    statement: JdbcTransaction.() -> T,
): T = virtualThreadJdbcTransactionAsync(
    executor = executor,
    readOnly = this.readOnly,
    statement = statement
).await()

/**
 * 가상 스레드에서 JDBC 트랜잭션을 비동기적으로 실행하고 [VirtualFuture]를 반환합니다.
 *
 * 반환된 [VirtualFuture]에 대해 [VirtualFuture.await]를 호출하면 결과를 블로킹 대기합니다.
 * 여러 [VirtualFuture]를 `awaitAll()`로 한 번에 대기하면 병렬 실행 효과를 얻을 수 있습니다.
 *
 * @param executor 사용할 [ExecutorService] (기본값: [VirtualThreadExecutor] 싱글톤)
 * @param db 사용할 [Database] (기본값: null — 현재 기본 데이터베이스 사용)
 * @param transactionIsolation 트랜잭션 격리 수준 (기본값: null — 데이터베이스 기본값 사용)
 * @param readOnly 읽기 전용 트랜잭션 여부 (기본값: false)
 * @param statement 트랜잭션 블록 내에서 실행할 람다
 * @return 트랜잭션 결과를 담는 [VirtualFuture]<[T]>
 * @throws IllegalArgumentException [executor]가 이미 종료된 경우
 *
 * ## 사용 예
 *
 * ```kotlin
 * // 10개의 INSERT를 가상 스레드에서 병렬 실행
 * val futures: List<VirtualFuture<Int>> = List(10) { index ->
 *     virtualThreadJdbcTransactionAsync {
 *         UserTable.insert { it[name] = "user-$index" }
 *         index
 *     }
 * }
 * val results: List<Int> = futures.awaitAll()
 *
 * // 커스텀 ExecutorService 사용
 * val executor = Executors.newSingleThreadExecutor { Thread(it, "my-vt") }
 * try {
 *     val future = virtualThreadJdbcTransactionAsync(executor = executor) {
 *         UserTable.selectAll().count()
 *     }
 *     val count = future.await()
 * } finally {
 *     executor.shutdown()
 * }
 * ```
 */
fun <T> virtualThreadJdbcTransactionAsync(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean = false,
    statement: JdbcTransaction.() -> T,
): VirtualFuture<T> {
    val effectiveExecutor = executor ?: Executors.newVirtualThreadPerTaskExecutor()
    require(!effectiveExecutor.isShutdown && !effectiveExecutor.isTerminated) {
        "ExecutorService is already shutdown."
    }

    return virtualFuture(executor = effectiveExecutor) {
        val isolationLevel = transactionIsolation ?: db?.transactionManager?.defaultIsolationLevel
        transaction(db = db, transactionIsolation = isolationLevel, readOnly = readOnly) {
            statement(this)
        }
    }
}
