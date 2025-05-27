package io.bluetape4k.exposed.r2dbc.tests

import io.bluetape4k.utils.Runtimex
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import org.jetbrains.exposed.v1.core.transactions.nullableTransactionScope
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager
import kotlin.coroutines.CoroutineContext

private val registeredOnShutdown = kotlin.collections.HashSet<TestDB>()

internal var currentTestDB by nullableTransactionScope<TestDB>()

private object CurrentTestDBInterceptor: StatementInterceptor {
    override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
        return userData.filterValues { it is TestDB }
    }
}

suspend fun withDb(
    testDB: TestDB,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},
    context: CoroutineContext? = null,
    statement: suspend R2dbcTransaction.(TestDB) -> Unit,
) {

    val unregistered = testDB !in registeredOnShutdown
    val newConfiguration = configure != null && !unregistered

    if (unregistered) {
        testDB.beforeConnection()
        Runtimex.addShutdownHook {
            testDB.afterTestFinished()
            registeredOnShutdown.remove(testDB)
        }
        registeredOnShutdown += testDB
        testDB.db = testDB.connect(configure ?: {})
    }

    val registeredDb = testDB.db!!
    if (newConfiguration) {
        testDB.db = testDB.connect(configure ?: {})
    }
    val database = testDB.db!!
    suspendTransaction(
        transactionIsolation = database.transactionManager.defaultIsolationLevel,
        context = context,
        db = database
    ) {
        maxAttempts = 1
        registerInterceptor(CurrentTestDBInterceptor)
        currentTestDB = testDB
        statement(testDB)
    }
    // revert any new configuration to not be carried over to the next test in suite
    if (configure != null) {
        testDB.db = registeredDb
    }
}
