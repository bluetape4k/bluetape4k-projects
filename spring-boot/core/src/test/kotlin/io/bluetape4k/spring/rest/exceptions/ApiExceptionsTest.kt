package io.bluetape4k.spring.rest.exceptions

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.spring.AbstractSpringTest
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ApiExceptionsTest: AbstractSpringTest() {

    @Test
    fun `ApiEntityNotFoundException httpStatus는 NOT_FOUND`() {
        val ex = ApiEntityNotFoundException("not found")
        ex.httpStatus shouldBeEqualTo HttpStatus.NOT_FOUND
        ex.message shouldBeEqualTo "not found"
    }

    @Test
    fun `ApiEntityNotFoundException cause 생성자`() {
        val cause = IllegalArgumentException("original cause")
        val ex = ApiEntityNotFoundException(cause)
        ex.httpStatus shouldBeEqualTo HttpStatus.NOT_FOUND
        ex.cause shouldBeEqualTo cause
    }

    @Test
    fun `ApiEntityNotFoundException message와 cause 생성자`() {
        val cause = RuntimeException("cause")
        val ex = ApiEntityNotFoundException("custom message", cause)
        ex.message shouldBeEqualTo "custom message"
        ex.cause shouldBeEqualTo cause
    }

    @Test
    fun `ApiBadRequestException httpStatus는 BAD_REQUEST`() {
        val ex = ApiBadRequestException("bad request")
        ex.httpStatus shouldBeEqualTo HttpStatus.BAD_REQUEST
        ex.message shouldBeEqualTo "bad request"
    }

    @Test
    fun `ApiBadRequestException cause 생성자`() {
        val cause = IllegalArgumentException("invalid input")
        val ex = ApiBadRequestException(cause)
        ex.httpStatus shouldBeEqualTo HttpStatus.BAD_REQUEST
        ex.message shouldBeEqualTo "invalid input"
    }

    @Test
    fun `ApiTooManyRequestsException httpStatus는 TOO_MANY_REQUESTS`() {
        val ex = ApiTooManyRequestsException("too many")
        ex.httpStatus shouldBeEqualTo HttpStatus.TOO_MANY_REQUESTS
    }

    @Test
    fun `ApiTooManyRequestsException cause 생성자`() {
        val cause = RuntimeException("limit exceeded")
        val ex = ApiTooManyRequestsException(cause)
        ex.httpStatus shouldBeEqualTo HttpStatus.TOO_MANY_REQUESTS
        ex.message shouldBeEqualTo "limit exceeded"
    }

    @Test
    fun `ApiForbiddenException httpStatus는 FORBIDDEN`() {
        val ex = ApiForbiddenException("forbidden")
        ex.httpStatus shouldBeEqualTo HttpStatus.FORBIDDEN
    }

    @Test
    fun `ApiForbiddenException cause 생성자`() {
        val cause = RuntimeException("access denied")
        val ex = ApiForbiddenException(cause)
        ex.httpStatus shouldBeEqualTo HttpStatus.FORBIDDEN
        ex.message shouldBeEqualTo "access denied"
    }

    @Test
    fun `ApiUnauthorizedException httpStatus는 UNAUTHORIZED`() {
        val ex = ApiUnauthorizedException("unauthorized")
        ex.httpStatus shouldBeEqualTo HttpStatus.UNAUTHORIZED
    }

    @Test
    fun `ApiUnauthorizedException cause 생성자`() {
        val cause = RuntimeException("expired token")
        val ex = ApiUnauthorizedException(cause)
        ex.httpStatus shouldBeEqualTo HttpStatus.UNAUTHORIZED
        ex.message shouldBeEqualTo "expired token"
    }

    @Test
    fun `ApiInternalServerErrorException httpStatus는 INTERNAL_SERVER_ERROR`() {
        val ex = ApiInternalServerErrorException("internal error")
        ex.httpStatus shouldBeEqualTo HttpStatus.INTERNAL_SERVER_ERROR
    }

    @Test
    fun `ApiInternalServerErrorException cause 생성자`() {
        val cause = RuntimeException("db down")
        val ex = ApiInternalServerErrorException(cause)
        ex.httpStatus shouldBeEqualTo HttpStatus.INTERNAL_SERVER_ERROR
        ex.message shouldBeEqualTo "db down"
    }

    @Test
    fun `ApiServiceUnavailableException httpStatus는 SERVICE_UNAVAILABLE`() {
        val ex = ApiServiceUnavailableException("maintenance")
        ex.httpStatus shouldBeEqualTo HttpStatus.SERVICE_UNAVAILABLE
    }

    @Test
    fun `ApiServiceUnavailableException cause 생성자`() {
        val cause = RuntimeException("temporarily down")
        val ex = ApiServiceUnavailableException(cause)
        ex.httpStatus shouldBeEqualTo HttpStatus.SERVICE_UNAVAILABLE
        ex.message shouldBeEqualTo "temporarily down"
    }

    @Test
    fun `모든 예외는 RuntimeException 타입`() {
        val exceptions: List<ApiException> = listOf(
            ApiEntityNotFoundException("test"),
            ApiBadRequestException("test"),
            ApiTooManyRequestsException("test"),
            ApiForbiddenException("test"),
            ApiUnauthorizedException("test"),
            ApiInternalServerErrorException("test"),
            ApiServiceUnavailableException("test"),
        )
        exceptions.forEach { ex ->
            ex shouldBeInstanceOf RuntimeException::class
            ex.message shouldBeEqualTo "test"
        }
    }

    @Test
    fun `cause가 null인 메시지 only 생성자`() {
        val ex = ApiBadRequestException("msg only")
        ex.message shouldBeEqualTo "msg only"
        ex.cause.shouldBeNull()
    }

    @Test
    fun `message와 null cause 생성자`() {
        val ex = ApiInternalServerErrorException("msg", null)
        ex.message shouldBeEqualTo "msg"
        ex.cause.shouldBeNull()
    }

    @Test
    fun `cause 메시지 없을 때 기본 메시지 사용`() {
        val causeNoMsg = RuntimeException()
        val ex = ApiBadRequestException(causeNoMsg)
        ex.message shouldBeEqualTo "Bad request"
        ex.cause.shouldBeNull()
    }
}
