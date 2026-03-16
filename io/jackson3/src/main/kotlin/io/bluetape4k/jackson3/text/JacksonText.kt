package io.bluetape4k.jackson3.text

import io.bluetape4k.jackson3.Jackson
import io.bluetape4k.logging.KLogging
import io.bluetape4k.jackson3.text.JacksonText.Csv.defaultFactory
import io.bluetape4k.jackson3.text.JacksonText.Csv.defaultMapper
import io.bluetape4k.jackson3.text.JacksonText.Csv.defaultSerializer
import io.bluetape4k.jackson3.text.JacksonText.Props.defaultFactory
import io.bluetape4k.jackson3.text.JacksonText.Props.defaultMapper
import io.bluetape4k.jackson3.text.JacksonText.Props.defaultSerializer
import io.bluetape4k.jackson3.text.JacksonText.Toml.defaultFactory
import io.bluetape4k.jackson3.text.JacksonText.Toml.defaultMapper
import io.bluetape4k.jackson3.text.JacksonText.Toml.defaultSerializer
import io.bluetape4k.jackson3.text.JacksonText.Yaml.defaultFactory
import io.bluetape4k.jackson3.text.JacksonText.Yaml.defaultMapper
import io.bluetape4k.jackson3.text.JacksonText.Yaml.defaultSerializer
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
 * Jackson 3 텍스트 포맷(CSV/Properties/TOML/YAML) 기본 mapper/factory/serializer를 제공합니다.
 *
 * ## 동작/계약
 * - 포맷별 `defaultMapper`, `defaultFactory`, `defaultSerializer`는 lazy singleton입니다.
 * - 공통 serialization/deserialization feature 집합을 각 mapper에 적용합니다.
 * - 공개 프로퍼티는 불변 레퍼런스입니다.
 *
 * ```kotlin
 * val serializer = JacksonText.Yaml.defaultSerializer
 * val text = serializer.serializeAsString(mapOf("name" to "debop"))
 * // text.contains("name") == true
 * ```
 */
object JacksonText: KLogging() {

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
     * CSV 포맷 기본 구성 요소를 제공합니다.
     *
     * ## 동작/계약
     * - [defaultMapper]는 공통 feature 집합을 적용해 생성됩니다.
     * - [defaultFactory], [defaultSerializer]는 lazy singleton입니다.
     *
     * ```kotlin
     * val serializer = JacksonText.Csv.defaultSerializer
     * // serializer.serializeAsString(mapOf("id" to 1)).isNotBlank() == true
     * ```
     */
    object Csv {
        /**
         * CSV 기본 [CsvMapper]입니다.
         *
         * ## 동작/계약
         * - Kotlin 모듈 자동 등록 후 공통 feature를 적용합니다.
         * - lazy singleton으로 1회 생성됩니다.
         *
         * ```kotlin
         * val mapper = JacksonText.Csv.defaultMapper
         * // mapper != null
         * ```
         */
        val defaultMapper: CsvMapper by lazy {
            CsvMapper.builder()
                .findAndAddModules()
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        /**
         * CSV 기본 [CsvFactory]입니다.
         *
         * ## 동작/계약
         * - lazy singleton으로 생성됩니다.
         *
         * ```kotlin
         * val factory = JacksonText.Csv.defaultFactory
         * // factory != null
         * ```
         */
        val defaultFactory: CsvFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * 기본 JSON mapper 참조입니다.
         *
         * ## 동작/계약
         * - [Jackson.defaultJsonMapper]를 그대로 노출합니다.
         *
         * ```kotlin
         * val same = JacksonText.Csv.defaultJsonMapper === Jackson.defaultJsonMapper
         * // same == true
         * ```
         */
        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        /**
         * CSV 기본 직렬화기입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]를 주입한 [CsvJacksonSerializer] singleton을 제공합니다.
         *
         * ```kotlin
         * val serializer = JacksonText.Csv.defaultSerializer
         * // serializer.serializeAsString(mapOf("id" to 1)).isNotBlank() == true
         * ```
         */
        val defaultSerializer: CsvJacksonSerializer by lazy {
            CsvJacksonSerializer(defaultMapper)
        }
    }

    /**
     * Java Properties 포맷 기본 구성 요소를 제공합니다.
     *
     * ## 동작/계약
     * - [defaultMapper]는 공통 feature 집합을 적용해 생성됩니다.
     * - [defaultFactory], [defaultSerializer]는 lazy singleton입니다.
     *
     * ```kotlin
     * val serializer = JacksonText.Props.defaultSerializer
     * // serializer.serializeAsString(mapOf("app.name" to "demo")).contains("app.name") == true
     * ```
     */
    object Props {
        /**
         * Java Properties 기본 [JavaPropsMapper]입니다.
         *
         * ## 동작/계약
         * - Kotlin 모듈 자동 등록 후 공통 feature를 적용합니다.
         * - lazy singleton으로 1회 생성됩니다.
         *
         * ```kotlin
         * val mapper = JacksonText.Props.defaultMapper
         * // mapper != null
         * ```
         */
        val defaultMapper: JavaPropsMapper by lazy {
            JavaPropsMapper.builder()
                .findAndAddModules()
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        /**
         * Java Properties 기본 [JavaPropsFactory]입니다.
         *
         * ## 동작/계약
         * - lazy singleton으로 생성됩니다.
         *
         * ```kotlin
         * val factory = JacksonText.Props.defaultFactory
         * // factory != null
         * ```
         */
        val defaultFactory: JavaPropsFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * 기본 JSON mapper 참조입니다.
         *
         * ## 동작/계약
         * - [Jackson.defaultJsonMapper]를 그대로 노출합니다.
         *
         * ```kotlin
         * val same = JacksonText.Props.defaultJsonMapper === Jackson.defaultJsonMapper
         * // same == true
         * ```
         */
        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        /**
         * Java Properties 기본 직렬화기입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]를 주입한 [PropsJacksonSerializer] singleton을 제공합니다.
         *
         * ```kotlin
         * val serializer = JacksonText.Props.defaultSerializer
         * // serializer.serializeAsString(mapOf("a.b" to "v")).contains("a.b") == true
         * ```
         */
        val defaultSerializer: PropsJacksonSerializer by lazy {
            PropsJacksonSerializer(defaultMapper)
        }
    }

    /**
     * TOML 포맷 기본 구성 요소를 제공합니다.
     *
     * ## 동작/계약
     * - [defaultMapper]는 공통 feature 집합을 적용해 생성됩니다.
     * - [defaultFactory], [defaultSerializer]는 lazy singleton입니다.
     *
     * ```kotlin
     * val serializer = JacksonText.Toml.defaultSerializer
     * // serializer.serializeAsString(mapOf("id" to 1)).contains("id") == true
     * ```
     */
    object Toml {
        /**
         * TOML 기본 [TomlMapper]입니다.
         *
         * ## 동작/계약
         * - Kotlin 모듈 자동 등록 후 공통 feature를 적용합니다.
         * - lazy singleton으로 1회 생성됩니다.
         *
         * ```kotlin
         * val mapper = JacksonText.Toml.defaultMapper
         * // mapper != null
         * ```
         */
        val defaultMapper: TomlMapper by lazy {
            TomlMapper.builder()
                .findAndAddModules()
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        /**
         * TOML 기본 [TomlFactory]입니다.
         *
         * ## 동작/계약
         * - lazy singleton으로 생성됩니다.
         *
         * ```kotlin
         * val factory = JacksonText.Toml.defaultFactory
         * // factory != null
         * ```
         */
        val defaultFactory: TomlFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * 기본 JSON mapper 참조입니다.
         *
         * ## 동작/계약
         * - [Jackson.defaultJsonMapper]를 그대로 노출합니다.
         *
         * ```kotlin
         * val same = JacksonText.Toml.defaultJsonMapper === Jackson.defaultJsonMapper
         * // same == true
         * ```
         */
        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        /**
         * TOML 기본 직렬화기입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]를 주입한 [TomlJacksonSerializer] singleton을 제공합니다.
         *
         * ```kotlin
         * val serializer = JacksonText.Toml.defaultSerializer
         * // serializer.serializeAsString(mapOf("id" to 1)).contains("id") == true
         * ```
         */
        val defaultSerializer: TomlJacksonSerializer by lazy {
            TomlJacksonSerializer(defaultMapper)
        }
    }

    /**
     * YAML 포맷 기본 구성 요소를 제공합니다.
     *
     * ## 동작/계약
     * - [defaultMapper]는 공통 feature 집합을 적용해 생성됩니다.
     * - [defaultFactory], [defaultSerializer]는 lazy singleton입니다.
     *
     * ```kotlin
     * val serializer = JacksonText.Yaml.defaultSerializer
     * // serializer.serializeAsString(mapOf("name" to "debop")).contains("name") == true
     * ```
     */
    object Yaml {
        /**
         * YAML 기본 [YAMLMapper]입니다.
         *
         * ## 동작/계약
         * - Kotlin 모듈 자동 등록 후 공통 feature를 적용합니다.
         * - lazy singleton으로 1회 생성됩니다.
         *
         * ```kotlin
         * val mapper = JacksonText.Yaml.defaultMapper
         * // mapper != null
         * ```
         */
        val defaultMapper: YAMLMapper by lazy {
            YAMLMapper.builder()
                .findAndAddModules()
                .enable(*enabledSerializationFeatures)
                .disable(*disabledSerializationFeatures)
                .enable(*enabledDeserializationFeatures)
                .disable(*disabledDeserializationFeatures)
                .build()
        }

        /**
         * YAML 기본 [YAMLFactory]입니다.
         *
         * ## 동작/계약
         * - lazy singleton으로 생성됩니다.
         *
         * ```kotlin
         * val factory = JacksonText.Yaml.defaultFactory
         * // factory != null
         * ```
         */
        val defaultFactory: YAMLFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * 기본 JSON mapper 참조입니다.
         *
         * ## 동작/계약
         * - [Jackson.defaultJsonMapper]를 그대로 노출합니다.
         *
         * ```kotlin
         * val same = JacksonText.Yaml.defaultJsonMapper === Jackson.defaultJsonMapper
         * // same == true
         * ```
         */
        val defaultJsonMapper: JsonMapper by lazy { Jackson.defaultJsonMapper }

        /**
         * YAML 기본 직렬화기입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]를 주입한 [YamlJacksonSerializer] singleton을 제공합니다.
         *
         * ```kotlin
         * val serializer = JacksonText.Yaml.defaultSerializer
         * // serializer.serializeAsString(mapOf("name" to "debop")).contains("name") == true
         * ```
         */
        val defaultSerializer: YamlJacksonSerializer by lazy {
            YamlJacksonSerializer(defaultMapper)
        }
    }
}
