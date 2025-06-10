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
 * `Content-Type` 에 따라 `application/json` 이 아닌 경우에는 `text/plain` 방식으로 decode 해주는 Feign Decoder 입니다.
 */
class FeignFastjsonDecoder: feign.codec.Decoder {

    companion object: KLogging() {
        private val fallbackDecoder by lazy { feign.codec.Decoder.Default() }

        val INSTANCE: FeignFastjsonDecoder by lazy { FeignFastjsonDecoder() }
    }

    inline fun <reified T: Any> decode(response: feign.Response): T? {
        return decode(response, T::class.java) as? T
    }

    override fun decode(response: feign.Response, type: Type): Any? = when {
        response.isJsonBody() -> runCatching { jsonDecode(response, type) }.getOrElse { fallback(response, type) }
        else -> fallback(response, type)
    }

    private fun jsonDecode(response: feign.Response, type: Type): Any? {
        if (response.status() in listOf(204, 404)) {
            return feign.Util.emptyValueOf(type)
        }
        if (response.body() == null) {
            return null
        }
        response.body().asInputStream().use { inputStream ->
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
                    "$type is not a type supported by JacksonDecoder2 decoder.",
                    response.request(),
                    e
                )
            } catch (e: Throwable) {
                log.error(e) { "Fail to decode response body. type=$type" }
                throw DecodeException(
                    response.status(),
                    "$type is not a type supported by JacksonDecoder2 decoder.",
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
