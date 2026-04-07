package io.bluetape4k.feign.codec

import feign.Request
import feign.Request.HttpMethod
import feign.RequestTemplate
import io.bluetape4k.feign.AbstractFeignTest
import io.bluetape4k.feign.feignResponse
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

abstract class AbstractFeignCodecTest: AbstractFeignTest() {
    companion object: KLogging()

    abstract val encoder: feign.codec.Encoder
    abstract val decoder: feign.codec.Decoder

    /** 테스트에서 공통으로 사용하는 더미 GET 요청 객체입니다. */
    private val dummyRequest: Request by lazy {
        Request.create(HttpMethod.GET, "/api", emptyMap(), null, Charsets.UTF_8, null)
    }

    @Test
    fun `encode map object numerical values as Int`() {
        val map = mutableMapOf<String, Any?>("foo" to 1)
        val template = RequestTemplate()

        encoder.encode(map, map.javaClass, template)
        template.body().toUtf8String() shouldBeEqualTo """{"foo":1}"""
    }

    @Test
    fun `encode string value`() {
        val map = mutableMapOf<String, Any?>("name" to "alice")
        val template = RequestTemplate()

        encoder.encode(map, map.javaClass, template)
        template.body().toUtf8String() shouldBeEqualTo """{"name":"alice"}"""
    }

    @Test
    fun `encode null object produces null json`() {
        val template = RequestTemplate()
        encoder.encode(null, String::class.java, template)
        template.body().toUtf8String() shouldBeEqualTo "null"
    }

    @Test
    fun `decode basic type`() {
        val expected = "HELLO"

        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("text/plain")))
                body(expected, Charsets.UTF_8)
            }

        decoder.decode(response, String::class.java) shouldBeEqualTo expected
    }

    @Test
    fun `decode json object`() {
        val json = """{"foo":42}"""

        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/json")))
                body(json, Charsets.UTF_8)
            }

        @Suppress("UNCHECKED_CAST")
        val result = decoder.decode(response, Map::class.java) as? Map<String, Any>
        result.shouldNotBeNull()
        result["foo"].toString().toInt() shouldBeEqualTo 42
    }

    @Test
    fun `decode returns null on empty body`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/json")))
                body("", Charsets.UTF_8)
            }

        // 빈 본문이면 null 반환
        val result = decoder.decode(response, Map::class.java)
        result.shouldBeNull()
    }

    @Test
    fun `decode 204 No Content returns empty value`() {
        val response =
            feignResponse {
                status(204)
                reason("No Content")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/json")))
            }

        // 204 응답은 emptyValueOf(type) 반환 — List → emptyList, String → null
        val result = decoder.decode(response, List::class.java)
        // emptyValueOf(List) == Collections.emptyList()
        result.shouldNotBeNull()
    }

    @Test
    fun `decode 404 response returns empty value`() {
        val response =
            feignResponse {
                status(404)
                reason("Not Found")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/json")))
            }

        val result = decoder.decode(response, List::class.java)
        result.shouldNotBeNull()
    }
}
