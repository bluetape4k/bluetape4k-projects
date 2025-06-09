package io.bluetape4k.jackson.text.toml

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import io.bluetape4k.jackson.Jackson

@Deprecated(
    message = "Use JacksonText.Toml instead",
    replaceWith = ReplaceWith("JacksonText.Toml")
)
object JacksonToml {

    val defaultTomlMapper: TomlMapper by lazy {
        TomlMapper.builder()
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

    val defaultTomlFactory: TomlFactory by lazy { TomlFactory() }

    val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
}
