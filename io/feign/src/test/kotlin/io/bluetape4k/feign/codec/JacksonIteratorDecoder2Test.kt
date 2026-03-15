package io.bluetape4k.feign.codec

import feign.Request
import feign.Request.HttpMethod
import feign.Util
import io.bluetape4k.feign.AbstractFeignTest
import io.bluetape4k.feign.feignResponse
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [JacksonIteratorDecoder2]의 JSON 배열 스트리밍 디코딩 동작을 검증합니다.
 */
class JacksonIteratorDecoder2Test : AbstractFeignTest() {
    companion object : KLogging()

    private val decoder = JacksonIteratorDecoder2.INSTANCE

    /** 테스트에서 공통으로 사용하는 더미 GET 요청 객체입니다. */
    private val dummyRequest: Request by lazy {
        Request.create(HttpMethod.GET, "/api", emptyMap(), null, Charsets.UTF_8, null)
    }

    @Test
    fun `decode json array as iterator`() {
        val json = """[{"name":"alice"},{"name":"bob"}]"""

        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/json")))
                body(json, Charsets.UTF_8)
            }

        val iteratorType =
            object : java.lang.reflect.ParameterizedType {
                override fun getActualTypeArguments() = arrayOf<java.lang.reflect.Type>(Map::class.java)

                override fun getRawType() = Iterator::class.java

                override fun getOwnerType() = null
            }

        val result = decoder.decode(response, iteratorType)
        result.shouldNotBeNull()
        val iterator = result as Iterator<*>
        iterator.hasNext() shouldBeEqualTo true
        val first = iterator.next()
        first.shouldNotBeNull()
    }

    @Test
    fun `decode 204 returns empty value`() {
        val response =
            feignResponse {
                status(204)
                reason("No Content")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/json")))
            }

        val iteratorType =
            object : java.lang.reflect.ParameterizedType {
                override fun getActualTypeArguments() = arrayOf<java.lang.reflect.Type>(Map::class.java)

                override fun getRawType() = Iterator::class.java

                override fun getOwnerType() = null
            }

        // 204 응답은 Util.emptyValueOf 반환
        val result = decoder.decode(response, iteratorType)
        result shouldBeEqualTo Util.emptyValueOf(iteratorType)
    }

    @Test
    fun `decode empty body returns null`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("application/json")))
                body("", Charsets.UTF_8)
            }

        val iteratorType =
            object : java.lang.reflect.ParameterizedType {
                override fun getActualTypeArguments() = arrayOf<java.lang.reflect.Type>(Map::class.java)

                override fun getRawType() = Iterator::class.java

                override fun getOwnerType() = null
            }

        val result = decoder.decode(response, iteratorType)
        result.shouldBeNull()
    }

    @Test
    fun `decode non-json falls back to default decoder`() {
        val response =
            feignResponse {
                status(200)
                reason("OK")
                request(dummyRequest)
                headers(mapOf("content-type" to listOf("text/plain")))
                body("plain text", Charsets.UTF_8)
            }

        val result = decoder.decode(response, String::class.java)
        result shouldBeEqualTo "plain text"
    }
}
