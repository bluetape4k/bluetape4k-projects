package io.bluetape4k.examples.vertx.sqlclient.templates

import io.bluetape4k.examples.vertx.sqlclient.AbstractSqlClientTest
import io.bluetape4k.examples.vertx.sqlclient.model.USER_ROW_MAPPER
import io.bluetape4k.examples.vertx.sqlclient.model.User
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.vertx.sqlclient.templates.tupleMapperOfRecord
import io.bluetape4k.vertx.sqlclient.tests.testWithSuspendTransaction
import io.bluetape4k.vertx.sqlclient.withSuspendTransaction
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxTestContext
import io.vertx.kotlin.core.json.json
import io.vertx.kotlin.core.json.obj
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.coroutines.dispatcher
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlResult
import io.vertx.sqlclient.templates.SqlTemplate
import io.vertx.sqlclient.templates.TupleMapper
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class SqlClientTemplateExamples: AbstractSqlClientTest() {

    companion object: KLoggingChannel()

    @BeforeAll
    fun setup(vertx: Vertx) {
        // setup 에서는 testContext 가 불필요합니다. 만약 injection을 받으면 꼭 completeNow() 를 호출해야 합니다.
        runBlocking(vertx.dispatcher()) {
            val pool = vertx.getH2Pool()
            try {
                pool.withSuspendTransaction { conn ->
                    conn.query("DROP TABLE users IF EXISTS").execute().coAwait()
                    conn.query(
                        """
                        CREATE TABLE IF NOT EXISTS users(
                             id LONG PRIMARY KEY,
                             first_name VARCHAR(255),
                             last_name VARCHAR(255)
                        )
                        """.trimIndent()
                    ).execute().coAwait()
                    conn.query("INSERT INTO users VALUES (1, 'John', 'Doe'), (2, 'Jane', 'Doe')").execute().coAwait()
                }
            } finally {
                pool.close().coAwait()
            }
        }
    }

    @Test
    fun `query example`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()
        vertx.testWithSuspendTransaction(testContext, pool) {
            val parameters = mapOf("ID" to 1)
            val rowSet: RowSet<Row> = SqlTemplate
                .forQuery(pool, "SELECT * FROM users WHERE id = #{ID}")
                .execute(parameters)
                .coAwait()

            rowSet.forEach { row ->
                log.debug { row.toJson() }
                val user = User(
                    id = row.getLong("id"),
                    firstName = row.getString("first_name"),
                    lastName = row.getString("last_name")
                )
                log.debug { user }
            }
        }
        pool.close().coAwait()
    }

    @Test
    fun `insert example`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()
        vertx.testWithSuspendTransaction(testContext, pool) {
            val parameters = mapOf(
                "id" to 3,
                "firstName" to "Dale",
                "lastName" to "Cooper"
            )
            val result: SqlResult<Void> = SqlTemplate
                .forUpdate(pool, "INSERT INTO users VALUES(#{id}, #{firstName}, #{lastName})")
                .execute(parameters)
                .coAwait()

            result.rowCount() shouldBeEqualTo 1
        }
        pool.close().coAwait()
    }

    @Test
    fun `query using row mapper`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()
        vertx.testWithSuspendTransaction(testContext, pool) {
            val parameters = mapOf("id" to 1)
            val users: RowSet<User> = SqlTemplate
                .forQuery(pool, "SELECT * FROM users WHERE id = #{id}")
                .mapTo(USER_ROW_MAPPER)                                             // mapping custom mapper
                .execute(parameters)
                .coAwait()

            users.size() shouldBeEqualTo 1
            users.forEach { user -> log.debug { user } }
        }
        pool.close().coAwait()
    }

    @Test
    fun `binding row with anemic JsonMapperr`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()

        vertx.testWithSuspendTransaction(testContext, pool) {
            val parameters = mapOf("id" to 1)
            val users: RowSet<JsonObject> = SqlTemplate
                .forQuery(pool, "SELECT * FROM users WHERE id = #{id}")
                .mapTo(Row::toJson)                                                 // mapping Row -> Json Object
                .execute(parameters)
                .coAwait()

            users.size() shouldBeEqualTo 1
            users.forEach { user ->
                log.debug { user.encode() }
            }
        }
        pool.close().coAwait()
    }

    @Test
    fun `binding parameter with custom mapper`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()

        vertx.testWithSuspendTransaction(testContext, pool) {
            val user = User(
                id = 4,
                firstName = "Iron",
                lastName = "Man"
            )

            val result = SqlTemplate
                .forQuery(pool, "INSERT INTO users VALUES (#{id}, #{firstName}, #{lastName})")
                // .mapFrom(USER_TUPLE_MAPPER)
                .mapFrom(tupleMapperOfRecord<User>())
                .execute(user)
                .coAwait()

            result.rowCount() shouldBeEqualTo 1

        }
        pool.close().coAwait()
    }

    @Test
    fun `binding parameter with anemic json mapper`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()

        vertx.testWithSuspendTransaction(testContext, pool) {
            val user = json {
                obj {
                    put(User::id.name, 5)
                    put(User::firstName.name, "Moon")
                    put(User::lastName.name, "Knight")
                }
            }

            val result = SqlTemplate
                .forQuery(pool, "INSERT INTO users VALUES (#{id}, #{firstName}, #{lastName})")
                .mapFrom(TupleMapper.jsonObject())
                .execute(user)
                .coAwait()

            result.rowCount() shouldBeEqualTo 1
        }
        pool.close().coAwait()
    }

    @Disabled("Vertx Jackson Databind 기능에서 kotlin module 을 등록하지 않아 예외가 발생한다.")
    // https://github.com/eclipse-vertx/vertx-sql-client/issues/1196
    @Test
    fun `binding row with jackson databind`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()
        vertx.testWithSuspendTransaction(testContext, pool) {
            val users: RowSet<User> = SqlTemplate
                .forQuery(pool, "SELECT * FROM users WHERE id = #{id}")
                .mapTo(User::class.java)
                .execute(mapOf("id" to 1))
                .coAwait()

            users.size() shouldBeEqualTo 1
            users.forEach { user -> log.debug { user } }
        }
        pool.close().coAwait()
    }

    @Test
    fun `binding parameter with jackson databind`(vertx: Vertx, testContext: VertxTestContext) = runSuspendIO {
        val pool = vertx.getH2Pool()
        vertx.testWithSuspendTransaction(testContext, pool) {
            val user = User(6, faker.name().firstName(), faker.name().lastName())

            val result: SqlResult<Void> = SqlTemplate
                .forUpdate(pool, "INSERT INTO users VALUES(#{id}, #{firstName}, #{lastName})")
                .mapFrom(User::class.java)
                .execute(user)
                .coAwait()

            result.rowCount() shouldBeEqualTo 1
        }
        pool.close().coAwait()
    }
}
