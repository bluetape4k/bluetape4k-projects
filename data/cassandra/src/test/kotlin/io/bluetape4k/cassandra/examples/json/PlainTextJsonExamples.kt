package io.bluetape4k.cassandra.examples.json

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import com.datastax.oss.driver.api.querybuilder.select.Selector
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.querybuilder.bindMarker
import io.bluetape4k.cassandra.querybuilder.functionTerm
import io.bluetape4k.cassandra.querybuilder.literal
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class PlainTextJsonExamples: AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        private val CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS querybuilder_json(
                id int PRIMARY KEY,
                name text,
                specs map<text, text>
            )                        
        """.trimIndent()
    }

    @Test
    fun `JSON 형태를 Core API 로 처리하기`() {
        createSchema(session)
        insertWithCoreApi(session)
        selectWithCoreApi(session)
    }

    private fun createSchema(session: CqlSession) {
        session.execute(CREATE_TABLE).wasApplied().shouldBeTrue()
    }

    private fun insertWithCoreApi(session: CqlSession) {
        val insertQuery = "INSERT INTO querybuilder_json JSON ?"
        val stmt = SimpleStatement.newInstance(
            insertQuery,
            """{ "id": 1, "name": "Mouse", "specs": { "color": "silver" } }"""
        )
        session.execute(stmt).wasApplied().shouldBeTrue()

        val ps = session.prepare("INSERT INTO querybuilder_json JSON :payload")
        val bs = ps.bind().setString(
            "payload",
            """{ "id": 2, "name": "Keyboard", "specs": { "layout": "qwerty" } }"""
        )
        session.execute(bs).wasApplied().shouldBeTrue()

        val query = """
            INSERT INTO querybuilder_json (id, name, specs)
            VALUES(?, ?, fromJson(?))
            """.trimIndent()

        val stmt2 = SimpleStatement.newInstance(query, 3, "Screen", """{ "size": "24-inch" }""")
        session.execute(stmt2).wasApplied().shouldBeTrue()
    }

    private fun selectWithCoreApi(session: CqlSession) {
        val query1 = "SELECT JSON * FROM querybuilder_json WHERE id=?"
        val stmt1 = SimpleStatement.newInstance(query1, 1)
        val row1 = session.execute(stmt1).one()
        row1.shouldNotBeNull()
        row1.getString("[json]")!!.shouldNotBeEmpty()
        log.debug { "Entry #1 as JSON: ${row1.getString("[json]")}" }

        val query2 = "SELECT id, toJson(specs) as json_specs FROM querybuilder_json WHERE id=?"
        val stmt2 = SimpleStatement.newInstance(query2, 2)
        val row2 = session.execute(stmt2).one()
        row2.shouldNotBeNull()
        row2.getInt("id") shouldBeEqualTo 2
        row2.getString("json_specs") shouldBeEqualTo """{"layout": "qwerty"}"""
        log.debug { "Entry #2's specs as JSON: ${row2.getString("json_specs")}" }
    }

    @Test
    fun `JSON 형태를 QueryBuilder 로 처리하기`() {
        createSchema(session)

        insertWithQueryBuilder(session)
        selectWithQueryBuilder(session)
    }

    private fun insertWithQueryBuilder(session: CqlSession) {
        val stmt = insertInto("querybuilder_json")
            .json("""{ "id": 1, "name": "Mouse", "specs": { "color": "silver" } }""")
            .build()
        session.execute(stmt).wasApplied().shouldBeTrue()

        val stmt2 = insertInto("querybuilder_json").json("payload".bindMarker()).build()
        val ps = session.prepare(stmt2)
        val bs = ps.bind().setString(
            "payload",
            """{ "id": 2, "name": "Keyboard", "specs": { "layout": "qwerty" } }"""
        )
        session.execute(bs).wasApplied().shouldBeTrue()

        val stmt3 = insertInto("querybuilder_json")
            .value("id", 3.literal())
            .value("name", "Screen".literal())
            .value("specs", functionTerm("fromJson", """{"layout": "qwerty"}""".literal()))
            .build()

        session.execute(stmt3).wasApplied().shouldBeTrue()
    }

    private fun selectWithQueryBuilder(session: CqlSession) {
        // Row 전체를 하나의 JSON 으로 읽기
        val stmt1 = selectFrom("querybuilder_json")
            .json()
            .all()
            .whereColumn("id")
            .isEqualTo(1.literal())
            .build()
        val row1 = session.execute(stmt1).one()!!
        println("Entry #1 as JSON: ${row1.getString("[json]")}")

        // 특정 컬럼만 JSON 으로 읽기
        val stmt2 = selectFrom("querybuilder_json")
            .column("id")
            .function("toJson", Selector.column("specs")).`as`("json_specs")
            .whereColumn("id").isEqualTo(2.literal())
            .build()
        val row2 = session.execute(stmt2).one()!!

        row2.getInt("id") shouldBeEqualTo 2
        row2.getString("json_specs") shouldBeEqualTo """{"layout": "qwerty"}"""
        log.debug { "Entry #2's specs as JSON: ${row2.getString("json_specs")}" }
    }
}
