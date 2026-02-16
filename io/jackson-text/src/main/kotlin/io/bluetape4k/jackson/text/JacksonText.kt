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

/**
 * Jackson 2.x 텍스트 데이터 포맷(CSV, Properties, TOML, YAML)을 위한 Mapper와 Serializer를 제공하는 싱글턴 오브젝트입니다.
 */
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

    /**
     * CSV(Comma-Separated Values) 형식의 데이터 직렬화/역직렬화를 위한 기본 [CsvMapper], [CsvFactory], [CsvJacksonSerializer]를 제공합니다.
     */
    object Csv {
        /**
         * CSV 데이터 처리를 위한 기본 [CsvMapper] 인스턴스입니다.
         */
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

        /**
         * CSV 데이터 처리를 위한 기본 [CsvFactory] 인스턴스입니다.
         */
        val defaultFactory: CsvFactory by lazy { CsvFactory() }

        /**
         * JSON 데이터 처리를 위한 기본 [JsonMapper] 인스턴스입니다.
         */
        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        /**
         * CSV 데이터 직렬화/역직렬화를 위한 기본 [CsvJacksonSerializer] 인스턴스입니다.
         */
        val defaultSerializer: CsvJacksonSerializer by lazy {
            CsvJacksonSerializer(defaultMapper)
        }
    }

    /**
     * Java Properties 형식의 데이터 직렬화/역직렬화를 위한 기본 [JavaPropsMapper], [JavaPropsFactory], [PropsJacksonSerializer]를 제공합니다.
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
        /**
         * Java Properties 데이터 처리를 위한 기본 [JavaPropsMapper] 인스턴스입니다.
         */
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

        /**
         * Java Properties 데이터 처리를 위한 기본 [JavaPropsFactory] 인스턴스입니다.
         */
        val defaultFactory: JavaPropsFactory by lazy { JavaPropsFactory() }

        /**
         * JSON 데이터 처리를 위한 기본 [JsonMapper] 인스턴스입니다.
         */
        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        /**
         * Java Properties 데이터 직렬화/역직렬화를 위한 기본 [PropsJacksonSerializer] 인스턴스입니다.
         */
        val defaultSerializer: PropsJacksonSerializer by lazy {
            PropsJacksonSerializer(defaultMapper)
        }
    }

    /**
     * TOML(Tom's Obvious, Minimal Language) 형식의 데이터 직렬화/역직렬화를 위한 기본 [TomlMapper], [TomlFactory], [TomlJacksonSerializer]를 제공합니다.
     */
    object Toml {
        /**
         * TOML 데이터 처리를 위한 기본 [TomlMapper] 인스턴스입니다.
         */
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

        /**
         * TOML 데이터 처리를 위한 기본 [TomlFactory] 인스턴스입니다.
         */
        val defaultFactory: TomlFactory by lazy { TomlFactory() }

        /**
         * JSON 데이터 처리를 위한 기본 [JsonMapper] 인스턴스입니다.
         */
        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        /**
         * TOML 데이터 직렬화/역직렬화를 위한 기본 [TomlJacksonSerializer] 인스턴스입니다.
         */
        val defaultSerializer: TomlJacksonSerializer by lazy {
            TomlJacksonSerializer(defaultMapper)
        }
    }

    /**
     * YAML(YAML Ain't Markup Language) 형식의 데이터 직렬화/역직렬화를 위한 기본 [YAMLMapper], [YAMLFactory], [YamlJacksonSerializer]를 제공합니다.
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
        /**
         * YAML 데이터 처리를 위한 기본 [YAMLMapper] 인스턴스입니다.
         */
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

        /**
         * YAML 데이터 처리를 위한 기본 [YAMLFactory] 인스턴스입니다.
         */
        val defaultFactory: YAMLFactory by lazy { YAMLFactory() }

        /**
         * JSON 데이터 처리를 위한 기본 [JsonMapper] 인스턴스입니다.
         */
        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        /**
         * YAML 데이터 직렬화/역직렬화를 위한 기본 [YamlJacksonSerializer] 인스턴스입니다.
         */
        val defaultSerializer: YamlJacksonSerializer by lazy {
            YamlJacksonSerializer(defaultMapper)
        }
    }
}
