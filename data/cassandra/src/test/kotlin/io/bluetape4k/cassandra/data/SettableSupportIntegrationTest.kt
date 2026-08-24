package io.bluetape4k.cassandra.data

import com.datastax.oss.driver.api.core.CqlIdentifier
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.cql.boundStatement
import io.bluetape4k.cassandra.cql.boundStatementOf
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
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

    @BeforeEach
    fun clearRows() {
        session.execute("TRUNCATE settable_support")
    }

    @Test
    fun `setMap overloads persist map values through Cassandra`() {
        val prepared = session.prepare(
            "INSERT INTO settable_support (id, tags, roles, attributes) VALUES (?, ?, ?, ?)"
        )
        val map = mapOf("role" to 1, "attempts" to 2)
        val id = CqlIdentifier.fromCql("attributes")

        val byName = prepared.bind()
            .setString(0, "set-map-name")
            .setList(1, listOf("a"), String::class.java)
            .setSet(2, setOf("admin"), String::class.java)
            .setMap("attributes", map)
        session.execute(byName)

        val byIndex = prepared.bind()
            .setString(0, "set-map-index")
            .setList(1, listOf("b"), String::class.java)
            .setSet(2, setOf("operator"), String::class.java)
            .setMap(3, map)
        session.execute(byIndex)

        val byIdentifier = prepared.bind()
            .setString(0, "set-map-identifier")
            .setList(1, listOf("c"), String::class.java)
            .setSet(2, setOf("auditor"), String::class.java)
            .setMap(id, map)
        session.execute(byIdentifier)

        assertPersistedAttributes("set-map-name", map)
        assertPersistedAttributes("set-map-index", map)
        assertPersistedAttributes("set-map-identifier", map)
    }

    @Suppress("DEPRECATION")
    @Test
    fun `bound statement helpers preserve existing values`() {
        val prepared = session.prepare(
            "INSERT INTO settable_support (id, tags, roles, attributes) VALUES (?, ?, ?, ?)"
        )
        val bound = prepared.bind()
            .setString(0, "map-key")
            .setList(1, listOf("a"), String::class.java)
            .setSet(2, setOf("admin"), String::class.java)
            .setMap("attributes", mapOf("role" to 1))

        val updated = boundStatementOf(bound) {
            setString(0, "updated-key")
        }
        updated.getString(0) shouldBeEqualTo "updated-key"

        val deprecated = boundStatement(bound) {
            setString(0, "deprecated-key")
        }
        deprecated.getString(0) shouldBeEqualTo "deprecated-key"
    }

    private fun assertPersistedAttributes(id: String, expected: Map<String, Int>) {
        val row = session.execute(
            "SELECT attributes FROM settable_support WHERE id='$id'"
        ).one()
        row.shouldNotBeNull()
        row.getMap("attributes", String::class.java, Int::class.javaObjectType) shouldBeEqualTo expected
    }
}
