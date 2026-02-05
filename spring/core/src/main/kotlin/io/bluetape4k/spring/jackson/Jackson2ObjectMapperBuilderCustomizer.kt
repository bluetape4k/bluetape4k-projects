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
 * Spring Boot 에서 재공하는 [com.fasterxml.jackson.databind.ObjectMapper] 를 설정할 때, Customizing 을 수행할 수 있도록 해줍니다.
 *
 * - 기본적으로 classpath 에 있는 module을 자동으로 등록해줍니다. (kotlin, jdk8, jsr310 등)
 * - `bluetape4k-jackson` 에서 제공하는 [io.bluetape4k.jackson.Jackson.defaultJsonMapper] 와 같은 설정을 제공합니다
 *
 * 추가적으로 [builder]를 통해 추가 설정을 할 수 있습니다.
 *
 * ```
 * @Configuration
 * class JsonConfiguration {
 *     @Bean
 *     fun jackson2ObjectMapperBuilderCustomizer(): Jackson2ObjectMapperBuilderCustomizer {
 *         return jackson2ObjectMapperBuilderCustomizer { builder: Jackson2ObjectMapperBuilder ->
 *              // additional setup for jackson ObjectMapper
 *              builder.timeZone(TimeZone.getDefault())
 *         }
 *     }
 * }
 * ```
 *
 * @param builder [Jackson2ObjectMapperBuilder] 를 이용하여 [io.bluetape4k.jackson.Jackson.defaultJsonMapper]의 설정을 추가합니다.
 * @receiver
 * @return [Jackson2ObjectMapperBuilderCustomizer] 인스턴스
 */
inline fun jackson2ObjectMapperBuilderCustomizer(
    @BuilderInference crossinline builder: Jackson2ObjectMapperBuilder.() -> Unit,
): Jackson2ObjectMapperBuilderCustomizer = Jackson2ObjectMapperBuilderCustomizer { builder ->

    // Classpath에 있는 모든 Jackson용 Module을 찾아서 추가합니다.
    builder.findModulesViaServiceLoader(true)

    ZoneOffset.getAvailableZoneIds()
    builder.timeZone(TimeZone.getTimeZone(ZoneId.of("Seoul/Asia")))

    builder.modules(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, true)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build(),
    )

    // 내부의 Module은 직접 등록합니다.
    builder.modules(JsonUuidModule())

    // Serialization feature
    builder.serializationInclusion(JsonInclude.Include.NON_NULL)
    builder.featuresToEnable(
        JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT,
        JsonGenerator.Feature.IGNORE_UNKNOWN,
        JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN,
    )

    // Serialization feature
    builder.featuresToDisable(
        SerializationFeature.FAIL_ON_EMPTY_BEANS
    )

    // Deserialization feature
    builder.featuresToEnable(
        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
        DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
        DeserializationFeature.READ_ENUMS_USING_TO_STRING,
    )
    builder.featuresToDisable(
        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
    )

    builder.builder()
}
