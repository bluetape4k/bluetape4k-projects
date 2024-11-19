package io.bluetape4k.cassandra.querybuilder.schema

import com.datastax.oss.driver.api.core.type.DataTypes
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.alterType
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.createType
import com.datastax.oss.driver.api.querybuilder.SchemaBuilder.dropType
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class TypeStatementExamples {

    companion object: KLogging()

    @Test
    fun `generate alterType`() {
        alterType("foo").toString() shouldBeEqualTo "ALTER TYPE foo"

        alterType("ks", "foo")
            .alterField("x", DataTypes.TEXT)
            .asCql() shouldBeEqualTo "ALTER TYPE ks.foo ALTER x TYPE text"

        alterType("ks", "foo")
            .addField("x", DataTypes.TEXT)
            .asCql() shouldBeEqualTo "ALTER TYPE ks.foo ADD x text"

        alterType("ks", "foo")
            .renameField("x", "y")
            .renameField("u", "v")
            .asCql() shouldBeEqualTo "ALTER TYPE ks.foo RENAME x TO y AND u TO v"
    }

    @Test
    fun `createType with single field`() {
        createType("ks", "type")
            .withField("single", DataTypes.TEXT)
            .asCql() shouldBeEqualTo "CREATE TYPE ks.type (single text)"

        createType("ks", "type")
            .ifNotExists()
            .withField("single", DataTypes.TEXT)
            .asCql() shouldBeEqualTo "CREATE TYPE IF NOT EXISTS ks.type (single text)"
    }

    @Test
    fun `createType with many field`() {
        createType("ks", "type")
            .withField("first", DataTypes.TEXT)
            .withField("second", DataTypes.INT)
            .withField("third", DataTypes.BLOB)
            .withField("fourth", DataTypes.BOOLEAN)
            .asCql() shouldBeEqualTo
                "CREATE TYPE ks.type (first text,second int,third blob,fourth boolean)"
    }

    @Test
    fun `createType with nested udt`() {
        createType("ks", "type")
            .withField("nested", SchemaBuilder.udt("val", true))
            .asCql() shouldBeEqualTo "CREATE TYPE ks.type (nested frozen<val>)"

        createType("ks", "type")
            .withField("nested", SchemaBuilder.udt("val", false))
            .asCql() shouldBeEqualTo "CREATE TYPE ks.type (nested val)"
    }

    @Test
    fun `createType with collections`() {
        createType("ks", "type")
            .withField("names", DataTypes.listOf(DataTypes.TEXT))
            .asCql() shouldBeEqualTo "CREATE TYPE ks.type (names list<text>)"

        createType("ks", "type")
            .withField("names", DataTypes.setOf(DataTypes.TEXT))
            .asCql() shouldBeEqualTo "CREATE TYPE ks.type (names set<text>)"

        createType("ks", "type")
            .withField("names", DataTypes.tupleOf(DataTypes.TEXT))
            .asCql() shouldBeEqualTo "CREATE TYPE ks.type (names frozen<tuple<text>>)"


        createType("ks", "type")
            .withField("names", DataTypes.mapOf(DataTypes.INT, DataTypes.TEXT))
            .asCql() shouldBeEqualTo "CREATE TYPE ks.type (names map<int, text>)"
    }

    @Test
    fun `generate dropType`() {
        dropType("ks", "foo")
            .ifExists()
            .asCql() shouldBeEqualTo "DROP TYPE IF EXISTS ks.foo"
    }
}
