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

/**
 * 테스트용 DB에 트랜잭션을 열고 블록을 실행합니다.
 *
 * ## 동작/계약
 * - DB별 세마포어를 사용해 동일 [TestDB]에 대한 동시 실행을 직렬화합니다.
 * - 첫 호출 시 DB 연결을 생성해 [TestDB.db]에 캐시하고, shutdown hook으로 정리 작업을 등록합니다.
 * - [configure]를 전달하면 현재 호출에만 임시 적용한 뒤 기존 DB 레퍼런스로 되돌립니다.
 * - 블록 실행은 `maxAttempts = 1` 트랜잭션에서 수행되며 `currentTestDB`가 설정됩니다.
 *
 * ```kotlin
 * withDb(TestDB.H2) {
 *     UtilityTable.exists()
 *     // result == false 또는 true (현재 스키마 상태)
 * }
 * ```
 */
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
