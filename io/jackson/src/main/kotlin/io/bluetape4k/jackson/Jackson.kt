package io.bluetape4k.jackson

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import java.io.IOException

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
     * 타입 정보를 제공하는 Jackson ObjectMapper
     */
    val typedJsonMapper: JsonMapper by lazy { createDefaultJsonMapper(needTypeInfo = true) }

    val prettyTypedJsonWriter: ObjectWriter by lazy { typedJsonMapper.writerWithDefaultPrettyPrinter() }

    /**
     * 기본 Jackson JsonMapper를 생성합니다.
     *
     * classpath 에 있는 Jackson Module을 찾아서 추가하고, 일반적으로 많이 사용하는 직렬화/역직렬화 특성을 설정합니다.
     *
     * @param needTypeInfo
     * @return
     */
    fun createDefaultJsonMapper(needTypeInfo: Boolean = false): JsonMapper {
        log.info { "Create JsonMapper instance ... needTypeInfo=$needTypeInfo" }

        return jsonMapper {
            // Classpath에 있는 모든 Jackson용 Module을 찾아서 추가합니다.
            findAndAddModules()

            // 리소스에 services 로 등록해줘서 이제 자동으로 등록됩니다.
            // addModules(JsonUuidModule())

            addModule(
                kotlinModule {
                    enable(KotlinFeature.NullIsSameAsDefault)
                    enable(KotlinFeature.NullToEmptyCollection)
                    enable(KotlinFeature.NullToEmptyMap)
                }
            )

            // Serialization feature
            enable(
                JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT,
                JsonGenerator.Feature.IGNORE_UNKNOWN,
                JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN,
            )
            disable(
                SerializationFeature.FAIL_ON_EMPTY_BEANS
            )

            // Deserialization feature
            enable(
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
                DeserializationFeature.READ_ENUMS_USING_TO_STRING,
            )
            disable(
                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            )
            enable(
                JsonReadFeature.ALLOW_TRAILING_COMMA
            )

        }.apply {
            if (needTypeInfo) {
                activateDefaultTyping(
                    BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Any::class.java)
                        .allowIfSubTypeIsArray()
                        .build(),
                    ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS
                )
                initTypeInclusion(this)
            }
        }
    }

    private fun initTypeInclusion(mapper: JsonMapper) {
        val mapTypers = StdTypeResolverBuilder().apply {
            init(JsonTypeInfo.Id.CLASS, null)
            inclusion(JsonTypeInfo.As.PROPERTY)
        }
        mapper.setDefaultTyping(mapTypers)

        try {
            val s = mapper.writeValueAsBytes(1)
            mapper.readValue(s, Any::class.java)
        } catch (e: IOException) {
            throw IllegalStateException("JsonMapper에 타입정보 추가에 실패했습니다", e)
        }
    }
}
