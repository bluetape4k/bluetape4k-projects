package io.bluetape4k.vertx.sqlclient

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Transaction
import io.vertx.sqlclient.TransactionRollbackException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.sql.SQLException

/**
 * Transaction 환경 하에서 Database 작업을 수행합니다.
 *
 * ```kotlin
 * val pool = JDBCPool.create(vertx)    // MySQLClient.create(vertx)
 * val rows = pool.withSuspendTransaction { conn ->
 *     SqlTemplate.forQuery(conn, "select * from Person where id=#{id}")
 *         .execute(mapOf("id" to 1))
 *         .coAwait()
 * }
 * // rows.size() >= 0
 * ```
 *
 * @param action Transaction 하에서 수행할 작업
 * @receiver [Pool] 인스턴스
 * @return DB 작업 결과
 *
 * [kotlinx.coroutines.CancellationException]은 래핑하지 않고 그대로 전파합니다.
 */
suspend inline fun <T> Pool.withSuspendTransaction(
    action: suspend (conn: SqlConnection) -> T,
): T {
    val conn = connection.coAwait()
    val tx = conn.begin().coAwait()
    var primaryFailure: Throwable? = null

    return try {
        val result = action(conn)
        tx.commit().coAwait()
        result
    } catch (e: TransactionRollbackException) {
        primaryFailure = e
        throw (e)
    } catch (e: CancellationException) {
        primaryFailure = e
        rollbackSuppressing(tx, e)
        throw e
    } catch (e: Throwable) {
        val sqlException = SQLException(e)
        primaryFailure = sqlException
        rollbackSuppressing(tx, sqlException)
        throw sqlException
    } finally {
        closeConnection(conn, primaryFailure)
    }
}

/**
 * 테스트 시에 기존 데이터에 영향을 주지 않도록, Tx 작업이 성공하더라도 Rollback을 하도록 합니다.
 *
 * ```kotlin
 * val pool = JDBCPool.create(vertx)    // MySQLClient.create(vertx)
 * val rows = pool.withSuspendRollback { conn ->
 *     SqlTemplate.forQuery(conn, "select * from Person where id=#{id}")
 *         .execute(mapOf("id" to 1))
 *         .coAwait()
 * }
 * // rows.size() >= 0  (작업 후 자동 롤백됨)
 * ```
 *
 * @param action Transaction 하에서 수행할 작업
 * @return 작업 결과
 *
 * [kotlinx.coroutines.CancellationException]은 래핑하지 않고 그대로 전파합니다.
 */
suspend inline fun <T> Pool.withSuspendRollback(
    action: suspend (conn: SqlConnection) -> T,
): T {
    val conn = connection.coAwait()
    val tx = conn.begin().coAwait()
    var primaryFailure: Throwable? = null
    return try {
        val result = action(conn)
        tx.rollback().coAwait()
        result
    } catch (e: TransactionRollbackException) {
        primaryFailure = e
        throw (e)
    } catch (e: CancellationException) {
        primaryFailure = e
        rollbackSuppressing(tx, e)
        throw e
    } catch (e: Throwable) {
        val sqlException = SQLException(e)
        primaryFailure = sqlException
        rollbackSuppressing(tx, sqlException)
        throw sqlException
    } finally {
        closeConnection(conn, primaryFailure)
    }
}

@PublishedApi
internal suspend fun rollbackSuppressing(
    tx: Transaction,
    primaryFailure: Throwable,
) {
    try {
        withContext(NonCancellable) {
            tx.rollback().coAwait()
        }
    } catch (rollbackFailure: Throwable) {
        primaryFailure.addSuppressed(rollbackFailure)
    }
}

@PublishedApi
internal suspend fun closeConnection(
    conn: SqlConnection,
    primaryFailure: Throwable?,
) {
    try {
        withContext(NonCancellable) {
            conn.close().coAwait()
        }
    } catch (closeFailure: Throwable) {
        if (primaryFailure == null) {
            throw closeFailure
        }
        primaryFailure.addSuppressed(closeFailure)
    }
}
