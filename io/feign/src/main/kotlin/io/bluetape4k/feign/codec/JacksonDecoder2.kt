package io.bluetape4k.feign.codec

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import feign.Response
import feign.Util
import feign.codec.DecodeException
import feign.codec.Decoder
import io.bluetape4k.feign.bodyAsReader
import io.bluetape4k.feign.isJsonBody
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import io.bluetape4k.logging.trace
import io.bluetape4k.support.closeSafe
import java.io.BufferedReader
import java.io.IOException
import java.io.Reader
import java.lang.reflect.Type

/**
 * JSON 응답은 Jackson으로, 그 외 응답은 기본 Feign decoder로 처리하는 Decoder입니다.
 *
 * ## 동작/계약
 * - `Content-Type`이 JSON 계열이면 Jackson 경로([jsonDecode])를 사용합니다.
 * - JSON이 아니면 [Decoder.Default]에 위임합니다.
 * - 상태 코드 `204/404`는 `Util.emptyValueOf(type)`를 반환합니다.
 *
 * ```kotlin
 * val decoder = JacksonDecoder2()
 * // decoder.decode(response, MyType::class.java) 결과는 content-type에 따라 분기됨
 * ```
 */
class JacksonDecoder2 private constructor(
    private val mapper: JsonMapper,
): Decoder {

    companion object: KLogging() {
        private val fallbackDecoder by lazy { Decoder.Default() }

        val INSTANCE: JacksonDecoder2 by lazy { invoke() }

        /**
         * [JacksonDecoder2] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - [mapper] 기본값은 [Jackson.defaultJsonMapper]입니다.
         *
         * ```kotlin
         * val decoder = JacksonDecoder2()
         * // decoder != null
         * ```
         */
        @JvmStatic
        operator fun invoke(mapper: JsonMapper = Jackson.defaultJsonMapper): JacksonDecoder2 {
            return JacksonDecoder2(mapper)
        }
    }

    /**
     * 응답을 reified 타입으로 디코딩합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [decode] 오버로드에 위임합니다.
     * - 캐스팅 실패 시 `null`을 반환합니다.
     *
     * ```kotlin
     * val value: MyType? = decoder.decode(response)
     * // value == null || value is MyType
     * ```
     */
    inline fun <reified T> decode(response: Response): T? {
        return decode(response, jacksonTypeRef<T>().type) as? T
    }

    /**
     * Feign 연동에서 `decode` 함수를 제공합니다.
     *
     * ```kotlin
     * val decoder = JacksonDecoder2()
     * // JSON content-type 응답 -> Jackson으로 디코딩
     * // non-JSON 응답 -> 기본 Feign decoder로 위임
     * ```
     */
    override fun decode(response: Response, type: Type): Any? = when {
        response.isJsonBody() -> jsonDecode(response, type)
        else                  -> fallback(response, type)
    }

    private fun jsonDecode(response: Response, type: Type): Any? {
        if (response.status() == 204 || response.status() == 404) {
            return Util.emptyValueOf(type)
        }
        if (response.body() == null) {
            return null
        }
        var reader: Reader = response.bodyAsReader()
        if (!reader.markSupported()) {
            reader = BufferedReader(reader, 1)
        }
        try {
            reader.mark(1)
            if (reader.read() == -1) {
                return null
            }
            reader.reset()
            log.trace { "Read json format response body. target type=$type" }
            return mapper.readValue(reader, mapper.constructType(type))
        } catch (e: JsonParseException) {
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
        } finally {
            reader.closeSafe()
        }
    }

    private fun fallback(response: Response, type: Type): Any? {
        log.debug {
            "Read non-json format response body by fallback decoder. " +
                    "type=$type, content-type=${response.headers()["content-type"]}"
        }
        return fallbackDecoder.decode(response, type)
    }
}
