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
 * JSON 응답은 fastjson2로, 그 외 응답은 기본 Feign decoder로 처리하는 Decoder입니다.
 *
 * ## 동작/계약
 * - `Content-Type`이 JSON 계열이면 fastjson2 경로를 사용합니다.
 * - JSON이 아니면 [feign.codec.Decoder.Default]로 위임합니다.
 * - 상태 코드 `204/404`는 `Util.emptyValueOf(type)`를 반환합니다.
 *
 * ```kotlin
 * val decoder = FeignFastjsonDecoder()
 * // decoder.decode(response, MyType::class.java) 결과는 content-type에 따라 분기됨
 * ```
 */
class FeignFastjsonDecoder: feign.codec.Decoder {

    companion object: KLogging() {
        private val fallbackDecoder by lazy { feign.codec.Decoder.Default() }

        val INSTANCE: FeignFastjsonDecoder by lazy { FeignFastjsonDecoder() }
    }

    /**
     * 응답을 reified 타입으로 디코딩합니다.
     *
     * ## 동작/계약
     * - 내부 [decode] 오버로드에 위임합니다.
     * - 캐스팅 실패 시 `null`을 반환합니다.
     *
     * ```kotlin
     * val value: MyType? = decoder.decode(response)
     * // value == null || value is MyType
     * ```
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
