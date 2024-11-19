package io.bluetape4k.cassandra.querybuilder.schema

import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createAggregate
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropAggregate
import io.bluetape4k.cassandra.querybuilder.tupleTerm
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class AggregationStatementExamples {

    companion object: KLogging()

    @Test
    fun `generate createAggregate`() {
        createAggregate("ks", "agg1")
            .withParameter(DataTypes.INT)
            .withSFunc("sfunction")
            .withSType(DataTypes.ASCII)
            .withFinalFunc("finalfunction")
            .withInitCond(tupleTerm(0, 0))
            .asCql() shouldBeEqualTo
                "CREATE AGGREGATE ks.agg1 (int) SFUNC sfunction STYPE ascii FINALFUNC finalfunction INITCOND (0,0)"

        createAggregate("ks", "agg1")
            .withParameter(DataTypes.INT)
            .withParameter(DataTypes.TEXT)
            .withParameter(DataTypes.BOOLEAN)
            .withSFunc("sfunction")
            .withSType(DataTypes.ASCII)
            .withFinalFunc("finalfunction")
            .withInitCond(tupleTerm(0, 0))
            .asCql() shouldBeEqualTo
                "CREATE AGGREGATE ks.agg1 (int,text,boolean) SFUNC sfunction STYPE ascii FINALFUNC finalfunction INITCOND (0,0)"
    }

    @Test
    fun `dropAggregate with keyspace`() {
        dropAggregate("ks", "agg1")
            .ifExists()
            .asCql() shouldBeEqualTo "DROP AGGREGATE IF EXISTS ks.agg1"
    }
}
