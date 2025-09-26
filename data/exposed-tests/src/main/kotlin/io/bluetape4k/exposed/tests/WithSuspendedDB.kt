package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.info
import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import kotlin.coroutines.CoroutineContext

@Suppress("DEPRECATION")
suspend fun withSuspendedDb(
    testDB: TestDB,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend JdbcTransaction.(TestDB) -> Unit,
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
    val mutex = Mutex()

    mutex.withLock {
        val registeredDb = testDB.db!!
        try {
            // NOTE: 코루틴과 @ParameterizedTest 를 동시에 사용할 때, TestDB가 꼬일 때가 있다. 그래서 매번 connect 를 수행하도록 수정
            if (newConfiguration) {
                testDB.db = testDB.connect(configure ?: {})
            }
            val database = testDB.db!!
            newSuspendedTransaction(
                context = context,
                db = database,
                transactionIsolation = database.transactionManager.defaultIsolationLevel,
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
}
