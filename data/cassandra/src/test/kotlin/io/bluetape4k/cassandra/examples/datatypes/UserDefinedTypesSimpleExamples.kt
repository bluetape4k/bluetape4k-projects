package io.bluetape4k.cassandra.examples.datatypes

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.type.UserDefinedType
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class UserDefinedTypesSimpleExamples: AbstractCassandraTest() {

    companion object: KLoggingChannel()

    @Test
    fun `사용자 정의 수형에 대한 처리 예`() {
        createSchema(session)
        insertData(session)
        retreiveData(session)
    }

    private fun createSchema(session: CqlSession) {
        session.execute("CREATE TYPE IF NOT EXISTS examples.coordinates(x int, y int)")
        session.execute("CREATE TABLE IF NOT EXISTS examples.udts(k int PRIMARY KEY, c coordinates)")
    }

    private fun insertData(session: CqlSession) {
        val ps = session.prepare("INSERT INTO examples.udts (k, c) VALUES (?, ?)")

        // retrieve the user-defined type metadata
        val coordinatesType = retrieveCoordinatesType(session)

        val coordinates1 = coordinatesType.newValue(12, 34)
        val boundStmt1 = ps.bind(1, coordinates1)
        session.execute(boundStmt1)

        val coordinates2 = coordinatesType.newValue(56, 78)
        val boundStmt2 = ps.bind().setInt("k", 2).setUdtValue("c", coordinates2)
        session.execute(boundStmt2)
    }

    private fun retreiveData(session: CqlSession) {
        for (k in 1..2) {
            val stmt = SimpleStatement.newInstance("SELECT c FROM examples.udts WHERE k=?", k)

            val row = session.execute(stmt).one()
            row.shouldNotBeNull()

            val coordinatesValue = row.getUdtValue("c")
            coordinatesValue.shouldNotBeNull()

            log.debug { "Found coordinate: (${coordinatesValue.getInt("x")}, ${coordinatesValue.getInt("y")})" }
        }
    }

    private fun retrieveCoordinatesType(session: CqlSession): UserDefinedType {
        return session
            .metadata
            .getKeyspace("examples")
            .flatMap { ks -> ks.getUserDefinedType("coordinates") }
            .orElseThrow(::IllegalArgumentException)
    }
}
