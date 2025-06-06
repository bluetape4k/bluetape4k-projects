package io.bluetape4k.cassandra.examples.datatypes

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.data.UdtValue
import com.datastax.oss.driver.api.core.type.UserDefinedType
import com.datastax.oss.driver.api.core.type.codec.MappingCodec
import com.datastax.oss.driver.api.core.type.codec.TypeCodec
import com.datastax.oss.driver.api.core.type.codec.registry.MutableCodecRegistry
import com.datastax.oss.driver.api.core.type.reflect.GenericType
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.cql.statementOf
import io.bluetape4k.cassandra.data.getValue
import io.bluetape4k.cassandra.data.setValue
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.Serializable

class UserDefinedTypesMappedExamples: AbstractCassandraTest() {

    companion object: KLoggingChannel()

    data class Coordinates(val x: Int, val y: Int): Serializable

    class CoordinatesCodec(
        innerCodec: TypeCodec<UdtValue>,
    ): MappingCodec<UdtValue, Coordinates>(innerCodec, GenericType.of(Coordinates::class.java)) {

        override fun getCqlType(): UserDefinedType = super.getCqlType() as UserDefinedType

        override fun innerToOuter(value: UdtValue?): Coordinates? =
            value?.let { Coordinates(it.getInt("x"), it.getInt("y")) }

        override fun outerToInner(value: Coordinates?): UdtValue? =
            value?.let { cqlType.newValue().setInt("x", it.x).setInt("y", it.y) }
    }

    @Test
    fun `사용자 Codec 을 이용하여 사용자 정의 수형 처리하기`() {
        createSchema(session)
        registerCoordinatesCodec(session)
        insertData(session)
        retrieveData(session)
    }

    private fun createSchema(session: CqlSession) {
        session.execute("CREATE TYPE IF NOT EXISTS examples.coordinates(x int, y int)")
        session.execute("CREATE TABLE IF NOT EXISTS examples.udts(k int PRIMARY KEY, c coordinates)")
    }

    private fun registerCoordinatesCodec(session: CqlSession) {
        val codecRegistry = session.context.codecRegistry as MutableCodecRegistry

        // create the tuple metadata
        val coordinatesType = retrieveCoordinatesType(session)
        // retrieve the driver built-in codec for the tuple "coordinates"
        val innerCodec = codecRegistry.codecFor<UdtValue>(coordinatesType)
        // create a custom codec to map the "coordinates" tuple to the Coordinates class
        val coordinatesCodec = CoordinatesCodec(innerCodec)

        // register the new codec
        codecRegistry.register(coordinatesCodec)
    }

    private fun retrieveCoordinatesType(session: CqlSession): UserDefinedType {
        return session
            .metadata
            .getKeyspace("examples")
            .flatMap { ks -> ks.getUserDefinedType("coordinates") }
            .orElseThrow(::IllegalArgumentException)
    }

    private fun insertData(session: CqlSession) {
        val ps = session.prepare("INSERT INTO examples.udts(k, c) VALUES (?, ?)")

        val coordinates1 = Coordinates(12, 34)
        val boundStmt1 = ps.bind(1, coordinates1)

        // insert coordinate1
        session.execute(boundStmt1)

        // alternate method: bind the parameters one by one
        val coordinates2 = Coordinates(56, 78)
        val boundStmt2 = ps.bind().setInt("k", 2).setValue("c", coordinates2)

        // insert coordinate2
        session.execute(boundStmt2)
    }

    private fun retrieveData(session: CqlSession) {
        for (k in 1..2) {
            val stmt = statementOf("SELECT c FROM examples.udts WHERE k=?", k)

            val row = session.execute(stmt).one()
            row.shouldNotBeNull()

            val coordinatesValue = row.getValue<Coordinates>("c")
            coordinatesValue.shouldNotBeNull()

            log.debug { "Found coordinate: $coordinatesValue" }
        }
    }
}
