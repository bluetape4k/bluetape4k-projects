package io.bluetape4k.vertx.sqlclient.tests

import io.bluetape4k.vertx.sqlclient.withRollbackSuspending
import io.bluetape4k.vertx.sqlclient.withTransactionSuspending
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
 * vertx.testWithTransactionSuspending(testContext, pool) { conn ->
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
suspend fun Vertx.testWithTransactionSuspending(
    testContext: VertxTestContext,
    pool: Pool,
    block: suspend (conn: SqlConnection) -> Unit,
) {
    try {
        pool.withTransactionSuspending(block)
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
 * vertx.testWithRollbackSuspending(testContext, pool) { conn ->
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
suspend fun Vertx.testWithRollbackSuspending(
    testContext: VertxTestContext,
    pool: Pool,
    block: suspend (conn: SqlConnection) -> Unit,
) {
    try {
        pool.withRollbackSuspending(block)
        testContext.completeNow()
    } catch (e: Throwable) {
        testContext.failNow(e)
    }
}
