package io.bluetape4k.spring.rest.exceptions

import io.bluetape4k.spring.AbstractSpringTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage

class ApiExceptionHandlerTest: AbstractSpringTest() {

    private val handler = ApiExceptionHandler()

    @Test
    fun `ApiBadRequestException 처리 - 400 반환`() {
        val response = handler.handle(ApiBadRequestException("invalid input"))
        response.statusCode.value() shouldBeEqualTo HttpStatus.BAD_REQUEST.value()
        response.body.shouldNotBeNull()
        response.body!!.message shouldBeEqualTo "invalid input"
    }

    @Test
    fun `ApiEntityNotFoundException 처리 - 404 반환`() {
        val response = handler.handle(ApiEntityNotFoundException("entity not found"))
        response.statusCode.value() shouldBeEqualTo HttpStatus.NOT_FOUND.value()
        response.body.shouldNotBeNull()
        response.body!!.message shouldBeEqualTo "entity not found"
    }

    @Test
    fun `ApiTooManyRequestsException 처리 - 429 반환`() {
        val response = handler.handle(ApiTooManyRequestsException("rate limit"))
        response.statusCode.value() shouldBeEqualTo HttpStatus.TOO_MANY_REQUESTS.value()
        response.body.shouldNotBeNull()
    }

    @Test
    fun `ApiForbiddenException 처리 - 403 반환`() {
        val response = handler.handle(ApiForbiddenException("forbidden"))
        response.statusCode.value() shouldBeEqualTo HttpStatus.FORBIDDEN.value()
        response.body.shouldNotBeNull()
    }

    @Test
    fun `ApiUnauthorizedException 처리 - 401 반환`() {
        val response = handler.handle(ApiUnauthorizedException("unauthorized"))
        response.statusCode.value() shouldBeEqualTo HttpStatus.UNAUTHORIZED.value()
        response.body.shouldNotBeNull()
    }

    @Test
    fun `ApiInternalServerErrorException 처리 - 500 반환`() {
        val response = handler.handle(ApiInternalServerErrorException("server error"))
        response.statusCode.value() shouldBeEqualTo HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.body.shouldNotBeNull()
    }

    @Test
    fun `ApiServiceUnavailableException 처리 - 503 반환`() {
        val response = handler.handle(ApiServiceUnavailableException("maintenance"))
        response.statusCode.value() shouldBeEqualTo HttpStatus.SERVICE_UNAVAILABLE.value()
        response.body.shouldNotBeNull()
    }

    @Test
    fun `HttpMessageNotReadableException 처리 - 400 반환`() {
        val inputMessage = MockHttpInputMessage("invalid json".toByteArray())
        val ex = HttpMessageNotReadableException("bad request body", inputMessage)
        val response = handler.handle(ex)
        response.statusCode.value() shouldBeEqualTo HttpStatus.BAD_REQUEST.value()
        response.body.shouldNotBeNull()
    }

    @Test
    fun `예외 원인 메시지가 응답 body에 포함`() {
        val cause = RuntimeException("root cause")
        val ex = ApiInternalServerErrorException(cause)
        val response = handler.handle(ex)
        response.body.shouldNotBeNull()
        response.statusCode.value() shouldBeEqualTo HttpStatus.INTERNAL_SERVER_ERROR.value()
    }
}
