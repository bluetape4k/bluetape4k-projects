package io.bluetape4k.spring.rest

import io.bluetape4k.spring.AbstractSpringTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ApiErrorBodyTest: AbstractSpringTest() {

    @Test
    fun `ApiErrorBody 기본 생성`() {
        val body = ApiErrorBody(message = "invalid input")
        body.message shouldBeEqualTo "invalid input"
        body.errorCode.shouldBeNull()
        body.timestamp.shouldNotBeNull()
    }

    @Test
    fun `ApiErrorBody errorCode 설정`() {
        val body = ApiErrorBody(errorCode = "ERR-001", message = "error")
        body.errorCode shouldBeEqualTo "ERR-001"
    }

    @Test
    fun `apiErrorResponseEntityOf 기본 상태 코드`() {
        val response = apiErrorResponseEntityOf(message = "error")
        response.statusCode.value() shouldBeEqualTo HttpStatus.INTERNAL_SERVER_ERROR.value()
        response.body.shouldNotBeNull()
        response.body!!.message shouldBeEqualTo "error"
    }

    @Test
    fun `apiErrorResponseEntityOf 404 응답`() {
        val response = apiErrorResponseEntityOf(
            statusCode = HttpStatus.NOT_FOUND.value(),
            message = "not found",
            errorCode = "NOT_FOUND"
        )
        response.statusCode.value() shouldBeEqualTo 404
        response.body!!.errorCode shouldBeEqualTo "NOT_FOUND"
        response.body!!.message shouldBeEqualTo "not found"
    }

    @Test
    fun `apiErrorResponseEntityOf 500 응답`() {
        val response = apiErrorResponseEntityOf(
            statusCode = 500,
            message = "server error",
        )
        response.body.shouldNotBeNull()
        response.body!!.message shouldBeEqualTo "server error"
    }
}
