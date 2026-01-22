package io.bluetape4k.cassandra.examples.json

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.datastax.oss.driver.api.core.type.codec.ExtraTypeCodecs
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.function
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.insertInto
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.selectFrom
import com.datastax.oss.driver.api.querybuilder.select.Selector
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.CqlSessionProvider
import io.bluetape4k.cassandra.cql.getStringOrEmpty
import io.bluetape4k.cassandra.data.getValue
import io.bluetape4k.cassandra.data.setValue
import io.bluetape4k.cassandra.querybuilder.bindMarker
import io.bluetape4k.cassandra.querybuilder.inValues
import io.bluetape4k.cassandra.querybuilder.literal
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.jackson.readValueOrNull
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.Serializable

class JacksonJsonFunctionExamples: AbstractCassandraTest() {

    companion object: KLoggingChannel() {
        data class User(
            val name: String? = null,
            val age: Int? = null,
        ): Serializable

        private val USER_CODEC = ExtraTypeCodecs.json(User::class.java)
        private val JSON_NODE_CODEC = ExtraTypeCodecs.json(JsonNode::class.java)
    }

    private val mapper = Jackson.defaultJsonMapper

    @Test
    fun `convert object to json`() {
        val user = User("debop", 53)
        val json = mapper.writeAsString(user)
        log.debug { "user=$json" }
        json.shouldNotBeNull().shouldNotBeEmpty()

        val actual = mapper.readValueOrNull<User>(json)
        actual shouldBeEqualTo user
    }

    @Test
    fun `Jackson Codec 과 함수를 이용하여 처리하기`() {
        //        newCqlSessionBuilder()
        //            .withKeyspace(DEFAULT_KEYSPACE)
        //            .addTypeCodecs(USER_CODEC, JSON_NODE_CODEC)
        //            .build()
        //            .use { session ->
        //                createSchema(session)
        //                insertFromJson(session)
        //                selectToJson(session)
        //            }

        val session = CqlSessionProvider.getOrCreateSession(
            "jackson_examples",
            { newCqlSessionBuilder() }
        ) {
            addTypeCodecs(USER_CODEC, JSON_NODE_CODEC)
        }
        createSchema(session)
        insertFromJson(session)
        selectToJson(session)
    }

    private fun createSchema(session: CqlSession) {
        val functionTypeQuery =
            """
            CREATE TYPE IF NOT EXISTS json_jackson_function_user( name text, age int )
            """.trimIndent()

        val tableQuery =
            """
            CREATE TABLE IF NOT EXISTS json_jackson_function(
                id int PRIMARY KEY,
                user frozen<json_jackson_function_user>, 
                scores map<varchar, float>
            )
            """.trimIndent()

        session.execute(functionTypeQuery).wasApplied().shouldBeTrue()
        session.execute(tableQuery).wasApplied().shouldBeTrue()
    }

    private fun insertFromJson(session: CqlSession) {
        val alice = User("alice", 30)
        val bob = User("bob", 35)

        val aliceScores = JsonNodeFactory.instance.objectNode()
            .put("call_of_duty", 4.8)
            .put("pokemon_go", 9.7)

        val bobScores = JsonNodeFactory.instance.objectNode()
            .put("zelda", 8.3)
            .put("pokemon_go", 12.4)

        val stmt = insertInto("json_jackson_function")
            .value("id", 1.literal())
            .value("user", function("fromJson", alice.literal(session.context.codecRegistry)))
            .value("scores", function("fromJson", aliceScores.literal(session.context.codecRegistry)))
            .build()

        log.debug { "query=${stmt.query}" }
        session.execute(stmt)

        val stmt2 = insertInto("json_jackson_function")
            .value("id", "id".bindMarker())
            .value("user", function("fromJson", "user".bindMarker()))
            .value("scores", function("fromJson", "scores".bindMarker()))
            .build()
        val ps = session.prepare(stmt2)
        log.debug { "query=${ps.query}" }

        val bs = ps.bind()
            .setValue<BoundStatement, Int>("id", 2)
            .setValue<BoundStatement, User>("user", bob)
            .setValue<BoundStatement, JsonNode>("scores", bobScores)
        // .set("user", bob, User::class.java)
        // .set("scores", bobScores, JsonNode::class.java)
        session.execute(bs)
    }

    private fun selectToJson(session: CqlSession) {
        val stmt = selectFrom("json_jackson_function")
            .column("id")
            .function("toJson", Selector.column("user")).`as`("user")
            .function("toJson", Selector.column("scores")).`as`("scores")
            .whereColumn("id").inValues(1.literal(), 2.literal())
            .build()

        log.debug { "query=${stmt.query}" }

        val rows = session.execute(stmt)

        rows.forEach { row ->
            val id = row.getInt("id")
            val user = row.getValue<User>("user")
            val userJson = row.getStringOrEmpty("user")
            val scores = row.getValue<JsonNode>("scores")
            val scoresJson = row.getStringOrEmpty("scores")

            log.debug {
                """
                Retrieved row:
                    id          = $id
                    user        = $user
                    userJson    = $userJson
                    scores      = $scores
                    scoresJson  = $scoresJson
                """.trimIndent()
            }
        }
    }
}
