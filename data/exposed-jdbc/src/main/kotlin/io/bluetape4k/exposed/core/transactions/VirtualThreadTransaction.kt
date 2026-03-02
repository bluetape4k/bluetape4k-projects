package io.bluetape4k.exposed.core.transactions

import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * 가상 스레드에서 JDBC 트랜잭션을 실행하고 결과를 동기 대기해 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 [virtualThreadTransactionAsync]를 호출하고 `await()`로 결과를 기다립니다.
 * - API는 deprecated 상태이며 `io.bluetape4k.exposed.jdbc.transactions` 패키지 함수로 대체되었습니다.
 */
@Deprecated(
    message = "use io.bluetape4k.exposed.jdbc.transactions.newVirtualThreadTransaction instead.",
    replaceWith = ReplaceWith("newVirtualThreadJdbcTransaction(executor, db, transactionIsolation, readOnly, statement)")
)
fun <T> newVirtualThreadTransaction(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean = false,
    statement: JdbcTransaction.() -> T,
): T = virtualThreadTransactionAsync(
    executor = executor,
    db = db,
    transactionIsolation = transactionIsolation,
    readOnly = readOnly,
    statement = statement
).await()

/**
 * 현재 트랜잭션 설정을 이어받아 가상 스레드 트랜잭션을 실행합니다.
 *
 * ## 동작/계약
 * - 현재 트랜잭션의 `readOnly` 값을 새 트랜잭션에 전달합니다.
 * - API는 deprecated 상태이며 `withVirtualJdbcThreadTransaction`으로 대체되었습니다.
 */
@Deprecated(
    message = "use io.bluetape4k.exposed.jdbc.transactions.withVirtualThreadTransaction instead",
    replaceWith = ReplaceWith("withVirtualJdbcThreadTransaction(executor, statement)")
)
fun <T> Transaction.withVirtualThreadTransaction(
    executor: ExecutorService? = Executors.newVirtualThreadPerTaskExecutor(),
    statement: JdbcTransaction.() -> T,
): T = virtualThreadTransactionAsync(
    executor = executor,
    readOnly = this.readOnly,
    statement = statement
).await()

/**
 * 가상 스레드에서 JDBC 트랜잭션을 비동기로 실행합니다.
 *
 * ## 동작/계약
 * - `executor`가 종료 상태면 [IllegalArgumentException]을 던집니다.
 * - 격리수준 미지정 시 `db?.transactionManager?.defaultIsolationLevel`을 사용합니다.
 * - API는 deprecated 상태이며 `virtualThreadJdbcTransactionAsync`로 대체되었습니다.
 */
@Deprecated(
    message = "use io.bluetape4k.exposed.jdbc.transactions.virtualThreadTransactionAsync instead.",
    replaceWith = ReplaceWith("virtualThreadJdbcTransactionAsync(executor, db, transactionIsolation, readOnly, statement)")
)
fun <T> virtualThreadTransactionAsync(
    executor: ExecutorService? = Executors.newVirtualThreadPerTaskExecutor(),
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
