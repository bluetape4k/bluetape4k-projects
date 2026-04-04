package io.bluetape4k.feign.codec

import com.alibaba.fastjson2.toJSONString
import feign.RequestTemplate
import feign.codec.EncodeException
import io.bluetape4k.logging.KLogging
import java.lang.reflect.Type

/**
 * fastjson2를 사용해 객체를 JSON 요청 본문으로 인코딩하는 Feign Encoder입니다.
 *
 * ## 동작/계약
 * - 입력 객체를 JSON 문자열로 직렬화해 UTF-8 바이트로 본문에 설정합니다.
 * - 인코딩 실패 시 [EncodeException]을 던집니다.
 *
 * ```kotlin
 * val encoder = FeignFastjsonEncoder()
 * // encoder.encode(obj, type, template)로 JSON 본문 설정
 * ```
 */
class FeignFastjsonEncoder: feign.codec.Encoder {

    companion object: KLogging() {
        val INSTANCE: FeignFastjsonEncoder by lazy { FeignFastjsonEncoder() }
    }

    /**
     * 객체를 JSON 문자열로 직렬화해 [RequestTemplate] 본문으로 설정합니다.
     *
     * ```kotlin
     * val encoder = FeignFastjsonEncoder()
     * val template = RequestTemplate()
     * encoder.encode(mapOf("key" to "value"), Map::class.java, template)
     * // template.body() != null
     * ```
     */
    override fun encode(obj: Any?, bodyType: Type?, template: RequestTemplate) {
        try {
            val json = obj.toJSONString()
            template.body(json.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
        } catch (e: Exception) {
            throw EncodeException(e.message, e)
        }
    }
}
