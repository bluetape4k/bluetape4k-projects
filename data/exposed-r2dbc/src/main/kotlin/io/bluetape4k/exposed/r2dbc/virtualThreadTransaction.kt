package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.r2dbc.spi.IsolationLevel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager
import java.util.concurrent.ExecutorService

/**
 * 가상 스레드 기반의 트랜잭션을 실행합니다.
 *
 * @param executor 사용할 ExecutorService (기본값: VirtualThreadExecutor)
 * @param db 사용할 R2dbcDatabase (기본값: null)
 * @param transactionIsolation 트랜잭션 격리 수준 (기본값: null)
 * @param readOnly 읽기 전용 여부 (기본값: false)
 * @param statement 실행할 트랜잭션 블록
 * @return 트랜잭션 블록의 결과
 */
suspend fun <T> virtualThreadTransaction(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: R2dbcDatabase? = null,
    transactionIsolation: IsolationLevel? = null,
    readOnly: Boolean = false,
    statement: suspend R2dbcTransaction.() -> T,
): T = virtualThreadTransactionAsync(
    executor = executor,
    db = db,
    transactionIsolation = transactionIsolation,
    readOnly = readOnly,
    statement = statement
).await()

/**
 * 현재 트랜잭션 내에서 가상 스레드 기반의 트랜잭션을 중첩 실행합니다.
 *
 * @receiver R2dbcTransaction
 * @param executor 사용할 ExecutorService (기본값: VirtualThreadExecutor)
 * @param transactionIsolation 트랜잭션 격리 수준 (기본값: 현재 트랜잭션의 격리 수준)
 * @param statement 실행할 트랜잭션 블록
 * @return 트랜잭션 블록의 결과
 */
suspend fun <T> R2dbcTransaction.withVirtualThreadTransaction(
    executor: ExecutorService? = VirtualThreadExecutor,
    transactionIsolation: IsolationLevel? = this.transactionIsolation,
    statement: suspend R2dbcTransaction.() -> T,
): T = virtualThreadTransactionAsync(
    executor = executor,
    db = this.db,
    transactionIsolation = transactionIsolation,
    readOnly = readOnly,
    statement = statement
).await()

/**
 * 가상 스레드 기반의 트랜잭션을 비동기로 실행합니다.
 *
 * @param executor 사용할 ExecutorService (기본값: VirtualThreadExecutor)
 * @param db 사용할 R2dbcDatabase (기본값: null)
 * @param transactionIsolation 트랜잭션 격리 수준
 * @param readOnly 읽기 전용 여부 (기본값: false)
 * @param statement 실행할 트랜잭션 블록
 * @return Deferred\<T\> 트랜잭션 블록의 결과를 담는 Deferred
 */
suspend fun <T> virtualThreadTransactionAsync(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: R2dbcDatabase? = null,
    transactionIsolation: IsolationLevel?,
    readOnly: Boolean? = false,
    statement: suspend R2dbcTransaction.() -> T,
): Deferred<T> = coroutineScope {
    val dispatcher = (executor ?: VirtualThreadExecutor).asCoroutineDispatcher()

    async(dispatcher) {
        suspendTransaction(
            db = db,
            transactionIsolation = transactionIsolation ?: db?.transactionManager?.defaultIsolationLevel,
            readOnly = readOnly ?: db?.transactionManager?.defaultReadOnly,
        ) {
            statement()
        }
    }
}
