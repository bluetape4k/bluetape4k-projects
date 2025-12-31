package io.bluetape4k.jackson3.text.csv

import io.bluetape4k.jackson3.Jackson
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.csv.CsvFactory
import tools.jackson.dataformat.csv.CsvMapper

@Deprecated(
    message = "Use JacksonText.Csv instead",
    replaceWith = ReplaceWith("JacksonText.Csv")
)
object JacksonCsv {

    val defaultCsvMapper: CsvMapper by lazy {
        CsvMapper.builder()
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

    val defaultCsvFactory: CsvFactory by lazy { CsvFactory() }

    val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
}
