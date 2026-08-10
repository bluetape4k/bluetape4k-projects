package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.CqlIdentifier
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.cql.boundStatement
import io.bluetape4k.cassandra.cql.boundStatementOf
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class SettableSupportIntegrationTest: AbstractCassandraTest() {

    @BeforeAll
    fun createTable() {
        session.execute("DROP TABLE IF EXISTS settable_support")
        session.execute(
            """
            CREATE TABLE settable_support (
                id text PRIMARY KEY,
                tags list<text>,
                roles set<text>,
                attributes map<text, int>
            )
            """.trimIndent()
        )
    }

    @Test
    fun `setMap overloads support name index and identifier receivers`() {
        val prepared = session.prepare(
            "INSERT INTO settable_support (id, tags, roles, attributes) VALUES (?, ?, ?, ?)"
        )
        val map = mapOf("role" to 1)
        val id = CqlIdentifier.fromCql("attributes")

        val bound = prepared.bind()
            .setString(0, "map-key")
            .setList(1, listOf("a"), String::class.java)
            .setSet(2, setOf("admin"), String::class.java)

        bound.setMap("attributes", map)
        bound.setMap(3, map)
        bound.setMap(id, map)

        val updated = boundStatementOf(bound) {
            setString(0, "updated-key")
        }
        updated.getString(0) shouldBeEqualTo "updated-key"

        @Suppress("DEPRECATION")
        val deprecated = boundStatement(bound) {
            setString(0, "deprecated-key")
        }
        deprecated.getString(0) shouldBeEqualTo "deprecated-key"
    }
}
