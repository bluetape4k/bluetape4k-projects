package io.bluetape4k.r2dbc.convert.postgresql

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldHaveSize
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

    @Test
    fun `변환 실패 로그는 payload와 예외 원문의 비밀값을 포함하지 않는다`() {
        listOf(JsonToMapConverter::class.java, MapToJsonConverter::class.java).forEach { type ->
            val logger = LoggerFactory.getLogger(type) as Logger
            val appender = ListAppender<ILoggingEvent>().apply { start() }
            logger.addAppender(appender)
            try {
                assertFailsWith<ConversionFailedException> {
                    if (type == JsonToMapConverter::class.java) {
                        JsonToMapConverter(mapper).convert(Json.of("{\"password\":\"payload-secret\",broken}"))
                    } else {
                        val failure = JacksonException.wrapWithPath(
                            IllegalStateException("cause-secret"), "source", "field"
                        )
                        val failingMapper = mockk<ObjectMapper> {
                            every { writeValueAsString(any<Map<String, Any?>>()) } throws failure
                        }
                        MapToJsonConverter(failingMapper).convert(mapOf("password" to "payload-secret"))
                    }
                }
                appender.list shouldHaveSize 1
                appender.list.forEach {
                    it.formattedMessage shouldNotContain "payload-secret"
                    it.formattedMessage shouldNotContain "cause-secret"
                    it.throwableProxy.shouldBeNull()
                }
            } finally {
                logger.detachAppender(appender)
                appender.stop()
            }
        }
    }

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
