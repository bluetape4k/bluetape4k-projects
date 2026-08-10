package io.bluetape4k.vertx.sqlclient

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.vertx.sqlclient.tests.testWithSuspendRollback
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.junit5.VertxTestContext
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.RowSet
import io.vertx.kotlin.coroutines.coAwait
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test

class RowExtensionsTest: AbstractVertxSqlClientTest() {

    override val schemaFileNames: List<String> = listOf("person.sql")

    override fun Vertx.getPool() = getH2Pool()

    @Test
    fun `nullable row accessors cover scalar arrays and JSON values`(
        vertx: Vertx,
        testContext: VertxTestContext,
    ) = runSuspendIO {
        vertx.testWithSuspendRollback(testContext, pool) { conn: SqlConnection ->
            val rows: RowSet<Row> = conn.query(
                "select id, first_name, birth_date, employed from Person where id = 1",
            ).execute().coAwait()
            val row = rows.first()

            row.hasColumn(0).shouldBeTrue()
            row.hasColumn(99).shouldBeFalse()
            row.hasColumn("id").shouldBeTrue()
            row.hasColumn("missing").shouldBeFalse()
            row.valueAs<Int>("id") shouldBeEqualTo 1
            row.getValueOrNull("id") shouldBeEqualTo 1
            row.getIntOrNull("id") shouldBeEqualTo 1
            row.getStringOrNull("first_name") shouldBeEqualTo "Fred"
            row.getLocalDateOrNull("birth_date").shouldNotBeNull()
            row.getBooleanOrNull("employed").shouldNotBeNull()

            row.getValueOrNull("missing").shouldBeNull()
            row.getBooleanOrNull("missing").shouldBeNull()
            row.getShortOrNull("missing").shouldBeNull()
            row.getLongOrNull("missing").shouldBeNull()
            row.getFloatOrNull("missing").shouldBeNull()
            row.getDoubleOrNull("missing").shouldBeNull()
            row.getNumericOrNull("missing").shouldBeNull()
            row.getJsonOrNull("missing").shouldBeNull()
            row.getJsonObjectOrNull("missing").shouldBeNull()
            row.getJsonArrayOrNull("missing").shouldBeNull()
            row.getTemporalOrNull("missing").shouldBeNull()
            row.getLocalTimeOrNull("missing").shouldBeNull()
            row.getLocalDateTimeOrNull("missing").shouldBeNull()
            row.getOffsetDateTimeOrNull("missing").shouldBeNull()
            row.getBufferOrNull("missing").shouldBeNull()
            row.getUUIDOrNull("missing").shouldBeNull()
            row.getBigDecimalOrNull("missing").shouldBeNull()
            row.getArrayOfBooleansOrNull("missing").shouldBeNull()
            row.getArrayOfShortsOrNull("missing").shouldBeNull()
            row.getArrayOfIntegersOrNull("missing").shouldBeNull()
            row.getArrayOfLongsOrNull("missing").shouldBeNull()
            row.getArrayOfFloatsOrNull("missing").shouldBeNull()
            row.getArrayOfDoublesOrNull("missing").shouldBeNull()
            row.getArrayOfNumericsOrNull("missing").shouldBeNull()
            row.getArrayOfStringsOrNull("missing").shouldBeNull()
            row.getArrayOfJsonObjectsOrNull("missing").shouldBeNull()
            row.getArrayOfJsonArraysOrNull("missing").shouldBeNull()
            row.getArrayOfTemporalsOrNull("missing").shouldBeNull()
            row.getArrayOfLocalDatesOrNull("missing").shouldBeNull()
            row.getArrayOfLocalTimesOrNull("missing").shouldBeNull()
            row.getArrayOfLocalDateTimesOrNull("missing").shouldBeNull()
            row.getArrayOfOffsetDatesTimesOrNull("missing").shouldBeNull()
            row.getArrayOfBuffersOrNull("missing").shouldBeNull()
            row.getArrayOfUUIDsOrNull("missing").shouldBeNull()
            row.getArrayOfBigDecimalsOrNull("missing").shouldBeNull()
            row.getArrayOfJsonsOrNull("missing").shouldBeNull()
            row.getOrNull<Int>("missing").shouldBeNull()

            row.jsonEncode() shouldContain "\"id\":1"
            rows.jsonEncode() shouldContain "\"first_name\":\"Fred\""
        }
    }

    @Test
    @Suppress("DEPRECATION")
    fun `row mapper and primitive mappers map Vertx rows`() {
        val row = mockk<Row>(relaxed = true)
        every { row.toJson() } returns JsonObject().put("id", 1).put("name", "Fred").put("enabled", true)
        every { row.getInteger(0) } returns 7
        every { row.getLong(0) } returns 9L

        val mapper = io.bluetape4k.vertx.sqlclient.templates.rowMapperAs<Map<String, Any>>()
        mapper.map(row)["id"] shouldBeEqualTo 1
        mapper.map(row)["name"] shouldBeEqualTo "Fred"
        io.bluetape4k.vertx.sqlclient.templates.INT_ROW_MAPPER.map(row) shouldBeEqualTo 7
        io.bluetape4k.vertx.sqlclient.templates.LONG_ROW_MAPPER.map(row) shouldBeEqualTo 9L
    }
}
