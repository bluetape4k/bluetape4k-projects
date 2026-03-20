package io.bluetape4k.spring.jackson

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.bluetape4k.jackson.uuid.JsonUuidModule
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*

/**
 * Spring Boot [Jackson2ObjectMapperBuilder] 기본 설정을 적용하는 커스터마이저를 생성합니다.
 *
 * ## 동작/계약
 * - Service Loader 기반 모듈 탐색을 활성화하고 Kotlin/UUID 모듈을 추가합니다.
 * - 직렬화/역직렬화 feature를 코드에 정의된 값으로 활성/비활성화한 뒤 [builder]를 실행합니다.
 *
 * ```kotlin
 * val customizer = jacksonObjectMapperBuilderCustomizer {
 *     timeZone(TimeZone.getTimeZone("Asia/Seoul"))
 * }
 * ```
 */
inline fun jacksonObjectMapperBuilderCustomizer(
    crossinline builder: Jackson2ObjectMapperBuilder.() -> Unit,
): Jackson2ObjectMapperBuilderCustomizer =
    Jackson2ObjectMapperBuilderCustomizer { jacksonBuilder ->

        // Classpath에 있는 모든 Jackson용 Module을 찾아서 추가합니다.
        jacksonBuilder.findModulesViaServiceLoader(true)

        ZoneOffset.getAvailableZoneIds()
        jacksonBuilder.timeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Seoul")))

        jacksonBuilder.modules(
            KotlinModule
                .Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.SingletonSupport, true)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )

        // 내부의 Module은 직접 등록합니다.
        jacksonBuilder.modules(JsonUuidModule())

        // Serialization feature
        jacksonBuilder.serializationInclusion(JsonInclude.Include.NON_NULL)
        jacksonBuilder.featuresToEnable(
            JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT,
            JsonGenerator.Feature.IGNORE_UNKNOWN,
            JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN
        )

        // Serialization feature
        jacksonBuilder.featuresToDisable(
            SerializationFeature.FAIL_ON_EMPTY_BEANS
        )

        // Deserialization feature
        jacksonBuilder.featuresToEnable(
            DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
            DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
            DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
            DeserializationFeature.READ_ENUMS_USING_TO_STRING
        )
        jacksonBuilder.featuresToDisable(
            DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
            DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
        )

        jacksonBuilder.builder()
    }
