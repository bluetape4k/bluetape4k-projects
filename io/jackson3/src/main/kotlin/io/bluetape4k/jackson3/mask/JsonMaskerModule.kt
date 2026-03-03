package io.bluetape4k.jackson3.mask

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import tools.jackson.databind.module.SimpleModule

/**
 * [JsonMasker] 애너테이션 기반 마스킹 직렬화를 Jackson 3 매퍼에 등록하는 모듈입니다.
 *
 * ## 동작/계약
 * - [setupModule]에서 [JsonMaskerAnnotationIntrospector]를 등록합니다.
 * - 모듈 등록 후 [JsonMasker] 필드에 마스킹 serializer 선택이 적용됩니다.
 *
 * ```kotlin
 * val mapper = Jackson.defaultJsonMapper.copy().registerModule(JsonMaskerModule())
 * // @JsonMasker 필드가 마스킹 문자열로 직렬화됨
 * ```
 *
 * @see [JsonMasker]
 * @see [JsonMaskerSerializer]
 * @see [JsonMaskerAnnotationIntrospector]
 */
class JsonMaskerModule: SimpleModule() {

    companion object: KLogging()

    /** 마스킹 인트로스펙터를 컨텍스트에 등록합니다. */
    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonMaskerModule ..." }
        context.insertAnnotationIntrospector(JsonMaskerAnnotationIntrospector())
    }
}
