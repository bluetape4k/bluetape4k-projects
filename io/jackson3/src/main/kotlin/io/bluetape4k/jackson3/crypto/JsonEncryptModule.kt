package io.bluetape4k.jackson3.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import tools.jackson.databind.module.SimpleModule

/**
 * [JsonEncrypt] 애너테이션 기반 암복호화 처리를 Jackson 3 매퍼에 등록하는 모듈입니다.
 *
 * ## 동작/계약
 * - [setupModule]에서 [JsonEncryptAnnotationIntrospector]를 등록합니다.
 * - 모듈 등록 후 [JsonEncrypt] 필드에 암복호화 serializer/deserializer 선택이 적용됩니다.
 *
 * ```kotlin
 * val mapper = Jackson.defaultJsonMapper.copy().registerModule(JsonEncryptModule())
 * // @JsonEncrypt 필드가 암복호화 규칙으로 처리됨
 * ```
 *
 * @see JsonEncrypt
 * @see JsonEncryptSerializer
 * @see JsonEncryptDeserializer
 * @see JsonEncryptAnnotationIntrospector
 */
class JsonEncryptModule: SimpleModule() {

    companion object: KLogging()

    /** 암복호화 인트로스펙터를 컨텍스트에 등록합니다. */
    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonEncryptModule ..." }
        context.insertAnnotationIntrospector(JsonEncryptAnnotationIntrospector())
    }
}
