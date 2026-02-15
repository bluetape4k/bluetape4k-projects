package io.bluetape4k.jackson3

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
 * Jackson 3.x JSON 라이브러리의 기본 [JsonMapper] 인스턴스와 설정을 제공하는 싱글턴 객체입니다.
 *
 * Kotlin 모듈 등 일반적으로 사용되는 설정이 미리 구성된
 * [JsonMapper] 인스턴스를 지연 생성(lazy) 방식으로 제공합니다.
 *
 * ### 사용 예시
 *
 * ```kotlin
 * val mapper = Jackson.defaultJsonMapper
 * val json = mapper.writeValueAsString(data)
 *
 * val prettyJson = Jackson.prettyJsonWriter.writeValueAsString(data)
 * ```
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
     * 기본 Jackson 3.x [JsonMapper]를 생성합니다.
     *
     * 클래스패스에 있는 Jackson 모듈을 자동으로 찾아 등록하고,
     * 일반적으로 많이 사용하는 직렬화/역직렬화 특성을 설정합니다.
     *
     * @return 설정이 완료된 [JsonMapper] 인스턴스
     */
    fun createDefaultJsonMapper(): JsonMapper {
        log.info { "Create Jackson3 JsonMapper instance ..." }

        return jsonMapper {
            // Classpath에 있는 모든 Jackson용 Module을 찾아서 추가합니다.
            findAndAddModules()

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
