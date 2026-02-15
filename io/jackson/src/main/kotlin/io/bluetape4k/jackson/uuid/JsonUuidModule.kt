package io.bluetape4k.jackson.uuid

import com.fasterxml.jackson.databind.module.SimpleModule
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import java.util.*

/**
 * UUID 수형을 Base62 로 인코딩/디코딩 하는 [JsonUuidEncoder] 를 사용할 수 있도록 하는 Module 입니다.
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

    /** [JsonUuidEncoderAnnotationInterospector]를 Jackson 모듈에 등록합니다. */
    override fun setupModule(context: SetupContext) {
        log.info { "Setup JsonUuidModule ..." }
        context.insertAnnotationIntrospector(interospector)
    }
}
