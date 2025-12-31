package io.bluetape4k.jackson3.text.yaml

import io.bluetape4k.jackson3.Jackson
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.SerializationFeature
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.dataformat.yaml.YAMLMapper

/**
 * Jackson의 [YAMLMapper], [YAMLFactory]의 기본 객체를 제공하는 object 입니다.
 *
 * ```
 * val yamlMapper = JacksonYaml.defaultYamlMapper
 *
 * val yaml = """
 *    |name: debop
 *    |age: 30
 *    |job: developer
 *    """.trimMargin()
 *
 * val map = yamlMapper.readValue<Map<String, Any>>(yaml)
 * ```
 */
@Deprecated(
    message = "Use `JacksonText.Yaml` instead.",
    replaceWith = ReplaceWith("JacksonText.Yaml")
)
object JacksonYaml {

    val defaultYamlMapper: YAMLMapper by lazy {
        YAMLMapper.builder()
            .findAndAddModules()
            .disable(
                SerializationFeature.FAIL_ON_EMPTY_BEANS
            )

            // Deserialization feature
            .enable(
                DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
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
