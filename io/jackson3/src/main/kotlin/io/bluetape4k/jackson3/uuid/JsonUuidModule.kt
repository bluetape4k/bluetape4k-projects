package io.bluetape4k.jackson3.uuid

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import tools.jackson.databind.module.SimpleModule

/**
 * UUID Base62 인코딩/디코딩 지원을 Jackson 3 매퍼에 등록하는 모듈입니다.
 *
 * ## 동작/계약
 * - [setupModule]에서 [JsonUuidEncoderAnnotationIntrospector]를 등록합니다.
 * - 모듈 등록 후 [JsonUuidEncoder] 필드에 Base62/Plain 전략이 적용됩니다.
 *
 * ```kotlin
 * val mapper = Jackson.defaultJsonMapper.copy().registerModule(JsonUuidModule())
 * // @JsonUuidEncoder 정책이 적용됨
 * ```
 *
 * @see [JsonUuidEncoder]
 */
class JsonUuidModule: SimpleModule() {

    companion object: KLogging()

    /** UUID 인코딩 인트로스펙터를 컨텍스트에 등록합니다. */
    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonUuidModule ..." }
        context.insertAnnotationIntrospector(JsonUuidEncoderAnnotationIntrospector())
    }
}
