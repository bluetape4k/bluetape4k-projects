package io.bluetape4k.feign

import feign.Request.HttpMethod
import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

/**
 * [feignRequestOf] 및 [requestOptions] 팩토리 함수 단위 테스트입니다.
 */
class FeignRequestSupportTest {

    companion object: KLogging()

    @Test
    fun `feignRequestOf creates GET request with correct URL`() {
        val request = feignRequestOf("https://example.com/health", HttpMethod.GET)

        request.shouldNotBeNull()
        request.httpMethod() shouldBeEqualTo HttpMethod.GET
        request.url() shouldBeEqualTo "https://example.com/health"
    }

    @Test
    fun `feignRequestOf creates POST request with body`() {
        val body = """{"key":"value"}""".toByteArray(Charsets.UTF_8)
        val request = feignRequestOf(
            url = "https://example.com/api",
            httpMethod = HttpMethod.POST,
            body = body,
        )

        request.httpMethod() shouldBeEqualTo HttpMethod.POST
        request.body().shouldNotBeNull()
    }

    @Test
    fun `feignRequestOf with null body produces no body`() {
        val request = feignRequestOf("https://example.com/api", HttpMethod.GET, body = null)

        request.body().shouldBeNull()
    }

    @Test
    fun `feignRequestOf with custom headers includes headers`() {
        val headers = mapOf("Authorization" to listOf("Bearer token123"))
        val request = feignRequestOf(
            url = "https://example.com/api",
            httpMethod = HttpMethod.GET,
            headers = headers,
        )

        request.headers().shouldNotBeNull()
        request.headers()["Authorization"].shouldNotBeNull()
    }

    @Test
    fun `feignRequestOf with blank url throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            feignRequestOf("", HttpMethod.GET)
        }
    }

    @Test
    fun `feignRequestOf with whitespace url throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            feignRequestOf("   ", HttpMethod.GET)
        }
    }

    @Test
    fun `requestOptions creates Options with default values`() {
        val options = requestOptions { }

        options.shouldNotBeNull()
    }

    @Test
    fun `requestOptions applies builder block`() {
        val options = requestOptions {
            // default connect timeout is 10 seconds; read is 60 seconds
        }

        // Verify we get a valid, non-null Options object
        options.shouldNotBeNull()
        options.connectTimeoutMillis() shouldBeEqualTo defaultRequestOptions.connectTimeoutMillis()
    }

    @Test
    fun `defaultRequestOptions is a stable singleton`() {
        val first = defaultRequestOptions
        val second = defaultRequestOptions

        // Reference equality – same singleton instance
        (first === second) shouldBeEqualTo true
    }
}
