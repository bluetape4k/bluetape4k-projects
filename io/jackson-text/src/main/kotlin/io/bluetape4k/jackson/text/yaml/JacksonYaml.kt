package io.bluetape4k.jackson.text.yaml

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import io.bluetape4k.jackson.Jackson

/**
 * Jackson의 [YAMLMapper], [YAMLFactory]의 기본 객체를 제공하는 object 입니다.
 *
 * ```
 * val yamlMapper = JacksonYaml.defaultYamlMapper
 *
 * val yaml = """
 *    |name: healingpaper
 *    |age: 30
 *    |job: developer
 *    """.trimMargin()
 *
 * val map = yamlMapper.readValue<Map<String, Any>>(yaml)
 * ```
 */
object JacksonYaml {

    val defaultYamlMapper: YAMLMapper by lazy {
        YAMLMapper.builder()
            .findAndAddModules()
            .enable(
                JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT,
                JsonGenerator.Feature.IGNORE_UNKNOWN,
                JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN,
            )
            .disable(
                SerializationFeature.FAIL_ON_EMPTY_BEANS
            )

            // Deserialization feature
            .enable(
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
                DeserializationFeature.READ_ENUMS_USING_TO_STRING,
            )
            .disable(
                DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
            )
            .build()
    }

    val defaultYamlFactory: YAMLFactory by lazy { YAMLFactory() }

    val defaultObjectMapper: ObjectMapper by lazy { Jackson.defaultJsonMapper }
}
