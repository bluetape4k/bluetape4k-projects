package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Key
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementInterceptor
import org.jetbrains.exposed.sql.transactions.nullableTransactionScope
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager

internal val registeredOnShutdown = mutableSetOf<TestDB>()

var currentTestDB by nullableTransactionScope<TestDB>()

object CurrentTestDBInterceptor: StatementInterceptor {
    override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
        return userData.filterValues { it is TestDB }
    }
}

fun withDb(
    testDB: TestDB,
    configure: (DatabaseConfig.Builder.() -> Unit)? = { },
    statement: Transaction.(TestDB) -> Unit,
) {
    logger.info { "Running `withDb` for $testDB" }

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

    val registeredDb = testDB.db
    try {
        if (newConfiguration) {
            testDB.db = testDB.connect(configure)
        }
        val database = testDB.db!!
        transaction(
            transactionIsolation = database.transactionManager.defaultIsolationLevel,
            db = database,
        ) {
            maxAttempts = 1
            registerInterceptor(CurrentTestDBInterceptor)  // interceptor 를 통해 다양한 작업을 할 수 있다
            currentTestDB = testDB
            statement(testDB)
        }
    } finally {
        // revert any new configuration to not be carried over to the next test in suite
        if (configure != null) {
            testDB.db = registeredDb
        }
    }
}
