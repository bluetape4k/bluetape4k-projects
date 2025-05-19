package io.bluetape4k.exposed.sql.transactions

import io.bluetape4k.concurrent.virtualthread.VirtualFuture
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import io.bluetape4k.concurrent.virtualthread.virtualFuture
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import java.util.concurrent.ExecutorService

fun <T> newVirtualThreadTransaction(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean = false,
    statement: Transaction.() -> T,
): T = virtualThreadTransactionAsync(
    executor = executor,
    db = db,
    transactionIsolation = transactionIsolation,
    readOnly = readOnly,
    statement = statement
).await()

fun <T> Transaction.withVirtualThreadTransaction(
    executor: ExecutorService? = VirtualThreadExecutor,
    statement: Transaction.() -> T,
): T = virtualThreadTransactionAsync(
    executor = executor,
    db = this.db,
    transactionIsolation = this.transactionIsolation,
    readOnly = this.readOnly,
    statement = statement
).await()

fun <T> virtualThreadTransactionAsync(
    executor: ExecutorService? = VirtualThreadExecutor,
    db: Database? = null,
    transactionIsolation: Int? = null,
    readOnly: Boolean = false,
    statement: Transaction.() -> T,
): VirtualFuture<T> = virtualFuture(executor = executor ?: VirtualThreadExecutor) {
    val isolationLevel = transactionIsolation ?: db.transactionManager.defaultIsolationLevel
    transaction(db = db, transactionIsolation = isolationLevel, readOnly = readOnly) {
        statement(this)
    }
}
