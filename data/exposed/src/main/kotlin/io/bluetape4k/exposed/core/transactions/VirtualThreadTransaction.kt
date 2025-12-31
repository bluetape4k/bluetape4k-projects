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

/**
 * 가상 스레드에서 트랜잭션을 실행하고 결과를 반환합니다.
 *
 * @param executor 사용할 ExecutorService (기본값: VirtualThreadExecutor)
 * @param db 사용할 Database (기본값: null)
 * @param transactionIsolation 트랜잭션 격리 수준 (기본값: null)
 * @param readOnly 읽기 전용 여부 (기본값: false)
 * @param statement 실행할 트랜잭션 블록
 * @return 트랜잭션 블록의 실행 결과
 */
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
 * 현재 트랜잭션 컨텍스트 내에서 가상 스레드 트랜잭션을 실행합니다.
 *
 * @param executor 사용할 ExecutorService (기본값: VirtualThreadExecutor)
 * @param statement 실행할 트랜잭션 블록
 * @return 트랜잭션 블록의 실행 결과
 */
fun <T> Transaction.withVirtualThreadTransaction(
    executor: ExecutorService? = VirtualThreadExecutor,
    statement: JdbcTransaction.() -> T,
): T = virtualThreadTransactionAsync(
    executor = executor,
    readOnly = this.readOnly,
    statement = statement
).await()

/**
 * 가상 스레드에서 비동기적으로 트랜잭션을 실행합니다.
 *
 * @param executor 사용할 ExecutorService (기본값: VirtualThreadExecutor)
 * @param db 사용할 Database (기본값: null)
 * @param transactionIsolation 트랜잭션 격리 수준 (기본값: null)
 * @param readOnly 읽기 전용 여부 (기본값: false)
 * @param statement 실행할 트랜잭션 블록
 * @return VirtualFuture\<T\> 트랜잭션 블록의 실행 결과를 담는 Future
 */
fun <T> virtualThreadTransactionAsync(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean = false,
    statement: JdbcTransaction.() -> T,
): VirtualFuture<T> = virtualFuture(executor = executor ?: VirtualThreadExecutor) {
    val isolationLevel = transactionIsolation ?: db?.transactionManager?.defaultIsolationLevel
    transaction(db = db, transactionIsolation = isolationLevel, readOnly = readOnly) {
        statement(this)
    }
}
