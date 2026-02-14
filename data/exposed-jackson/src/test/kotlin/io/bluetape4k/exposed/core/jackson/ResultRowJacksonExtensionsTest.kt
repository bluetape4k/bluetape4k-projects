package io.bluetape4k.exposed.core.jackson

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class ResultRowJacksonExtensionsTest: AbstractExposedTest() {

    private data class Payload(val user: JacksonSchema.User, val active: Boolean)

    private object JsonTextTable: Table("jackson_result_row_test") {
        val jsonText = text("json_text")
        val nullableText = text("nullable_text").nullable()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ResultRow Jackson 전용 getter를 지원한다`(testDB: TestDB) {
        val payload = Payload(JacksonSchema.User("tester", "A"), true)
        val jsonText = DefaultJacksonSerializer.serializeAsString(payload)

        withTables(testDB, JsonTextTable) {
            JsonTextTable.insert {
                it[JsonTextTable.jsonText] = jsonText
                it[nullableText] = null
            }

            val row = JsonTextTable.selectAll().single()

            row.getJackson<Payload>(JsonTextTable.jsonText) shouldBeEqualTo payload
            row.getJsonNode(JsonTextTable.jsonText).path("user").path("name").asText() shouldBeEqualTo "tester"
            row.getJacksonOrNull<Payload>(JsonTextTable.nullableText) shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ResultRow Jackson non-null getter는 null일 때 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, JsonTextTable) {
            JsonTextTable.insert {
                it[jsonText] = """{"a":1}"""
                it[nullableText] = null
            }

            val row = JsonTextTable.selectAll().single()

            assertFailsWith<IllegalStateException> {
                row.getJsonNode(JsonTextTable.nullableText)
            }
        }
    }
}
