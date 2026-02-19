package io.bluetape4k.jdbc.sql

import io.bluetape4k.jdbc.model.Actor
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldContainAll
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class DataSourceSupportTest: AbstractJdbcSqlTest() {

    companion object: KLogging() {
        const val SELECT_ACTORS = "SELECT * FROM Actors"
    }

    @Test
    fun `with connection`() {
        dataSource.withConnect { conn ->
            conn.withStatement { stmt ->
                stmt.verifyQuery(SELECT_ACTORS)
            }
        }
    }

    @Test
    fun `with statement`() {
        dataSource.withStatement { stmt ->
            stmt.verifyQuery(SELECT_ACTORS)
        }
    }

    @Test
    fun `execute query`() {
        dataSource.runQuery(SELECT_ACTORS) { rs ->
            rs.shouldNotBeNull()
            rs.next().shouldBeTrue()
        }
    }

    @Test
    fun `executeQuery and instancing Actors`() {
        val actors = dataSource.runQuery(SELECT_ACTORS) { rs ->
            rs.extract {
                Actor(
                    id = int["id"],
                    firstname = string["firstname"],
                    lastname = string["lastname"]
                ).apply {
                    log.debug { "Actor:$this" }
                }
            }
        }

        actors.size shouldBeGreaterThan 0
        actors.map { it.lastname } shouldContainAll listOf("Bae", "Kwon")
    }

    @Test
    fun `executeUpdate with DataSource`() {
        val rowAffected = dataSource.executeUpdate("UPDATE Actors set lastname='BAE' where id=1")
        rowAffected shouldBeEqualTo 1
    }
}
