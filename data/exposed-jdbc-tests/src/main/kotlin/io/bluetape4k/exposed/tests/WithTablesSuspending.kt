package io.bluetape4k.exposed.tests

import io.bluetape4k.logging.error
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.inTopLevelTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transactionManager
import kotlin.coroutines.CoroutineContext

/**
 * 코루틴 환경에서 테이블 생성/정리 수명주기를 관리하며 테스트 블록을 실행합니다.
 *
 * ## 동작/계약
 * - 시작 시 기존 테이블 드롭 시도 후 새 테이블을 생성합니다.
 * - 블록 수행 후 `commit()`하고, [dropTables]가 `true`면 테이블 삭제를 시도합니다.
 * - 드롭 실패 시 top-level transaction으로 재시도합니다.
 *
 * ```kotlin
 * withTablesSuspending(TestDB.H2, UtilityTable, dropTables = false) {
 *     UtilityTable.exists()
 *     // result == true
 * }
 * ```
 */
suspend fun withTablesSuspending(
    testDB: TestDB,
    vararg tables: Table,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},
    dropTables: Boolean = true,
    statement: suspend JdbcTransaction.(TestDB) -> Unit,
) {
    withDbSuspending(testDB, context, configure) {
        runCatching {
            SchemaUtils.drop(*tables)
        }

        SchemaUtils.create(*tables)
        try {
            statement(testDB)
            commit()
        } finally {
            if (dropTables) {
                try {
                    SchemaUtils.drop(*tables)
                    commit()
                } catch (ex: Throwable) {
                    logger.error(ex) { "Fail to drop tables, ${tables.joinToString { it.tableName }}" }
                    val database = testDB.db!!
                    inTopLevelTransaction(
                        db = database,
                        transactionIsolation = database.transactionManager.defaultIsolationLevel
                    ) {
                        maxAttempts = 1
                        SchemaUtils.drop(*tables)
                    }
                }
            }
        }
    }
}

@Deprecated(
    message = "Use withTablesSuspending() instead.",
    replaceWith = ReplaceWith(
        "withTablesSuspending(testDB, *tables, context = context, configure = configure, dropTables = dropTables, statement = statement)",
        "io.bluetape4k.exposed.tests.withTablesSuspending"
    )
)
/**
 * [withTablesSuspending]의 deprecated 별칭입니다.
 */
suspend fun withSuspendedTables(
    testDB: TestDB,
    vararg tables: Table,
    context: CoroutineContext? = Dispatchers.IO,
    configure: (DatabaseConfig.Builder.() -> Unit)? = {},
    dropTables: Boolean = true,
    statement: suspend JdbcTransaction.(TestDB) -> Unit,
) {
    withTablesSuspending(
        testDB = testDB,
        tables = tables,
        context = context,
        configure = configure,
        dropTables = dropTables,
        statement = statement,
    )
}
