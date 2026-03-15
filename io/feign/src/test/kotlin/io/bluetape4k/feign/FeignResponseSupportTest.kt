package io.bluetape4k.feign

import feign.Request
import feign.Request.HttpMethod
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * [FeignResponseSupport]의 Content-Type 판별 및 본문 접근 확장 함수를 검증합니다.
 */
class FeignResponseSupportTest : AbstractFeignTest() {
    companion object : KLogging()

    /** 테스트에서 공통으로 사용하는 더미 GET 요청 객체입니다. */
    private val dummyRequest: Request by lazy {
        Request.create(HttpMethod.GET, "/api", emptyMap(), null, Charsets.UTF_8, null)
    }

    @Test
    fun `isJsonBody returns true for application-json content type`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/json")))
                body("{}", Charsets.UTF_8)
            }
        response.isJsonBody().shouldBeTrue()
    }

    @Test
    fun `isJsonBody returns true for json subtype`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/vnd.api+json")))
                body("{}", Charsets.UTF_8)
            }
        response.isJsonBody().shouldBeTrue()
    }

    @Test
    fun `isJsonBody returns false for text-plain content type`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("text/plain")))
                body("hello", Charsets.UTF_8)
            }
        response.isJsonBody().shouldBeFalse()
    }

    @Test
    fun `isJsonBody returns false when content-type header is absent`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(emptyMap())
                body("hello", Charsets.UTF_8)
            }
        response.isJsonBody().shouldBeFalse()
    }

    @Test
    fun `isTextBody returns true for text-plain content type`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("text/plain; charset=UTF-8")))
                body("hello", Charsets.UTF_8)
            }
        response.isTextBody().shouldBeTrue()
    }

    @Test
    fun `isTextBody returns false for application-json content type`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/json")))
                body("{}", Charsets.UTF_8)
            }
        response.isTextBody().shouldBeFalse()
    }

    @Test
    fun `isTextBody returns false when content-type header is absent`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(emptyMap())
            }
        response.isTextBody().shouldBeFalse()
    }

    @Test
    fun `bodyAsReader reads response body content`() {
        val expected = "hello world"
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("text/plain")))
                body(expected, Charsets.UTF_8)
            }
        val reader = response.bodyAsReader()
        val content = reader.readText()
        content shouldBeEqualTo expected
    }
}
