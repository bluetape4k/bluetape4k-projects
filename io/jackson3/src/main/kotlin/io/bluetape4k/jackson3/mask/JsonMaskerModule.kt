package io.bluetape4k.jackson3.mask

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import tools.jackson.databind.module.SimpleModule

/**
 * 값을 masking 하는 [JsonMaskerSerializer] 를 사용할 수 있도록 하는 모듈입니다.
 *
 * @see [JsonMasker]
 * @see [JsonMaskerSerializer]
 * @see [JsonMaskerAnnotationInterospector]
 */
class JsonMaskerModule: SimpleModule() {

    companion object: KLogging()

    init {
        log.debug { "Add Jackson 3 JsonMaskerSerializer ..." }
        addSerializer(Any::class.java, JsonMaskerSerializer())
    }

    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonMaskerModule ..." }
        context.insertAnnotationIntrospector(JsonMaskerAnnotationInterospector())
    }
}
