package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.Jackson
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.dataformat.csv.CsvFactory
import tools.jackson.dataformat.csv.CsvMapper
import tools.jackson.dataformat.javaprop.JavaPropsFactory
import tools.jackson.dataformat.javaprop.JavaPropsMapper
import tools.jackson.dataformat.toml.TomlFactory
import tools.jackson.dataformat.toml.TomlMapper
import tools.jackson.dataformat.yaml.YAMLFactory
import tools.jackson.dataformat.yaml.YAMLMapper

/**
 * Jackson JSON 처리에서 사용하는 `JacksonText` 타입입니다.
 */
object JacksonText {

    private val enabledSerializationFeatures = arrayOf(
        SerializationFeature.WRITE_EMPTY_JSON_ARRAYS
    )

    private val disabledSerializationFeatures = arrayOf(
        SerializationFeature.FAIL_ON_EMPTY_BEANS,
    )

    private val enabledDeserializationFeatures = arrayOf(
        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
    )

    private val disabledDeserializationFeatures = arrayOf(
        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
    )

    /**
     * Jackson JSON 처리에서 사용하는 `Csv` 타입입니다.
     */
    object Csv {
        val defaultMapper: CsvMapper by lazy {
            CsvMapper.builder()
                .findAndAddModules()
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        val defaultFactory: CsvFactory by lazy { CsvFactory() }

        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        val defaultSerializer: CsvJacksonSerializer by lazy {
            CsvJacksonSerializer(defaultMapper)
        }
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
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        val defaultFactory: JavaPropsFactory by lazy { JavaPropsFactory() }

        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        val defaultSerializer: PropsJacksonSerializer by lazy {
            PropsJacksonSerializer(defaultMapper)
        }
    }

    /**
     * Jackson JSON 처리에서 사용하는 `Toml` 타입입니다.
     */
    object Toml {
        val defaultMapper: TomlMapper by lazy {
            TomlMapper.builder()
                .findAndAddModules()
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        val defaultFactory: TomlFactory by lazy { TomlFactory() }

        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        val defaultSerializer: TomlJacksonSerializer by lazy {
            TomlJacksonSerializer(defaultMapper)
        }
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
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        val defaultFactory: YAMLFactory by lazy { YAMLFactory() }

        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        val defaultSerializer: YamlJacksonSerializer by lazy {
            YamlJacksonSerializer(defaultMapper)
        }
    }
}
