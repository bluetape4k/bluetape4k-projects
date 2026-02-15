package io.bluetape4k.feign.codec

import com.alibaba.fastjson2.toJSONString
import feign.RequestTemplate
import feign.codec.EncodeException
import io.bluetape4k.logging.KLogging
import java.lang.reflect.Type

/**
 * `fastjson2`를 사용해 Kotlin 타입을 JSON 요청 본문으로 인코딩하는 Feign Encoder입니다.
 */
class FeignFastjsonEncoder: feign.codec.Encoder {

    companion object: KLogging() {
        val INSTANCE: FeignFastjsonEncoder by lazy { FeignFastjsonEncoder() }
    }

    /**
     * 객체를 JSON 문자열로 직렬화해 [RequestTemplate] 본문으로 설정합니다.
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
