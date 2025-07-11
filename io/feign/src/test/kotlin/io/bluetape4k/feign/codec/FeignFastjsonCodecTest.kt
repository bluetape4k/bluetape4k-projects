package io.bluetape4k.feign.codec

import feign.Request
import feign.Request.HttpMethod
import feign.RequestTemplate
import io.bluetape4k.feign.AbstractFeignTest
import io.bluetape4k.feign.feignResponse
import io.bluetape4k.logging.KLogging
import io.bluetape4k.support.toUtf8String
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class FeignFastjsonCodecTest: AbstractFeignTest() {

    companion object: KLogging()

    private val zonesJson =
        """
        [
            {
                "name": "denominator.io."
            },
            {
                "name": "denominator.io.",
                "id": "ABCD"
            }
        ]
        """.trimIndent()

    @Test
    fun `encode map object numerical values as Int`() {
        val map = mutableMapOf<String, Any?>("foo" to 1)
        val template = RequestTemplate()

        FeignFastjsonEncoder.INSTANCE.encode(map, map.javaClass, template)
        template.body().toUtf8String() shouldBeEqualTo """{"foo":1}"""
    }

    @Test
    fun `decode basic type`() {
        val expected = "HELLO"

        val response = feignResponse {
            status(200)
            reason("OK")
            request(Request.create(HttpMethod.GET, "/api", emptyMap(), null, Charsets.UTF_8, null))
            headers(mapOf("content-type" to listOf("text/plain")))
            body(expected, Charsets.UTF_8)
        }

        FeignFastjsonDecoder.INSTANCE.decode<String>(response) shouldBeEqualTo expected
    }
}
