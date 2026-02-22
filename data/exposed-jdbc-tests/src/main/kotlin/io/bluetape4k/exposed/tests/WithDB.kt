package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import org.jetbrains.exposed.v1.core.transactions.nullableTransactionScope
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

internal val registeredOnShutdown = ConcurrentHashMap.newKeySet<TestDB>()
internal val testDbSemaphores = ConcurrentHashMap<TestDB, Semaphore>()

var currentTestDB by nullableTransactionScope<TestDB>()

object CurrentTestDBInterceptor: StatementInterceptor {
    override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
        return userData.filterValues { it is TestDB }
    }
}

fun withDb(
    testDB: TestDB,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: JdbcTransaction.(TestDB) -> Unit,
) {
    logger.info { "Running `withDb` for $testDB" }
    val semaphore = testDbSemaphores.computeIfAbsent(testDB) { Semaphore(1, true) }
    semaphore.acquire()
    try {
        val unregistered = testDB !in registeredOnShutdown
        val newConfiguration = configure != null && !unregistered

        if (unregistered) {
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
    } finally {
        semaphore.release()
    }
}
