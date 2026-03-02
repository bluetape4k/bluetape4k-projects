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
 * Jackson 3 기본 매퍼 구성을 제공하는 싱글턴입니다.
 *
 * ## 동작/계약
 * - [defaultJsonMapper]와 [prettyJsonWriter]는 lazy 초기화 후 동일 인스턴스를 재사용합니다.
 * - 모듈 자동 등록과 Kotlin feature 설정을 포함한 기본 JsonMapper를 제공합니다.
 * - 매퍼 생성/설정 실패 시 예외가 호출자에게 전파됩니다.
 *
 * ```kotlin
 * val mapper = Jackson.defaultJsonMapper
 * val prettyJson = Jackson.prettyJsonWriter.writeValueAsString(data)
 * // prettyJson.contains(\"\\n\") == true
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
     * ## 동작/계약
     * - classpath 모듈 자동 등록 후 직렬화/역직렬화 feature를 설정합니다.
     * - 호출할 때마다 새 [JsonMapper] 인스턴스를 생성합니다.
     *
     * ```kotlin
     * val mapper = Jackson.createDefaultJsonMapper()
     * // mapper !== Jackson.defaultJsonMapper
     * ```
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
