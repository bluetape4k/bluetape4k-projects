package io.bluetape4k.feign.codec

import com.alibaba.fastjson2.toJSONString
import feign.RequestTemplate
import feign.codec.EncodeException
import io.bluetape4k.logging.KLogging
import java.lang.reflect.Type

/**
 * `fastjson2` 을 사용하여, Kotlin 수형에 대해서도 처리가 가능한 Feign Encoder 입니다.
 */
class FeignFastjsonEncoder: feign.codec.Encoder {

    companion object: KLogging() {
        val INSTANCE: FeignFastjsonEncoder by lazy { FeignFastjsonEncoder() }
    }

    /**
     * Feign 연동에서 `encode` 함수를 제공합니다.
     */
    override fun encode(obj: Any?, bodyType: Type?, template: RequestTemplate) {
        try {
            val bytes = obj.toJSONString()
            template.body(bytes)
        } catch (e: Exception) {
            throw EncodeException(e.message, e)
        }
    }
}
