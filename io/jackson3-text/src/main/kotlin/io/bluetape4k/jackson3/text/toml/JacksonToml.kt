package io.bluetape4k.jackson3.text.toml

import io.bluetape4k.jackson3.Jackson
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.toml.TomlFactory
import tools.jackson.dataformat.toml.TomlMapper

@Deprecated(
    message = "Use JacksonText.Toml instead",
    replaceWith = ReplaceWith("JacksonText.Toml")
)
object JacksonToml {

    val defaultTomlMapper: TomlMapper by lazy {
        TomlMapper.builder()
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

    val defaultTomlFactory: TomlFactory by lazy { TomlFactory() }

    val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
}
