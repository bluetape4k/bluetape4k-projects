package io.bluetape4k.jackson.uuid

import com.fasterxml.jackson.databind.module.SimpleModule
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import java.util.*

/**
 * UUID Base62 인코딩/디코딩 지원을 Jackson 매퍼에 등록하는 모듈입니다.
 *
 * ## 동작/계약
 * - UUID serializer/deserializer를 모듈 초기화 시점에 등록합니다.
 * - [setupModule]에서 [JsonUuidEncoderAnnotationInterospector]를 삽입합니다.
 * - 모듈 등록 후 [JsonUuidEncoder]가 UUID 필드에 대해 적용됩니다.
 *
 * ```kotlin
 * val mapper = Jackson.defaultJsonMapper.copy().registerModule(JsonUuidModule())
 * // @JsonUuidEncoder 필드가 Base62/Plain 정책으로 직렬화됨
 * ```
 *
 * @see [JsonUuidEncoder]
 */
class JsonUuidModule: SimpleModule() {

    companion object: KLogging()

    private val interospector = JsonUuidEncoderAnnotationInterospector()

    init {
        // log.debug { "Add JsonUuidBase62Serializer, JsonUuidBase62Deserializer ..." }

        addSerializer(UUID::class.java, JsonUuidBase62Serializer())
        addDeserializer(UUID::class.java, JsonUuidBase62Deserializer())
    }

    /** UUID 인코딩 애너테이션 인트로스펙터를 컨텍스트에 등록합니다. */
    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonUuidModule ..." }
        context.insertAnnotationIntrospector(interospector)
    }
}
