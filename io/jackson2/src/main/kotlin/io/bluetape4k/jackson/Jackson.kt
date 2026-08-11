package io.bluetape4k.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import java.io.IOException

/**
 * Bluetape4k 기본 Jackson 매퍼 구성을 제공하는 싱글턴입니다.
 *
 * ## 동작/계약
 * - [defaultJsonMapper] is lazily initialized and reused.
 * - Use [createTypedJsonMapper] for allowlisted default typing.
 * - [typedJsonMapper] and [prettyTypedJsonWriter] are legacy APIs and fail explicitly when accessed.
 * - 매퍼 생성 중 I/O/설정 오류가 발생하면 [IllegalStateException]으로 전파됩니다.
 *
 * ```kotlin
 * val mapper = Jackson.defaultJsonMapper
 * val typedMapper = Jackson.createTypedJsonMapper("com.example.model.")
 * ```
 */
object Jackson: KLogging() {

    private const val DEFAULT_TYPE_PROPERTY_NAME = "@class"
    private const val LEGACY_TYPED_MAPPER_MESSAGE =
        "Legacy polymorphic typing is disabled. Use Jackson.createTypedJsonMapper(\"trusted.package.\") with an explicit allowlist."

    /** 기본 JsonMapper 인스턴스입니다. */
    val defaultJsonMapper: JsonMapper by lazy { createDefaultJsonMapper() }

    /** [defaultJsonMapper] 기반 pretty-print [ObjectWriter]입니다. */
    val prettyJsonWriter: ObjectWriter by lazy { defaultJsonMapper.writerWithDefaultPrettyPrinter() }

    /**
     * 타입 정보를 포함하는 JsonMapper 인스턴스입니다.
     *
     * @deprecated legacy permissive typing 경로이므로 접근 시 [UnsupportedOperationException]이 발생합니다.
     * 신뢰된 패키지 allowlist를 사용하는 [createTypedJsonMapper]("com.example.") 를 사용하세요.
    */
    @Deprecated(
        "legacy permissive typing은 모든 타입을 허용하여 RCE 취약점을 야기할 수 있습니다. " +
                "createTypedJsonMapper(\"com.example.\") 를 사용하세요.",
        ReplaceWith("Jackson.createTypedJsonMapper(\"com.example.\")")
    )
    val typedJsonMapper: JsonMapper by lazy { createDefaultJsonMapper(needTypeInfo = true) }

    /**
     * 타입 정보를 포함하며 포맷된 JSON을 출력하는 [ObjectWriter]
     *
     * @deprecated legacy permissive typing 경로이므로 접근 시 [UnsupportedOperationException]이 발생합니다.
     * [createTypedJsonMapper](...).writerWithDefaultPrettyPrinter() 를 사용하세요.
     */
    @Deprecated(
        "typedJsonMapper와 함께 deprecated됩니다. " +
                "createTypedJsonMapper(...).writerWithDefaultPrettyPrinter() 를 사용하세요.",
        ReplaceWith("Jackson.createTypedJsonMapper(\"com.example.\").writerWithDefaultPrettyPrinter()")
    )
    val prettyTypedJsonWriter: ObjectWriter by lazy {
        createDefaultJsonMapper(needTypeInfo = true).writerWithDefaultPrettyPrinter()
    }

    /**
     * property 기반 타입 정보를 기록하고 신뢰된 타입만 허용하는 [JsonMapper]를 생성합니다
     * trusted subtype packages.
     *
     * ## Security contract
     * - [allowedBasePackages] must contain trusted subtype package prefixes.
     * - Empty allowlists are rejected with [IllegalArgumentException].
     * - Type ids are written as the `@class` property and validated during polymorphic deserialization.
     *
     * ```kotlin
     * val mapper = Jackson.createTypedJsonMapper("com.example.", "io.myapp.")
     * // Only subtypes under the supplied package prefixes are accepted.
     * ```
     *
     * @param allowedBasePackages Trusted subtype package prefixes, for example `"com.example."`.
     * @param typing Default typing strategy. Defaults to [ObjectMapper.DefaultTyping.NON_FINAL_AND_ENUMS].
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
            allowedBasePackages.forEach { allowIfSubType(it) }
            allowIfSubTypeIsArray()
        }.build()
        return createDefaultJsonMapper().apply {
            activateDefaultTypingAsProperty(validator, typing, DEFAULT_TYPE_PROPERTY_NAME)
            verifyTypeInclusion(this)
        }
    }

    /**
     * 기본 Jackson JsonMapper를 생성합니다.
     *
     * ## 동작/계약
     * - classpath의 Jackson 모듈을 자동 등록합니다.
     * - Kotlin null/collection 관련 기능과 직렬화·역직렬화 feature를 기본 활성화합니다.
     * - [needTypeInfo]가 `true`이면 legacy permissive typing을 차단하기 위해
     *   [UnsupportedOperationException]을 발생시킵니다. 명시적 allowlist가 필요하면
     *   [createTypedJsonMapper]를 사용하세요.
     *
     * ```kotlin
     * val mapper = Jackson.createTypedJsonMapper("com.example.")
     * // mapper !== Jackson.defaultJsonMapper
     * ```
     * @param needTypeInfo legacy source compatibility를 위한 옵션입니다. `true`는 migration failure입니다.
     */
    fun createDefaultJsonMapper(needTypeInfo: Boolean = false): JsonMapper {
        if (needTypeInfo) {
            throw UnsupportedOperationException(LEGACY_TYPED_MAPPER_MESSAGE)
        }
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

        }
    }

    private fun verifyTypeInclusion(mapper: JsonMapper) {
        try {
            val s = mapper.writeValueAsBytes(1)
            mapper.readValue(s, Any::class.java)
        } catch (e: IOException) {
            throw IllegalStateException("JsonMapper에 타입정보 추가에 실패했습니다", e)
        }
    }
}
