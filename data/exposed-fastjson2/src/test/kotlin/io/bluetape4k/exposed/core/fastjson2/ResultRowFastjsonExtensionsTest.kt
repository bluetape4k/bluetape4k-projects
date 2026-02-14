package io.bluetape4k.exposed.core.fastjson2

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.fastjson2.FastjsonSerializer
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.test.assertFailsWith

class ResultRowFastjsonExtensionsTest: AbstractExposedTest() {

    private data class Payload(val user: FastjsonSchema.User, val active: Boolean)

    private object JsonTextTable: Table("fastjson_result_row_test") {
        val jsonObjectText = text("json_object_text")
        val jsonArrayText = text("json_array_text")
        val nullableText = text("nullable_text").nullable()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ResultRow Fastjson 전용 getter를 지원한다`(testDB: TestDB) {
        val payload = Payload(FastjsonSchema.User("tester", "A"), true)
        val objectText = FastjsonSerializer.Default.serializeAsString(payload)
        val arrayText = """[1,2,3]"""

        withTables(testDB, JsonTextTable) {
            JsonTextTable.insert {
                it[jsonObjectText] = objectText
                it[jsonArrayText] = arrayText
                it[nullableText] = null
            }

            val row = JsonTextTable.selectAll().single()

            row.getFastjson<Payload>(JsonTextTable.jsonObjectText) shouldBeEqualTo payload
            row.getFastjsonObject(JsonTextTable.jsonObjectText).getJSONObject("user")
                .getString("name") shouldBeEqualTo "tester"
            row.getFastjsonArray(JsonTextTable.jsonArrayText).size shouldBeEqualTo 3
            row.getFastjsonOrNull<Payload>(JsonTextTable.nullableText) shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ResultRow Fastjson non-null getter는 null일 때 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, JsonTextTable) {
            JsonTextTable.insert {
                it[jsonObjectText] = """{"a":1}"""
                it[jsonArrayText] = """[1]"""
                it[nullableText] = null
            }

            val row = JsonTextTable.selectAll().single()

            assertFailsWith<IllegalStateException> {
                row.getFastjsonObject(JsonTextTable.nullableText)
            }
        }
    }
}
