package io.bluetape4k.r2dbc.convert.postgresql

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.jackson3.Jackson
import io.mockk.every
import io.mockk.mockk
import io.r2dbc.postgresql.codec.Json
import org.junit.jupiter.api.Test
import org.springframework.core.convert.ConversionFailedException
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper

class PostgresJsonConvertersTest {

    private val mapper = Jackson.defaultJsonMapper

    @Test
    fun `JsonToMapConverter converts valid PostgreSQL Json`() {
        val converter = JsonToMapConverter(mapper)

        val result = converter.convert(Json.of("""{"name":"debop","active":true}"""))

        result["name"] shouldBeEqualTo "debop"
        result["active"] shouldBeEqualTo true
    }

    @Test
    fun `JsonToMapConverter fails malformed PostgreSQL Json without replacing data`() {
        val converter = JsonToMapConverter(mapper)

        val error = assertFailsWith<ConversionFailedException> {
            converter.convert(Json.of("""{"name":"""))
        }

        error.cause.shouldBeInstanceOf<JacksonException>()
    }

    @Test
    fun `MapToJsonConverter converts map to PostgreSQL Json`() {
        val converter = MapToJsonConverter(mapper)

        val json = converter.convert(mapOf("name" to "debop", "active" to true))

        json.asString() shouldBeEqualTo """{"name":"debop","active":true}"""
    }

    @Test
    fun `MapToJsonConverter fails serialization errors without replacing data`() {
        val jacksonFailure = JacksonException.wrapWithPath(IllegalStateException("boom"), "source", "field")
        val failingMapper = mockk<ObjectMapper> {
            every { writeValueAsString(any<Map<String, Any?>>()) } throws jacksonFailure
        }
        val converter = MapToJsonConverter(failingMapper)

        val error = assertFailsWith<ConversionFailedException> {
            converter.convert(mapOf("name" to "debop"))
        }

        error.cause shouldBeEqualTo jacksonFailure
    }
}
