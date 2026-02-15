package io.bluetape4k.jackson3.uuid

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import tools.jackson.databind.module.SimpleModule

/**
 * UUID 수형을 Base62 로 인코딩/디코딩 하는 [JsonUuidEncoder] 를 사용할 수 있도록 하는 Module 입니다.
 *
 * @see [JsonUuidEncoder]
 */
class JsonUuidModule: SimpleModule() {

    companion object: KLogging()

    /**
     * [JsonUuidEncoderAnnotationInterospector]를 Jackson 3.x 모듈에 등록합니다.
     */
    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonUuidModule ..." }
        context.insertAnnotationIntrospector(JsonUuidEncoderAnnotationInterospector())
    }
}
