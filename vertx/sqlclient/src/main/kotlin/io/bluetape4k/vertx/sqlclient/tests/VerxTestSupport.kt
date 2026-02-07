package io.bluetape4k.vertx.sqlclient.tests

import io.bluetape4k.vertx.sqlclient.withSuspendRollback
import io.bluetape4k.vertx.sqlclient.withSuspendTransaction
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection

/**
 * Vertx Sql Client 작업 테스트를 [testContext]하에서 Transactional 하게 수행합니다.
 *
 * ```
 * val pool = JDBCPool.create(vertx)    // MySQLClient.create(vertx)
 *
 * vertx.testWithSuspendTransaction(testContext, pool) { conn ->
 *    val rows = conn.query("select * from Person where id=#{id}")
 *      .execute(mapOf("id" to 1))
 *      .coAwait()
 * }
 * ```
 *
 * @param testContext Vertx Test Context
 * @param pool Sql Client Pool
 * @param block Transactional 작업
 */
@Suppress("UnusedReceiverParameter")
suspend inline fun Vertx.testWithSuspendTransaction(
    testContext: VertxTestContext,
    pool: Pool,
    @BuilderInference block: suspend (conn: SqlConnection) -> Unit,
) {
    try {
        pool.withSuspendTransaction(block)
        testContext.completeNow()
    } catch (e: Throwable) {
        testContext.failNow(e)
    }
}

/**
 * Vertx Sql Client 작업 테스트를 [testContext]하에서
 * 테스트 시에 기존 데이터에 영향을 주지 않도록, Tx 작업이 성공하더라도 Rollback을 하도록 합니다.
 *
 * ```
 * val pool = JDBCPool.create(vertx)    // MySQLClient.create(vertx)
 *
 * vertx.testWithSuspendRollback(testContext, pool) { conn ->
 *    val rows = conn.query("select * from Person where id=#{id}")
 *      .execute(mapOf("id" to 1))
 *      .coAwait()
 * }
 * ```
 *
 * @param testContext Vertx Test Context
 * @param pool Sql Client Pool
 * @param block Transactional 작업
 */
@Suppress("UnusedReceiverParameter")
suspend inline fun Vertx.testWithSuspendRollback(
    testContext: VertxTestContext,
    pool: Pool,
    @BuilderInference block: suspend (conn: SqlConnection) -> Unit,
) {
    try {
        pool.withSuspendRollback(block)
        testContext.completeNow()
    } catch (e: Throwable) {
        testContext.failNow(e)
    }
}
