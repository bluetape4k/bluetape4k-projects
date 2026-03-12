package io.bluetape4k.examples.vertx.sqlclient

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.sqlclient.tests.testWithSuspendTransaction
import io.bluetape4k.vertx.sqlclient.withSuspendTransaction
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class JDBCPoolExamples: AbstractSqlClientTest() {

    companion object: KLoggingChannel()

    @BeforeAll
    fun setup(vertx: Vertx) {
        // setup 에서는 testContext 가 불필요합니다. 만약 injection을 받으면 꼭 completeNow() 를 호출해야 합니다.
        runBlocking(vertx.dispatcher()) {
            val pool = vertx.getH2Pool()
            pool.withSuspendTransaction { conn: SqlConnection ->
                conn
                    .query(
                        """
                        DROP TABLE test IF EXISTS;
                        CREATE TABLE IF NOT EXISTS test(
                             id int primary key,
                             name varchar(255)
                        )
                        """.trimMargin()
                    )
                    .execute().coAwait()

                conn.query("INSERT INTO test VALUES (1, 'Hello'), (2, 'World')").execute().coAwait()
            }
            pool.close().coAwait()
        }
    }

    @Test
    fun `connect to mysql`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()
        vertx.testWithSuspendTransaction(testContext, pool) {
            val rows = pool.query("SELECT * from test").execute().coAwait()

            val records = rows.map { it.toJson() }
            records shouldHaveSize 2
            records[0] shouldBeEqualTo json { obj("id" to 1, "name" to "Hello") }
            records[1] shouldBeEqualTo json { obj("id" to 2, "name" to "World") }
            records.forEach { println(it) }
        }
        pool.close().coAwait()
    }

    @Test
    fun `connect to mysql in coroutines`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()

        vertx.testWithSuspendTransaction(testContext, pool) {
            val rows = pool.query("SELECT * from test").execute().coAwait()
            val records = rows.map { it.toJson() }

            records.forEach { log.debug { it } }
            records shouldHaveSize 2
            records[0] shouldBeEqualTo json { obj("id" to 1, "name" to "Hello") }
            records[1] shouldBeEqualTo json { obj("id" to 2, "name" to "World") }
        }
        pool.close().coAwait()
    }

    @Test
    fun `query with parameters`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()

        vertx.testWithSuspendTransaction(testContext, pool) {
            val rows = pool.preparedQuery("SELECT * from test where id = ?")
                .execute(Tuple.of(1))
                .coAwait()

            val records = rows.map { it.toJson() }

            records.forEach { log.debug { it } }
            records shouldHaveSize 1
            records.first() shouldBeEqualTo json { obj("id" to 1, "name" to "Hello") }
        }

        pool.close().coAwait()

    }

    @Test
    fun `with transaction`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()

        vertx.testWithSuspendTransaction(testContext, pool) {
            val rows = pool.withSuspendTransaction { conn ->
                conn.query("SELECT COUNT(*) FROM test").execute().coAwait()
            }

            val records = rows.map { it.toJson() }
            records.forEach { log.debug { it } }
            records shouldHaveSize 1
            records.first() shouldBeEqualTo json { obj("COUNT(*)" to 2) }
        }

        pool.close().coAwait()
    }
}
