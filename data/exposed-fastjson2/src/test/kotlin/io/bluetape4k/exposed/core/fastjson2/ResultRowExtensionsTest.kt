package io.bluetape4k.exposed.core.fastjson2

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.fastjson2.FastjsonSerializer
import io.mockk.every
import io.mockk.mockk
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import io.bluetape4k.assertions.assertFailsWith

class ResultRowExtensionsTest: AbstractExposedTest() {

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

    @Test
    fun `ResultRow getFastjsonOrNull은 ByteArray 값을 역직렬화한다`() {
        val payload = Payload(FastjsonSchema.User("bytes-user", "B"), false)
        val jsonBytes = FastjsonSerializer.Default.serialize(payload)

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns jsonBytes

        row.getFastjsonOrNull<Payload>(expr) shouldBeEqualTo payload
    }

    @Test
    fun `ResultRow getFastjsonOrNull은 이미 T 타입인 값을 그대로 반환한다`() {
        val payload = Payload(FastjsonSchema.User("direct-user", "C"), true)

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns payload

        row.getFastjsonOrNull<Payload>(expr) shouldBeEqualTo payload
    }

    @Test
    fun `ResultRow getFastjsonOrNull은 else 분기에서 toString으로 역직렬화한다`() {
        val payload = Payload(FastjsonSchema.User("tostring-user", "D"), true)
        val jsonText = FastjsonSerializer.Default.serializeAsString(payload)
        val customObj = object {
            override fun toString() = jsonText
        }

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns customObj

        row.getFastjsonOrNull<Payload>(expr) shouldBeEqualTo payload
    }

    @Test
    fun `ResultRow getFastjsonOrNull은 null 값에서 null을 반환한다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        row.getFastjsonOrNull<Payload>(expr).shouldBeNull()
    }

    @Test
    fun `ResultRow getFastjsonObjectOrNull은 ByteArray 값을 파싱한다`() {
        val jsonBytes = """{"x":123}""".toByteArray()

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns jsonBytes

        val obj = row.getFastjsonObjectOrNull(expr)
        obj.shouldNotBeNull()
        obj.getInteger("x") shouldBeEqualTo 123
    }

    @Test
    fun `ResultRow getFastjsonObjectOrNull은 이미 JSONObject인 값을 그대로 반환한다`() {
        val jsonObject: JSONObject = JSON.parseObject("""{"y":42}""")

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns jsonObject

        row.getFastjsonObjectOrNull(expr) shouldBeEqualTo jsonObject
    }

    @Test
    fun `ResultRow getFastjsonObjectOrNull은 else 분기에서 toString으로 파싱한다`() {
        val jsonText = """{"z":7}"""
        val customObj = object {
            override fun toString() = jsonText
        }

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns customObj

        val obj = row.getFastjsonObjectOrNull(expr)
        obj.shouldNotBeNull()
        obj.getInteger("z") shouldBeEqualTo 7
    }

    @Test
    fun `ResultRow getFastjsonObjectOrNull은 null 값에서 null을 반환한다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        row.getFastjsonObjectOrNull(expr).shouldBeNull()
    }

    @Test
    fun `ResultRow getFastjsonArrayOrNull은 ByteArray 값을 파싱한다`() {
        val jsonBytes = """[10,20,30]""".toByteArray()

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns jsonBytes

        val arr = row.getFastjsonArrayOrNull(expr)
        arr.shouldNotBeNull()
        arr.size shouldBeEqualTo 3
    }

    @Test
    fun `ResultRow getFastjsonArrayOrNull은 이미 JSONArray인 값을 그대로 반환한다`() {
        val jsonArray: JSONArray = JSON.parseArray("""[1,2,3]""")

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns jsonArray

        row.getFastjsonArrayOrNull(expr) shouldBeEqualTo jsonArray
    }

    @Test
    fun `ResultRow getFastjsonArrayOrNull은 else 분기에서 toString으로 파싱한다`() {
        val jsonText = """[7,8,9]"""
        val customObj = object {
            override fun toString() = jsonText
        }

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns customObj

        val arr = row.getFastjsonArrayOrNull(expr)
        arr.shouldNotBeNull()
        arr.size shouldBeEqualTo 3
    }

    @Test
    fun `ResultRow getFastjsonArrayOrNull은 null 값에서 null을 반환한다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        row.getFastjsonArrayOrNull(expr).shouldBeNull()
    }

    @Test
    fun `ResultRow getFastjson은 null 값에서 IllegalStateException을 던진다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        assertFailsWith<IllegalStateException> {
            row.getFastjson<Payload>(expr)
        }
    }

    @Test
    fun `ResultRow getFastjsonObject은 null 값에서 IllegalStateException을 던진다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        assertFailsWith<IllegalStateException> {
            row.getFastjsonObject(expr)
        }
    }

    @Test
    fun `ResultRow getFastjsonArray은 null 값에서 IllegalStateException을 던진다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        assertFailsWith<IllegalStateException> {
            row.getFastjsonArray(expr)
        }
    }
}
