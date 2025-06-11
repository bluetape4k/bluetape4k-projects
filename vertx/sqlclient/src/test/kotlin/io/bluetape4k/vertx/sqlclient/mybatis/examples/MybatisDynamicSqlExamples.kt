package io.bluetape4k.vertx.sqlclient.mybatis.examples

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.sqlclient.AbstractVertxSqlClientTest
import io.bluetape4k.vertx.sqlclient.mybatis.renderForVertx
import io.bluetape4k.vertx.sqlclient.schema.PersonMapper
import io.bluetape4k.vertx.sqlclient.schema.PersonSchema.person
import io.bluetape4k.vertx.sqlclient.tests.testWithSuspendTransaction
import io.vertx.core.Vertx
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.templates.SqlTemplate
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.mybatis.dynamic.sql.util.kotlin.model.select

class MybatisDynamicSqlExamples: AbstractVertxSqlClientTest() {

    companion object: KLoggingChannel()

    override fun Vertx.getPool() = this.getMySQLPool()

    override val schemaFileNames: List<String> = listOf("person.sql", "generatedAlways.sql")

    @Test
    fun `MyBatis 로 SQL 구문 생성하고 Vertx SQL Client 로 실행하기`(
        vertx: Vertx, testContext: VertxTestContext,
    ) = runSuspendIO {
        log.debug { "Use MyBatis Dynamic SQL..." }

        vertx.testWithSuspendTransaction(testContext, pool) { conn ->
            // MyBatis Dynamic SQL 로 SQL Statement 생성
            val select = select(person.allColumns()) {
                from(person)
                where { person.id isEqualTo 0 }   // parameter 로 생성될 것이므로 값은 아무 것이나 상관없다
            }.renderForVertx()

            val sql = select.selectStatement
            log.debug { "SQL: $sql" }

            // Vertx SqlClient 로 DB 작업 수행
            val rows = SqlTemplate.forQuery(conn, sql)
                .mapTo(PersonMapper)
                .execute(mapOf("p1" to 1))
                .coAwait()

            testContext.verify {
                rows.size() shouldBeEqualTo 1
                log.debug { rows.first() }
            }
        }
    }
}
