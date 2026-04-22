package io.bluetape4k.mockserver.httpbin

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

/**
 * [HttpbinSupport] 확장 함수 단위 테스트.
 *
 * [toHeaderMap]과 [toHttpbinResponse]의 동작을 MockHttpServletRequest를 사용해 검증한다.
 */
class HttpbinSupportTest {

    companion object: KLogging()

    @Test
    fun `toHeaderMap은 요청 헤더를 Map으로 변환한다`() {
        val request = MockHttpServletRequest()
        request.addHeader("Content-Type", "application/json")
        request.addHeader("X-Custom-Header", "custom-value")

        val headers = request.toHeaderMap()

        headers.shouldNotBeNull()
        headers.containsKey("Content-Type").shouldBeTrue()
        headers.containsKey("X-Custom-Header").shouldBeTrue()
        headers["Content-Type"] shouldBeEqualTo "application/json"
        headers["X-Custom-Header"] shouldBeEqualTo "custom-value"
    }

    @Test
    fun `toHeaderMap은 헤더가 없으면 빈 Map을 반환한다`() {
        val request = MockHttpServletRequest()

        val headers = request.toHeaderMap()

        headers.shouldNotBeNull()
<<<<<<< feat/coverage-improvement
        headers.shouldBeEmpty()
=======
>>>>>>> develop
    }

    @Test
    fun `toHttpbinResponse는 기본 요청 정보를 HttpbinResponse로 변환한다`() {
        val request = MockHttpServletRequest("GET", "/httpbin/get")
        request.remoteAddr = "127.0.0.1"
        request.serverName = "localhost"
        request.serverPort = 8080

        val response = request.toHttpbinResponse()

        response.shouldNotBeNull()
        response.origin shouldBeEqualTo "127.0.0.1"
        response.method shouldBeEqualTo "GET"
    }

    @Test
    fun `toHttpbinResponse는 쿼리 파라미터를 args로 변환한다`() {
        val request = MockHttpServletRequest("GET", "/httpbin/get")
        request.queryString = "foo=bar&baz=qux"
        request.remoteAddr = "127.0.0.1"

        val response = request.toHttpbinResponse()

        response.args.containsKey("foo").shouldBeTrue()
        response.args["foo"] shouldBeEqualTo "bar"
        response.args.containsKey("baz").shouldBeTrue()
        response.args["baz"] shouldBeEqualTo "qux"
    }

    @Test
    fun `toHttpbinResponse는 쿼리 파라미터가 없으면 빈 args를 반환한다`() {
        val request = MockHttpServletRequest("GET", "/httpbin/get")
        request.remoteAddr = "127.0.0.1"

        val response = request.toHttpbinResponse()

        response.args.shouldBeEmpty()
    }

    @Test
    fun `toHttpbinResponse는 JSON body를 json 필드에 담는다`() {
        val request = MockHttpServletRequest("POST", "/httpbin/post")
        request.contentType = "application/json"
        request.remoteAddr = "127.0.0.1"
        val jsonBody = """{"key":"value","num":42}"""

        val response = request.toHttpbinResponse(body = jsonBody, method = "POST")

        response.json.shouldNotBeNull()
        @Suppress("UNCHECKED_CAST")
        val jsonMap = response.json as Map<String, Any>
        jsonMap.containsKey("key").shouldBeTrue()
        jsonMap["key"] shouldBeEqualTo "value"
        response.data shouldBeEqualTo ""
    }

    @Test
    fun `toHttpbinResponse는 form body를 form 필드에 담는다`() {
        val request = MockHttpServletRequest("POST", "/httpbin/post")
        request.contentType = "application/x-www-form-urlencoded"
        request.remoteAddr = "127.0.0.1"
        val formBody = "username=alice&password=secret"

        val response = request.toHttpbinResponse(body = formBody, method = "POST")

        response.form.containsKey("username").shouldBeTrue()
        response.form["username"] shouldBeEqualTo "alice"
        response.form["password"] shouldBeEqualTo "secret"
        response.data shouldBeEqualTo ""
    }

    @Test
    fun `toHttpbinResponse는 plain text body를 data 필드에 담는다`() {
        val request = MockHttpServletRequest("POST", "/httpbin/post")
        request.contentType = "text/plain"
        request.remoteAddr = "127.0.0.1"
        val textBody = "hello world"

        val response = request.toHttpbinResponse(body = textBody, method = "POST")

        response.data shouldBeEqualTo "hello world"
        response.form.shouldBeEmpty()
        response.json shouldBeEqualTo null
    }

    @Test
    fun `toHttpbinResponse는 body가 null이면 data가 빈 문자열이다`() {
        val request = MockHttpServletRequest("DELETE", "/httpbin/delete")
        request.remoteAddr = "127.0.0.1"

        val response = request.toHttpbinResponse(body = null, method = "DELETE")

        response.data shouldBeEqualTo ""
        response.method shouldBeEqualTo "DELETE"
    }

    @Test
    fun `toHttpbinResponse는 method 파라미터가 없으면 요청 메서드를 사용한다`() {
        val request = MockHttpServletRequest("PUT", "/httpbin/put")
        request.remoteAddr = "127.0.0.1"

        val response = request.toHttpbinResponse()

        response.method shouldBeEqualTo "PUT"
    }

    @Test
    fun `toHttpbinResponse는 잘못된 JSON body를 json null로 처리한다`() {
        val request = MockHttpServletRequest("POST", "/httpbin/post")
        request.contentType = "application/json"
        request.remoteAddr = "127.0.0.1"
        val invalidJson = "not-valid-json"

        val response = request.toHttpbinResponse(body = invalidJson, method = "POST")

        response.json shouldBeEqualTo null
        response.data shouldBeEqualTo "not-valid-json"
    }

    @Test
    fun `toHttpbinResponse는 헤더를 headers 필드에 담는다`() {
        val request = MockHttpServletRequest("GET", "/httpbin/headers")
        request.addHeader("Accept", "application/json")
        request.remoteAddr = "127.0.0.1"

        val response = request.toHttpbinResponse()

        response.headers.containsKey("Accept").shouldBeTrue()
    }
}
