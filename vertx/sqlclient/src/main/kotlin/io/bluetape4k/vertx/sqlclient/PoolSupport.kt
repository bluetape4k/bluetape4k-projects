package io.bluetape4k.vertx.sqlclient

import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.TransactionRollbackException
import java.sql.SQLException

/**
 * Transaction 환경 하에서 Database 작업을 수행합니다.
 *
 * ```
 * val pool = JDBCPool.create(vertx)    // MySQLClient.create(vertx)
 * val rows = pool.withSuspendTransaction {
 *     SqlTemplate.forQuery("select * from Person where id=#{id}")
 *      .execute(mapOf("id" to 1))
 *      .await()
 * }
 * ```
 *
 * @param action Transaction 하에서 수행할 작업
 * @receiver [Pool] 인스턴스
 * @return DB 작업 결과
 */
suspend fun <T> Pool.withSuspendTransaction(
    @BuilderInference action: suspend (conn: SqlConnection) -> T,
): T {
    val conn = connection.coAwait()
    val tx = conn.begin().coAwait()

    return try {
        val result = action(conn)
        tx.commit().coAwait()
        result
    } catch (e: TransactionRollbackException) {
        throw (e)
    } catch (e: Throwable) {
        runCatching { tx.rollback().coAwait() }
        throw SQLException(e)
    } finally {
        conn.close().coAwait()
    }
}

/**
 * 테스트 시에 기존 데이터에 영향을 주지 않도록, Tx 작업이 성공하더라도 Rollback을 하도록 합니다.
 *
 * ```
 * val pool = JDBCPool.create(vertx)    // MySQLClient.create(vertx)
 * val rows = pool.withSuspendRollback {
 *     SqlTemplate.forQuery("select * from Person where id=#{id}")
 *      .execute(mapOf("id" to 1))
 *      .await()
 * }
 * ```
 *
 * @param T
 * @param action Transaction 하에서 수행할 작업
 * @return 작업 결과
 */
suspend fun <T> Pool.withSuspendRollback(
    @BuilderInference action: suspend (conn: SqlConnection) -> T,
): T {
    val conn = connection.coAwait()
    val tx = conn.begin().coAwait()
    return try {
        val result = action(conn)
        tx.rollback().coAwait()
        result
    } catch (e: TransactionRollbackException) {
        throw (e)
    } catch (e: Throwable) {
        runCatching { tx.rollback().coAwait() }
        throw SQLException(e)
    } finally {
        conn.close().coAwait()
    }
}
