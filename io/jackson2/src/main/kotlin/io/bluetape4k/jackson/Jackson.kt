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
import io.bluetape4k.jackson.Jackson.defaultJsonMapper
import io.bluetape4k.jackson.Jackson.typedJsonMapper
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import java.io.IOException

/**
 * Bluetape4k 기본 Jackson 매퍼 구성을 제공하는 싱글턴입니다.
 *
 * ## 동작/계약
 * - [defaultJsonMapper], [typedJsonMapper]는 lazy 초기화 후 동일 인스턴스를 재사용합니다.
 * - `typed` 계열은 default typing 정보를 포함해 다형 타입 직렬화를 지원합니다.
 * - 매퍼 생성 중 I/O/설정 오류가 발생하면 [IllegalStateException]으로 전파됩니다.
 *
 * ```kotlin
 * val mapper = Jackson.defaultJsonMapper
 * val typedMapper = Jackson.typedJsonMapper
 * // mapper !== typedMapper
 * ```
 */
object Jackson: KLogging() {

    /** 기본 JsonMapper 인스턴스입니다. */
    val defaultJsonMapper: JsonMapper by lazy { createDefaultJsonMapper() }

    /** [defaultJsonMapper] 기반 pretty-print [ObjectWriter]입니다. */
    val prettyJsonWriter: ObjectWriter by lazy { defaultJsonMapper.writerWithDefaultPrettyPrinter() }

    /**
     * 타입 정보를 포함하는 JsonMapper 인스턴스입니다.
     *
     * @deprecated 보안 취약점(allowIfBaseType(Any::class.java))이 있습니다.
     * [createTypedJsonMapper]("com.example.") 를 사용하세요.
     */
    @Deprecated(
        "allowIfBaseType(Any::class.java)는 모든 타입을 허용하여 RCE 취약점을 야기할 수 있습니다. " +
            "createTypedJsonMapper(\"com.example.\") 를 사용하세요.",
        ReplaceWith("Jackson.createTypedJsonMapper()")
    )
    val typedJsonMapper: JsonMapper by lazy { createDefaultJsonMapper(needTypeInfo = true) }

    /**
     * 타입 정보를 포함하며 포맷된 JSON을 출력하는 [ObjectWriter]
     *
     * @deprecated [typedJsonMapper]와 함께 deprecated됩니다.
     * [createTypedJsonMapper](...).writerWithDefaultPrettyPrinter() 를 사용하세요.
     */
    @Deprecated(
        "typedJsonMapper와 함께 deprecated됩니다. " +
            "createTypedJsonMapper(...).writerWithDefaultPrettyPrinter() 를 사용하세요.",
        ReplaceWith("Jackson.createTypedJsonMapper().writerWithDefaultPrettyPrinter()")
    )
    val prettyTypedJsonWriter: ObjectWriter by lazy {
        createDefaultJsonMapper(needTypeInfo = true).writerWithDefaultPrettyPrinter()
    }

    /**
     * 사용자가 지정한 패키지만 허용하는 다형 타입 지원 [JsonMapper]를 생성합니다.
     *
     * ## 보안 주의
     * - [allowedBasePackages]에 신뢰할 수 있는 패키지만 지정하세요.
     * - 빈 목록은 허용되지 않으며 [IllegalArgumentException]을 발생시킵니다.
     *
     * ```kotlin
     * val mapper = Jackson.createTypedJsonMapper("com.example.", "io.myapp.")
     * // 지정된 패키지의 타입만 다형 역직렬화 허용
     * ```
     *
     * @param allowedBasePackages 허용할 기본 타입 패키지 접두사 목록 (예: `"com.example."`)
     * @param typing 다형 타입 처리 전략 (기본값: [ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS])
     */
    fun createTypedJsonMapper(
        vararg allowedBasePackages: String,
        typing: ObjectMapper.DefaultTyping = ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS,
    ): JsonMapper {
        require(allowedBasePackages.isNotEmpty()) {
            "보안상 허용할 패키지를 하나 이상 지정해야 합니다. 예: createTypedJsonMapper(\"com.example.\")"
        }
        log.info { "Create TypedJsonMapper ... allowedBasePackages=${allowedBasePackages.toList()}" }
        val validator = BasicPolymorphicTypeValidator.builder().apply {
            allowedBasePackages.forEach { allowIfBaseType(it) }
            allowIfSubTypeIsArray()
        }.build()
        return createDefaultJsonMapper().apply {
            activateDefaultTyping(validator, typing)
            initTypeInclusion(this)
        }
    }

    /**
     * 기본 Jackson JsonMapper를 생성합니다.
     *
     * ## 동작/계약
     * - classpath의 Jackson 모듈을 자동 등록합니다.
     * - Kotlin null/collection 관련 기능과 직렬화·역직렬화 feature를 기본 활성화합니다.
     * - [needTypeInfo]가 `true`이면 default typing을 활성화하고 검증 직렬화/역직렬화를 수행합니다.
     *
     * ```kotlin
     * val mapper = Jackson.createDefaultJsonMapper(needTypeInfo = true)
     * // mapper !== Jackson.defaultJsonMapper
     * ```
     * @param needTypeInfo 타입 정보 포함 여부
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
                // 보안 경고: allowIfBaseType(Any::class.java)는 모든 클래스를 기본 타입으로 허용합니다.
                // 신뢰할 수 없는 JSON 데이터를 역직렬화할 경우 임의 코드 실행(RCE) 취약점이 발생할 수 있습니다.
                // (CVE-2019-12384 계열 취약점 참고)
                // 보안이 중요한 환경에서는 allowIfBaseType() 에 신뢰할 패키지만 명시적으로 지정하세요.
                // 예: .allowIfBaseType("com.example.model")
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
