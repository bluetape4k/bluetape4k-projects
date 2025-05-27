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

fun <T> Transaction.withVirtualThreadTransaction(
    executor: ExecutorService? = VirtualThreadExecutor,
    statement: JdbcTransaction.() -> T,
): T = virtualThreadTransactionAsync(
    executor = executor,
    readOnly = this.readOnly,
    statement = statement
).await()

fun <T> virtualThreadTransactionAsync(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean = false,
    statement: JdbcTransaction.() -> T,
): VirtualFuture<T> = virtualFuture(executor = executor ?: VirtualThreadExecutor) {
    val isolationLevel = transactionIsolation ?: db.transactionManager.defaultIsolationLevel
    transaction(db = db, transactionIsolation = isolationLevel, readOnly = readOnly) {
        statement(this)
    }
}
