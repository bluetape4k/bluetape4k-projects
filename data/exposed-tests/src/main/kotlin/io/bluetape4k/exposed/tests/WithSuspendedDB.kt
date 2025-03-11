package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import kotlin.coroutines.CoroutineContext

suspend fun withSuspendedDb(
    testDB: TestDB,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend Transaction.(TestDB) -> Unit,
) {
    logger.info { "Running withSuspendedDb for $testDB" }

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
    try {
        if (newConfiguration) {
            testDB.db = testDB.connect(configure ?: {})
        }
        val database = testDB.db!!
        newSuspendedTransaction(
            context = context,
            db = database,
            transactionIsolation = database.transactionManager.defaultIsolationLevel
        ) {
            maxAttempts = 1
            registerInterceptor(CurrentTestDBInterceptor)
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
