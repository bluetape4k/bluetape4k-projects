package io.bluetape4k.cassandra.querybuilder.schema

import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createFunction
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropFunction
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class FunctionStatementExamples {

    companion object: KLoggingChannel()

    @Test
    fun `generate createFunction`() {
        createFunction("ks", "func1")
            .withParameter("param1", DataTypes.TIMESTAMP)
            .returnsNullOnNull()
            .returnsType(DataTypes.DOUBLE)
            .toString() shouldBeEqualTo
                "CREATE FUNCTION ks.func1 (param1 timestamp) RETURNS NULL ON NULL INPUT RETURNS double"

        createFunction("ks", "func1")
            .withParameter("param1", DataTypes.TIMESTAMP)
            .returnsNullOnNull()
            .returnsType(DataTypes.DOUBLE)
            .withJavaLanguage()
            .toString() shouldBeEqualTo
                "CREATE FUNCTION ks.func1 (param1 timestamp) RETURNS NULL ON NULL INPUT RETURNS double LANGUAGE java"

        createFunction("ks", "func1")
            .withParameter("param1", DataTypes.tupleOf(DataTypes.INT, DataTypes.INT))
            .returnsNullOnNull()
            .returnsType(SchemaBuilder.udt("person", true))
            .withJavaLanguage()
            .`as`("'return Integer.toString(param1);'")
            .toString() shouldBeEqualTo
                "CREATE FUNCTION ks.func1 (param1 tuple<int, int>) RETURNS NULL ON NULL INPUT " +
                "RETURNS person LANGUAGE java AS 'return Integer.toString(param1);'"

        createFunction("ks", "func1")
            .withParameter("param1", DataTypes.tupleOf(DataTypes.INT, DataTypes.INT))
            .returnsNullOnNull()
            .returnsType(SchemaBuilder.udt("person", true))
            .withJavaLanguage()
            .asQuoted("return Integer.toString(param1);")
            .toString() shouldBeEqualTo
                "CREATE FUNCTION ks.func1 (param1 tuple<int, int>) " +
                "RETURNS NULL ON NULL INPUT RETURNS person LANGUAGE java AS 'return Integer.toString(param1);'"

        createFunction("ks", "func1")
            .withParameter("param1", DataTypes.TIMESTAMP)
            .withParameter("param2", DataTypes.INT)
            .withParameter("param3", DataTypes.BOOLEAN)
            .returnsNullOnNull()
            .returnsType(DataTypes.DOUBLE)
            .withJavaLanguage()
            .toString() shouldBeEqualTo
                "CREATE FUNCTION ks.func1 (param1 timestamp,param2 int,param3 boolean) " +
                "RETURNS NULL ON NULL INPUT RETURNS double LANGUAGE java"

        createFunction("ks", "func1")
            .returnsNullOnNull()
            .returnsType(DataTypes.TEXT)
            .withJavaLanguage()
            .asQuoted("""return "hello world";""")
            .toString() shouldBeEqualTo
                """CREATE FUNCTION ks.func1 () RETURNS NULL ON NULL INPUT RETURNS text LANGUAGE java AS 'return "hello world";'"""

        createFunction("ks", "func1")
            .orReplace()
            .withParameter("param1", DataTypes.TIMESTAMP)
            .returnsNullOnNull()
            .returnsType(DataTypes.DOUBLE)
            .toString() shouldBeEqualTo
                "CREATE OR REPLACE FUNCTION ks.func1 (param1 timestamp) " +
                "RETURNS NULL ON NULL INPUT RETURNS double"
    }

    @Test
    fun `generate dropFunction`() {
        dropFunction("ks", "func1")
            .ifExists()
            .asCql() shouldBeEqualTo "DROP FUNCTION IF EXISTS ks.func1"
    }
}
