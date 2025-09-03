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
            transactionIsolation ?: db.transactionManager.defaultIsolationLevel
            ?: error("Default transaction isolation not set"),
            readOnly ?: db.transactionManager.defaultReadOnly,
            db
        ) {
            statement()
        }
    }
}
