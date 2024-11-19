package io.bluetape4k.cassandra.examples.datatypes

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.data.TupleValue
import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.core.type.TupleType
import com.datastax.oss.driver.api.core.type.codec.MappingCodec
import com.datastax.oss.driver.api.core.type.codec.TypeCodec
import com.datastax.oss.driver.api.core.type.codec.registry.MutableCodecRegistry
import com.datastax.oss.driver.api.core.type.reflect.GenericType
import io.bluetape4k.cassandra.AbstractCassandraTest
import io.bluetape4k.cassandra.data.getValue
import io.bluetape4k.cassandra.data.setValue
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.Serializable

class TuplesMappedExamples: AbstractCassandraTest() {

    companion object: KLogging()

    data class Coordinates(val x: Int, val y: Int): Serializable

    class CoordinatesCodec(
        innerCodec: TypeCodec<TupleValue>,
    ): MappingCodec<TupleValue, Coordinates>(innerCodec, GenericType.of(Coordinates::class.java)) {

        override fun getCqlType(): TupleType = super.getCqlType() as TupleType

        override fun innerToOuter(value: TupleValue?): Coordinates? =
            value?.let { Coordinates(it.getInt(0), it.getInt(1)) }

        override fun outerToInner(value: Coordinates?): TupleValue? =
            value?.let { cqlType.newValue().setInt(0, it.x).setInt(1, it.y) }
    }

    @Test
    fun `사용자 Codec 을 이용하여 Tuple 수형 처리하기`() {
        createSchema(session)
        registerCoordinatesCodec(session)
        insertData(session)
        retrieveData(session)
    }

    private fun createSchema(session: CqlSession) {
        session.execute("CREATE TABLE IF NOT EXISTS examples.tuples(k int PRIMARY KEY, c tuple<int, int>)")
    }

    private fun registerCoordinatesCodec(session: CqlSession) {
        val codecRegistry = session.context.codecRegistry as MutableCodecRegistry

        // create the tuple metadata
        val coordinatesType = DataTypes.tupleOf(DataTypes.INT, DataTypes.INT)
        // retrieve the driver built-in codec for the tuple "coordinates"
        val innerCodec = codecRegistry.codecFor<TupleValue>(coordinatesType)
        // create a custom codec to map the "coordinates" tuple to the Coordinates class
        val coordinatesCodec = CoordinatesCodec(innerCodec)

        // register the new codec
        codecRegistry.register(coordinatesCodec)
    }

    private fun insertData(session: CqlSession) {
        val ps = session.prepare("INSERT INTO examples.tuples(k, c) VALUES (?, ?)")

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
            val stmt = SimpleStatement.newInstance("SELECT c FROM examples.tuples WHERE k=?", k)

            val row = session.execute(stmt).one()
            row.shouldNotBeNull()

            val coordinatesValue = row.getValue<Coordinates>("c")
            coordinatesValue.shouldNotBeNull()

            println("Found coordinate: $coordinatesValue")
        }
    }
}
