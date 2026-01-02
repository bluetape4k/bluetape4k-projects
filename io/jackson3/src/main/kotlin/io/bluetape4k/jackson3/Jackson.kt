package io.bluetape4k.jackson3

import io.bluetape4k.jackson3.crypto.JsonEncryptModule
import io.bluetape4k.jackson3.mask.JsonMaskerModule
import io.bluetape4k.jackson3.uuid.JsonUuidModule
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import tools.jackson.core.json.JsonReadFeature
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectWriter
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule

/**
 * Jackson Json Library 가 제공하는 [JsonMapper] 를 제공합니다.
 *
 * @constructor Create empty Jackson
 */
object Jackson: KLogging() {

    /**
     * 기본 Jackson ObjectMapper
     */
    val defaultJsonMapper: JsonMapper by lazy { createDefaultJsonMapper() }

    /**
     * 포맷된 JSON을 출력하는 Jackson [ObjectWriter]
     */
    val prettyJsonWriter: ObjectWriter by lazy { defaultJsonMapper.writerWithDefaultPrettyPrinter() }

    /**
     * 기본 Jackson JsonMapper를 생성합니다.
     *
     * classpath 에 있는 Jackson Module을 찾아서 추가하고, 일반적으로 많이 사용하는 직렬화/역직렬화 특성을 설정합니다.
     *
     * @param needTypeInfo
     * @return
     */
    fun createDefaultJsonMapper(): JsonMapper {
        log.info { "Create Jackson3 JsonMapper instance ..." }

        return jsonMapper {
            // Classpath에 있는 모든 Jackson용 Module을 찾아서 추가합니다.
            findAndAddModules()

            // 내부의 Module은 직접 등록합니다. (findAndRegisterModules() 에서 등록해주지 않는다)
            // 리소스에 services 로 등록해줘서 이제 자동으로 등록됩니다.
            addModules(JsonEncryptModule())
            addModules(JsonMaskerModule())
            addModules(JsonUuidModule())

            addModule(
                kotlinModule {
                    enable(KotlinFeature.NullIsSameAsDefault)
                    enable(KotlinFeature.NullToEmptyCollection)
                    enable(KotlinFeature.NullToEmptyMap)
                }
            )

            // Serialization feature
            enable(
                SerializationFeature.CLOSE_CLOSEABLE,
                SerializationFeature.FLUSH_AFTER_WRITE_VALUE,
            )
            disable(
                SerializationFeature.FAIL_ON_EMPTY_BEANS
            )

            // Deserialization feature
            enable(
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
            )
            disable(
                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            )
            enable(
                JsonReadFeature.ALLOW_TRAILING_COMMA
            )

        }
    }
}
