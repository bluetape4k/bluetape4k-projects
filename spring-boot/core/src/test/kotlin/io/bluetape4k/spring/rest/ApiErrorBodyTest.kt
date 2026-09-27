package io.bluetape4k.spring.rest

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.spring.AbstractSpringTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ApiErrorBodyTest: AbstractSpringTest() {

    companion object: KLogging()

    // classpath의 Jackson 모듈을 자동 등록하는 ObjectMapper (JavaTimeModule 포함)
    private val objectMapper = Jackson.defaultJsonMapper

    @Test
    fun `ApiErrorBody 기본 생성`() {
        val body = ApiErrorBody(message = "invalid input")

        log.debug { "body: $body" }
        body.message shouldBeEqualTo "invalid input"
        body.errorCode.shouldBeNull()
        body.timestamp.shouldNotBeNull()
    }

    @Test
    fun `ApiErrorBody errorCode 설정`() {
        val body = ApiErrorBody(errorCode = "ERR-001", message = "error")

        log.debug { "body: $body" }
        body.errorCode shouldBeEqualTo "ERR-001"
    }

    @Test
    fun `apiErrorResponseEntityOf 기본 상태 코드`() {
        val response = apiErrorResponseEntityOf(message = "error")

        log.debug { "response=$response" }
        response.statusCode.value() shouldBeEqualTo HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.body?.message shouldBeEqualTo "error"
    }

    @Test
    fun `apiErrorResponseEntityOf 404 응답`() {
        val response = apiErrorResponseEntityOf(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "not found",
            errorCode = "NOT_FOUND"
        )
        log.debug { "response=$response" }
        response.statusCode.value() shouldBeEqualTo 404
        response.body?.errorCode shouldBeEqualTo "NOT_FOUND"
        response.body?.message shouldBeEqualTo "not found"
    }

    @Test
    fun `apiErrorResponseEntityOf 500 응답`() {
        val response = apiErrorResponseEntityOf(
            statusCode = 500,
            message = "server error",
        )
        log.debug { "response=$response" }
        response.body?.message shouldBeEqualTo "server error"
    }

    @Test
    fun `ApiErrorBody JSON 직렬화에 stackTraces 필드가 포함되지 않는다`() {
        val body = ApiErrorBody(errorCode = "ERR-001", message = "error occurred")
        val json = objectMapper.writeValueAsString(body)

        log.debug { "json=$json" }
        json shouldNotContain "stackTraces"
        json shouldNotContain "stackTrace"
    }

    @Test
    fun `apiErrorResponseEntityOf 응답 본문 JSON에 stackTraces 필드가 포함되지 않는다`() {
        val response = apiErrorResponseEntityOf(
            statusCode = 500,
            errorCode = "INTERNAL_ERROR",
            message = "server error",
        )
        val body = response.body
        body.shouldNotBeNull()

        val json = objectMapper.writeValueAsString(body)
        log.debug { "json=$json" }
        json shouldNotContain "stackTraces"
        json shouldNotContain "stackTrace"
    }
}
