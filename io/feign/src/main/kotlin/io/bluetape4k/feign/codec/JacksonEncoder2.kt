package io.bluetape4k.feign.codec

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.json.JsonMapper
import feign.RequestTemplate
import feign.codec.EncodeException
import feign.codec.Encoder
import io.bluetape4k.jackson.Jackson
import io.bluetape4k.logging.KLogging
import java.lang.reflect.Type

/**
 * Jackson 기반 JSON 요청 본문 인코더입니다.
 *
 * ## 동작/계약
 * - [bodyType]을 Jackson JavaType으로 변환해 JSON 바이트로 직렬화합니다.
 * - 결과 바이트를 UTF-8로 [RequestTemplate.body]에 설정합니다.
 * - 직렬화 실패 시 [EncodeException]을 던집니다.
 *
 * ```kotlin
 * val encoder = JacksonEncoder2()
 * // encoder.encode(obj, type, template)로 본문 설정
 * ```
 */
class JacksonEncoder2 private constructor(
    private val mapper: JsonMapper,
): Encoder {

    companion object: KLogging() {
        val INSTANCE: JacksonEncoder2 by lazy { invoke() }

        /**
         * [JacksonEncoder2] 인스턴스를 생성합니다.
         *
         * ## 동작/계약
         * - [mapper] 기본값은 [Jackson.defaultJsonMapper]입니다.
         *
         * ```kotlin
         * val encoder = JacksonEncoder2()
         * // encoder != null
         * ```
         */
        @JvmStatic
        operator fun invoke(mapper: JsonMapper = Jackson.defaultJsonMapper): JacksonEncoder2 {
            return JacksonEncoder2(mapper)
        }
    }

    /**
     * Feign 연동에서 `encode` 함수를 제공합니다.
     *
     * ```kotlin
     * val encoder = JacksonEncoder2()
     * val template = RequestTemplate()
     * encoder.encode(mapOf("key" to "value"), Map::class.java, template)
     * // template.body() != null
     * ```
     */
    override fun encode(obj: Any?, bodyType: Type, template: RequestTemplate) {
        try {
            val javaType = mapper.typeFactory.constructType(bodyType)
            template.body(mapper.writerFor(javaType).writeValueAsBytes(obj), Charsets.UTF_8)
        } catch (e: JsonProcessingException) {
            throw EncodeException(e.message, e)
        }
    }
}
