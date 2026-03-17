package io.bluetape4k.exposed.r2dbc

import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.r2dbc.spi.IsolationLevel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExecutorCoroutineDispatcher
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
 * ## 동작/계약
 * - 내부적으로 [virtualThreadTransactionAsync]를 호출하고 `await()`로 결과를 기다립니다.
 * - `executor`가 `null` 또는 [VirtualThreadExecutor]면 공유 dispatcher를 재사용합니다.
 * - 호출자는 suspend 문맥에서 결과를 받으며, 블로킹 없이 가상 스레드에서 트랜잭션을 실행합니다.
 *
 * ```kotlin
 * val count = virtualThreadTransaction {
 *     UserTable.selectAll().count()
 * }
 * // count >= 0L
 * ```
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
 * ## 동작/계약
 * - 현재 트랜잭션의 `db`, `readOnly`를 이어받아 새 가상 스레드 트랜잭션을 실행합니다.
 * - [transactionIsolation] 기본값은 현재 트랜잭션의 격리 수준입니다.
 *
 * ```kotlin
 * suspendTransaction {
 *     val value = withVirtualThreadTransaction { UserTable.selectAll().count() }
 *     // value >= 0L
 * }
 * ```
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
 * ## 동작/계약
 * - [executor]에 맞는 dispatcher를 생성해 `async`로 트랜잭션을 실행합니다.
 * - 사용자 제공 executor를 dispatcher로 만든 경우 실행 완료 후 `close()`로 정리합니다.
 * - 반환값은 [Deferred]이며 호출자가 `await()`/`awaitAll()`로 완료를 제어합니다.
 *
 * ```kotlin
 * val jobs = List(5) { index ->
 *     virtualThreadTransactionAsync { index }
 * }
 * val values = jobs.map { it.await() }
 * // values.size == 5
 * ```
 */
suspend fun <T> virtualThreadTransactionAsync(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: R2dbcDatabase? = null,
    transactionIsolation: IsolationLevel? = null,
    readOnly: Boolean = false,
    statement: suspend R2dbcTransaction.() -> T,
): Deferred<T> = coroutineScope {
    val (dispatcher, shouldClose) = createDispatcher(executor)

    async(dispatcher) {
        try {
            suspendTransaction(
                db = db,
                transactionIsolation = transactionIsolation ?: db?.transactionManager?.defaultIsolationLevel,
                readOnly = readOnly,
            ) {
                statement()
            }
        } finally {
            if (shouldClose) {
                dispatcher.close()
            }
        }
    }
}

private val virtualThreadDispatcher: ExecutorCoroutineDispatcher by lazy {
    VirtualThreadExecutor.asCoroutineDispatcher()
}

private fun createDispatcher(executor: ExecutorService?): Pair<ExecutorCoroutineDispatcher, Boolean> {
    if (executor == null || executor === VirtualThreadExecutor) {
        return virtualThreadDispatcher to false
    }
    return executor.asCoroutineDispatcher() to true
}
