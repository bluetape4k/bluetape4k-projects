package io.bluetape4k.feign.codec

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONException
import feign.codec.DecodeException
import io.bluetape4k.feign.isJsonBody
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import java.io.IOException
import java.lang.reflect.Type

/**
 * `Content-Type`이 JSON이면 fastjson2로 디코딩하고, 아니면 기본 Decoder로 위임하는 Feign Decoder입니다.
 */
class FeignFastjsonDecoder: feign.codec.Decoder {

    companion object: KLogging() {
        private val fallbackDecoder by lazy { feign.codec.Decoder.Default() }

        val INSTANCE: FeignFastjsonDecoder by lazy { FeignFastjsonDecoder() }
    }

    /**
     * Feign 연동에서 `decode` 함수를 제공합니다.
     */
    inline fun <reified T: Any> decode(response: feign.Response): T? {
        return decode(response, T::class.java) as? T
    }

    /**
     * Feign 연동에서 `decode` 함수를 제공합니다.
     */
    override fun decode(response: feign.Response, type: Type): Any? = when {
        response.isJsonBody() -> jsonDecode(response, type)
        else -> fallback(response, type)
    }

    private fun jsonDecode(response: feign.Response, type: Type): Any? {
        if (response.status() == 204 || response.status() == 404) {
            return feign.Util.emptyValueOf(type)
        }
        val responseBody = response.body() ?: run {
            return null
        }
        responseBody.asInputStream().use { inputStream ->
            try {
                log.trace { "Read json format response body. target type=$type" }
                return JSON.parseObject(inputStream, type)
            } catch (e: JSONException) {
                log.error(e) { "Fail to read json format response body. type=$type" }

                if (e.cause is IOException) {
                    throw e.cause as IOException
                }
                throw DecodeException(
                    response.status(),
                    "$type is not a type supported by FeignFastjsonDecoder decoder.",
                    response.request(),
                    e
                )
            } catch (e: Throwable) {
                log.error(e) { "Fail to decode response body. type=$type" }
                throw DecodeException(
                    response.status(),
                    "$type is not a type supported by FeignFastjsonDecoder decoder.",
                    response.request(),
                    e
                )
            }
        }
    }

    private fun fallback(response: feign.Response, type: Type): Any? {
        log.debug { "Fallback to Default Decoder for response: ${response.status()} with type: $type, content-type=${response.headers()["content-type"]}" }
        return fallbackDecoder.decode(response, type)
    }
}
