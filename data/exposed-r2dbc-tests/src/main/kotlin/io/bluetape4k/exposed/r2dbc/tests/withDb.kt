package io.bluetape4k.exposed.r2dbc.tests

import io.bluetape4k.utils.Runtimex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Key
import org.jetbrains.exposed.v1.core.statements.StatementInterceptor
import org.jetbrains.exposed.v1.core.transactions.nullableTransactionScope
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.transactions.transactionManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Semaphore

private val registeredOnShutdown = ConcurrentHashMap.newKeySet<TestDB>()
private val testDbSemaphores = ConcurrentHashMap<TestDB, Semaphore>()

internal var currentTestDB by nullableTransactionScope<TestDB>()

private object CurrentTestDBInterceptor: StatementInterceptor {
    override fun keepUserDataInTransactionStoreOnCommit(userData: Map<Key<*>, Any?>): Map<Key<*>, Any?> {
        return userData.filterValues { it is TestDB }
    }
}

private suspend fun acquireSemaphoreSuspending(testDB: TestDB) =
    withContext(Dispatchers.IO) {
        testDbSemaphores.computeIfAbsent(testDB) { Semaphore(1, true) }.acquire()
    }

/**
 * 지정한 [testDB]에 대해 R2DBC 트랜잭션 블록을 실행합니다.
 *
 * 같은 [testDB]를 사용하는 테스트는 공용 세마포어로 직렬화되어,
 * 병렬 실행 시 초기화/연결/테이블 작업 충돌을 방지합니다.
 */
suspend fun withDb(
    testDB: TestDB,
    configure: (DatabaseConfig.Builder.() -> Unit)? = null,
    statement: suspend R2dbcTransaction.(TestDB) -> Unit,
) {
    acquireSemaphoreSuspending(testDB)
    try {
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
                testDB.db = testDB.connect(configure)
            }
            val database = testDB.db!!
            suspendTransaction(
                transactionIsolation = database.transactionManager.defaultIsolationLevel!!,
                db = database,
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
    } finally {
        testDbSemaphores.getValue(testDB).release()
    }
}
