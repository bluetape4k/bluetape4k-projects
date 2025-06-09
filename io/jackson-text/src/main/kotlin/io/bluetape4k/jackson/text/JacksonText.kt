package io.bluetape4k.jackson.text

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.dataformat.csv.CsvFactory
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsFactory
import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.toml.TomlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import io.bluetape4k.jackson.Jackson

object JacksonText {

    private val enabledJsonGeneratorFeatures = arrayOf(
        JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT,
        JsonGenerator.Feature.IGNORE_UNKNOWN,
        JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN
    )

    private val enabledSerializationFeatures = arrayOf(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
        SerializationFeature.WRITE_ENUMS_USING_TO_STRING,
        SerializationFeature.WRITE_NULL_MAP_VALUES,
        SerializationFeature.WRITE_EMPTY_JSON_ARRAYS
    )

    private val disabledSerializationFeatures = arrayOf(
        SerializationFeature.FAIL_ON_EMPTY_BEANS,
    )

    private val enabledDeserializationFeatures = arrayOf(
        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
        DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
        DeserializationFeature.READ_ENUMS_USING_TO_STRING
    )

    private val disabledDeserializationFeatures = arrayOf(
        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
    )

    object Csv {
        val defaultMapper: CsvMapper by lazy {
            CsvMapper.builder()
                .findAndAddModules()
                .enable(*enabledJsonGeneratorFeatures)
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        val defaultFactory: CsvFactory by lazy { CsvFactory() }

        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
    }

    /**
     * Jackson의 [JavaPropsMapper], [JavaPropsFactory]의 기본 객체를 제공하는 object 입니다.
     *
     * ```
     * val propMapper = JacksonText.Props.defaultMapper
     *
     * class MapWrapper {
     *     var map: MutableMap<String, String> = mutableMapOf()
     * }
     *
     * @Test
     * fun `map with branch`() {
     *     val props =
     *         """
     *         |map=first
     *         |map.b = second
     *         |map.xyz = third
     *         """.trimMargin()
     *     val wrapper = propsMapper.readValue<MapWrapper>(props)
     *     ...
     * }
     * ```
     */
    object Props {
        val defaultMapper: JavaPropsMapper by lazy {
            JavaPropsMapper.builder()
                .findAndAddModules()
                .enable(*enabledJsonGeneratorFeatures)
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        val defaultFactory: JavaPropsFactory by lazy { JavaPropsFactory() }

        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
    }

    object Toml {
        val defaultMapper: TomlMapper by lazy {
            TomlMapper.builder()
                .findAndAddModules()
                .enable(*enabledJsonGeneratorFeatures)
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        val defaultFactory: TomlFactory by lazy { TomlFactory() }

        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
    }

    /**
     * Jackson의 [YAMLMapper], [YAMLFactory]의 기본 객체를 제공하는 object 입니다.
     *
     * ```
     * val yamlMapper = JacksonText.Yaml.defaultMapper
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
    object Yaml {
        val defaultMapper: YAMLMapper by lazy {
            YAMLMapper.builder()
                .findAndAddModules()
                .enable(*enabledJsonGeneratorFeatures)
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        val defaultFactory: YAMLFactory by lazy { YAMLFactory() }

        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }
    }
}
