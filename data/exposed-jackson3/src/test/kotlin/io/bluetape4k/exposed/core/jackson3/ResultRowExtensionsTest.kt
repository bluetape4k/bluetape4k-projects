package io.bluetape4k.exposed.core.jackson3

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
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
import tools.jackson.databind.JsonNode
import kotlin.test.assertFailsWith

class ResultRowExtensionsTest: AbstractExposedTest() {

    private data class Payload(val user: JacksonSchema.User, val active: Boolean)

    private object JsonTextTable: Table("jackson3_result_row_test") {
        val jsonText = text("json_text")
        val nullableText = text("nullable_text").nullable()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ResultRow Jackson3 전용 getter를 지원한다`(testDB: TestDB) {
        val payload = Payload(JacksonSchema.User("tester", "A"), true)
        val jsonText = DefaultJacksonSerializer.serializeAsString(payload)

        withTables(testDB, JsonTextTable) {
            JsonTextTable.insert {
                it[JsonTextTable.jsonText] = jsonText
                it[nullableText] = null
            }

            val row = JsonTextTable.selectAll().single()

            row.getJackson<Payload>(JsonTextTable.jsonText) shouldBeEqualTo payload
            row.getJsonNode(JsonTextTable.jsonText).path("user").path("name").asString() shouldBeEqualTo "tester"
            row.getJacksonOrNull<Payload>(JsonTextTable.nullableText) shouldBeEqualTo null
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `ResultRow Jackson3 non-null getter는 null일 때 예외를 던진다`(testDB: TestDB) {
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

    @Test
    fun `ResultRow getJacksonOrNull은 ByteArray 값을 역직렬화한다`() {
        val payload = Payload(JacksonSchema.User("bytes-user", "B"), false)
        val jsonBytes = DefaultJacksonSerializer.serialize(payload)

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns jsonBytes

        row.getJacksonOrNull<Payload>(expr) shouldBeEqualTo payload
    }

    @Test
    fun `ResultRow getJacksonOrNull은 이미 T 타입인 값을 그대로 반환한다`() {
        val payload = Payload(JacksonSchema.User("direct-user", "C"), true)

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns payload

        row.getJacksonOrNull<Payload>(expr) shouldBeEqualTo payload
    }

    @Test
    fun `ResultRow getJacksonOrNull은 else 분기에서 toString으로 역직렬화한다`() {
        val payload = Payload(JacksonSchema.User("tostring-user", "D"), true)
        val jsonText = DefaultJacksonSerializer.serializeAsString(payload)
        val customObj = object {
            override fun toString() = jsonText
        }

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns customObj

        row.getJacksonOrNull<Payload>(expr) shouldBeEqualTo payload
    }

    @Test
    fun `ResultRow getJacksonOrNull은 null 값에서 null을 반환한다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        row.getJacksonOrNull<Payload>(expr).shouldBeNull()
    }

    @Test
    fun `ResultRow getJsonNodeOrNull은 ByteArray 값을 파싱한다`() {
        val jsonBytes = """{"x":123}""".toByteArray()

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns jsonBytes

        val node = row.getJsonNodeOrNull(expr)
        node.shouldNotBeNull()
    }

    @Test
    fun `ResultRow getJsonNodeOrNull은 이미 JsonNode인 값을 그대로 반환한다`() {
        val jsonNode: JsonNode = DefaultJacksonSerializer.mapper.readTree("""{"y":42}""")

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns jsonNode

        row.getJsonNodeOrNull(expr) shouldBeEqualTo jsonNode
    }

    @Test
    fun `ResultRow getJsonNodeOrNull은 else 분기에서 toString으로 파싱한다`() {
        val jsonText = """{"z":7}"""
        val customObj = object {
            override fun toString() = jsonText
        }

        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns customObj

        val node = row.getJsonNodeOrNull(expr)
        node.shouldNotBeNull()
    }

    @Test
    fun `ResultRow getJsonNodeOrNull은 null 값에서 null을 반환한다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        row.getJsonNodeOrNull(expr).shouldBeNull()
    }

    @Test
    fun `ResultRow getJackson은 null 값에서 IllegalStateException을 던진다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        assertFailsWith<IllegalStateException> {
            row.getJackson<Payload>(expr)
        }
    }

    @Test
    fun `ResultRow getJsonNode은 null 값에서 IllegalStateException을 던진다`() {
        val expr = mockk<Expression<Any?>>()
        val row = mockk<ResultRow>()
        every { row.getOrNull(expr) } returns null

        assertFailsWith<IllegalStateException> {
            row.getJsonNode(expr)
        }
    }
}
