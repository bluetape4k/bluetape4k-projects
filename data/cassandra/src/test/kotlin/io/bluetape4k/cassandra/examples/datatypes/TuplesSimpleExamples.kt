package io.bluetape4k.cassandra.examples.datatypes

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.type.DataTypes
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class TuplesSimpleExamples: AbstractCassandraTest() {

    companion object: KLogging()

    @Test
    fun `tuple 수형에 대한 처리`() {
        createSchema(session)
        insertData(session)
        retreiveData(session)
    }

    private fun createSchema(session: CqlSession) {
        session.execute(
            """
            CREATE TABLE IF NOT EXISTS examples.tuples(
                k int PRIMARY KEY,
                c tuple<int, int>
            )            
            """.trimIndent()
        )
            .wasApplied().shouldBeTrue()
    }

    private fun insertData(session: CqlSession) {
        val ps = session.prepare("INSERT INTO examples.tuples (k, c) VALUES (?, ?)")

        // create tuple metadata
        val coordinatesTypes = DataTypes.tupleOf(DataTypes.INT, DataTypes.INT)

        // bind the parameters
        val coordinate1 = coordinatesTypes.newValue(12, 34)
        val boundStmt = ps.bind(1, coordinate1)

        // execute insertion
        session.execute(boundStmt)

        val coordinate2 = coordinatesTypes.newValue(56, 78)
        val boundStmt2 = ps.bind().setInt("k", 2).setTupleValue("c", coordinate2)
        session.execute(boundStmt2)
    }

    private fun retreiveData(session: CqlSession) {
        for (k in 1..2) {
            val stmt = SimpleStatement.newInstance("SELECT c FROM examples.tuples WHERE k=?", k)

            val row = session.execute(stmt).one()
            row.shouldNotBeNull()

            val coordinatesValue = row.getTupleValue("c")
            coordinatesValue.shouldNotBeNull()

            println("Found coordinate: (${coordinatesValue.getInt(0)}, ${coordinatesValue.getInt(1)})")
        }
    }
}
