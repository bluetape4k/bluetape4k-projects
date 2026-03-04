package io.bluetape4k.jackson3.crypto

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import tools.jackson.databind.module.SimpleModule

/**
 * [JsonTinkEncrypt] 애너테이션 기반 Tink 암복호화 처리를 Jackson 3 매퍼에 등록하는 모듈입니다.
 *
 * ## 동작/계약
 * - [setupModule]에서 [JsonTinkEncryptAnnotationIntrospector]를 등록합니다.
 * - 모듈 등록 후 [JsonTinkEncrypt] 필드에 Tink 암복호화 serializer/deserializer 선택이 적용됩니다.
 *
 * ```kotlin
 * val mapper = Jackson.createDefaultJsonMapper().rebuild()
 *     .addModule(JsonTinkEncryptModule())
 *     .build()
 * // @JsonTinkEncrypt 필드가 Tink 암복호화 규칙으로 처리됨
 * ```
 *
 * @see JsonTinkEncrypt
 * @see JsonTinkEncryptSerializer
 * @see JsonTinkEncryptDeserializer
 * @see JsonTinkEncryptAnnotationIntrospector
 */
class JsonTinkEncryptModule: SimpleModule() {

    companion object: KLogging()

    /** Tink 암복호화 인트로스펙터를 컨텍스트에 등록합니다. */
    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonTinkEncryptModule ..." }
        context.insertAnnotationIntrospector(JsonTinkEncryptAnnotationIntrospector())
    }
}
