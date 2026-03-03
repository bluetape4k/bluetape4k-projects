package io.bluetape4k.jackson.binary

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper
import com.fasterxml.jackson.dataformat.ion.IonFactory
import com.fasterxml.jackson.dataformat.ion.IonGenerator
import com.fasterxml.jackson.dataformat.ion.IonObjectMapper
import com.fasterxml.jackson.dataformat.smile.SmileFactory
import com.fasterxml.jackson.dataformat.smile.SmileGenerator
import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper
import io.bluetape4k.jackson.binary.JacksonBinary.CBOR.defaultFactory
import io.bluetape4k.jackson.binary.JacksonBinary.CBOR.defaultMapper
import io.bluetape4k.jackson.binary.JacksonBinary.CBOR.defaultSerializer
import io.bluetape4k.jackson.binary.JacksonBinary.ION.defaultFactory
import io.bluetape4k.jackson.binary.JacksonBinary.ION.defaultMapper
import io.bluetape4k.jackson.binary.JacksonBinary.ION.defaultSerializer
import io.bluetape4k.jackson.binary.JacksonBinary.Smile.defaultFactory
import io.bluetape4k.jackson.binary.JacksonBinary.Smile.defaultMapper
import io.bluetape4k.jackson.binary.JacksonBinary.Smile.defaultSerializer
import io.bluetape4k.logging.KLogging

/**
 * Jackson 바이너리 포맷(CBOR/ION/Smile) 기본 mapper/factory/serializer를 제공합니다.
 *
 * ## 동작/계약
 * - 각 포맷별 `defaultMapper`, `defaultFactory`, `defaultSerializer`는 lazy singleton으로 재사용됩니다.
 * - serialization/deserialization feature 집합을 공통으로 적용합니다.
 * - 공개 프로퍼티는 불변 레퍼런스이며 재설정하지 않습니다.
 *
 * ```kotlin
 * val serializer = JacksonBinary.CBOR.defaultSerializer
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 */
object JacksonBinary: KLogging() {

    private val enabledSerializationFeatures = setOf(
        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
        SerializationFeature.WRITE_ENUMS_USING_TO_STRING,
        SerializationFeature.WRITE_ENUMS_USING_INDEX,
        SerializationFeature.WRITE_NULL_MAP_VALUES,
        SerializationFeature.WRITE_EMPTY_JSON_ARRAYS
    )
    private val disabledSerializationFeatures = setOf(
        SerializationFeature.FAIL_ON_EMPTY_BEANS,
        SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN
    )

    private val enabledDeserializationFeatures = setOf(
        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
        DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
        DeserializationFeature.READ_ENUMS_USING_TO_STRING
    )
    private val disabledDeserializationFeatures = setOf(
        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
    )

    /**
     * CBOR 기본 구성 요소를 제공합니다.
     *
     * ## 동작/계약
     * - [defaultMapper]는 `WRITE_TYPE_HEADER`와 공통 feature 집합을 적용합니다.
     * - [defaultFactory]는 [defaultMapper.factory]를 그대로 노출합니다.
     * - [defaultSerializer]는 [defaultMapper]를 사용하는 singleton입니다.
     *
     * ```kotlin
     * val mapper = JacksonBinary.CBOR.defaultMapper
     * // mapper.factory == JacksonBinary.CBOR.defaultFactory
     * ```
     */
    object CBOR {
        /**
         * CBOR 기본 [CBORMapper]입니다.
         *
         * ## 동작/계약
         * - Kotlin 모듈 자동 등록과 타입 헤더 기록 옵션을 활성화합니다.
         * - 공통 serialization/deserialization feature 집합을 적용합니다.
         *
         * ```kotlin
         * val mapper = JacksonBinary.CBOR.defaultMapper
         * // mapper != null
         * ```
         */
        val defaultMapper: CBORMapper by lazy {
            CBORMapper.builder()
                .findAndAddModules()
                .enable(
                    CBORGenerator.Feature.WRITE_TYPE_HEADER,
                )
                .enable(*enabledSerializationFeatures.toTypedArray())
                .disable(*disabledSerializationFeatures.toTypedArray())
                .enable(*enabledDeserializationFeatures.toTypedArray())
                .disable(*disabledDeserializationFeatures.toTypedArray())
                .build()
        }

        /**
         * CBOR 기본 [CBORFactory]입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]의 factory 인스턴스를 재사용합니다.
         *
         * ```kotlin
         * val same = JacksonBinary.CBOR.defaultFactory === JacksonBinary.CBOR.defaultMapper.factory
         * // same == true
         * ```
         */
        val defaultFactory: CBORFactory by lazy { CBOR.defaultMapper.factory }

        /**
         * CBOR 기본 직렬화기입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]를 주입한 [CborJacksonSerializer] singleton을 제공합니다.
         *
         * ```kotlin
         * val serializer = JacksonBinary.CBOR.defaultSerializer
         * // serializer.serialize(mapOf("id" to 1)).isNotEmpty() == true
         * ```
         */
        val defaultSerializer: CborJacksonSerializer by lazy {
            CborJacksonSerializer(CBOR.defaultMapper)
        }
    }

    /**
     * Ion 기본 구성 요소를 제공합니다.
     *
     * ## 동작/계약
     * - [defaultMapper]는 `USE_NATIVE_TYPE_ID`와 공통 feature 집합을 적용합니다.
     * - [defaultFactory], [defaultSerializer]는 mapper 기반 singleton입니다.
     *
     * ```kotlin
     * val serializer = JacksonBinary.ION.defaultSerializer
     * // serializer.serialize(mapOf("id" to 1)).isNotEmpty() == true
     * ```
     */
    object ION {

        /**
         * Ion 기본 [IonObjectMapper]입니다.
         *
         * ## 동작/계약
         * - Kotlin 모듈 자동 등록과 native type id 사용 옵션을 활성화합니다.
         * - 공통 serialization/deserialization feature 집합을 적용합니다.
         *
         * ```kotlin
         * val mapper = JacksonBinary.ION.defaultMapper
         * // mapper != null
         * ```
         */
        val defaultMapper: IonObjectMapper by lazy {
            IonObjectMapper.builder()
                .findAndAddModules()
                .enable(
                    IonGenerator.Feature.USE_NATIVE_TYPE_ID,
                )
                .enable(*enabledSerializationFeatures.toTypedArray())
                .disable(*disabledSerializationFeatures.toTypedArray())
                .enable(*enabledDeserializationFeatures.toTypedArray())
                .disable(*disabledDeserializationFeatures.toTypedArray())
                .build()
        }

        /**
         * Ion 기본 [IonFactory]입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]의 factory 인스턴스를 재사용합니다.
         *
         * ```kotlin
         * val same = JacksonBinary.ION.defaultFactory === JacksonBinary.ION.defaultMapper.factory
         * // same == true
         * ```
         */
        val defaultFactory: IonFactory by lazy { ION.defaultMapper.factory }

        /**
         * Ion 기본 직렬화기입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]를 주입한 [IonJacksonSerializer] singleton을 제공합니다.
         *
         * ```kotlin
         * val serializer = JacksonBinary.ION.defaultSerializer
         * // serializer.serialize(mapOf("id" to 1)).isNotEmpty() == true
         * ```
         */
        val defaultSerializer: IonJacksonSerializer by lazy {
            IonJacksonSerializer(ION.defaultMapper)
        }
    }

    /**
     * Smile 기본 구성 요소를 제공합니다.
     *
     * ## 동작/계약
     * - [defaultMapper]는 헤더/종료 마커 기록 옵션과 공통 feature 집합을 적용합니다.
     * - [defaultFactory], [defaultSerializer]는 mapper 기반 singleton입니다.
     *
     * ```kotlin
     * val serializer = JacksonBinary.Smile.defaultSerializer
     * // serializer.serialize(mapOf("id" to 1)).isNotEmpty() == true
     * ```
     */
    object Smile {

        /**
         * Smile 기본 [SmileMapper]입니다.
         *
         * ## 동작/계약
         * - Kotlin 모듈 자동 등록과 헤더/종료 마커 기록 옵션을 활성화합니다.
         * - 공통 serialization/deserialization feature 집합을 적용합니다.
         *
         * ```kotlin
         * val mapper = JacksonBinary.Smile.defaultMapper
         * // mapper != null
         * ```
         */
        val defaultMapper: SmileMapper by lazy {
            SmileMapper.builder()
                .findAndAddModules()
                .enable(
                    SmileGenerator.Feature.WRITE_HEADER,
                    SmileGenerator.Feature.WRITE_END_MARKER,
                )
                .enable(*enabledSerializationFeatures.toTypedArray())
                .disable(*disabledSerializationFeatures.toTypedArray())
                .enable(*enabledDeserializationFeatures.toTypedArray())
                .disable(*disabledDeserializationFeatures.toTypedArray())
                .build()
        }

        /**
         * Smile 기본 [SmileFactory]입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]의 factory 인스턴스를 재사용합니다.
         *
         * ```kotlin
         * val same = JacksonBinary.Smile.defaultFactory === JacksonBinary.Smile.defaultMapper.factory
         * // same == true
         * ```
         */
        val defaultFactory: SmileFactory by lazy { Smile.defaultMapper.factory }

        /**
         * Smile 기본 직렬화기입니다.
         *
         * ## 동작/계약
         * - [defaultMapper]를 주입한 [SmileJacksonSerializer] singleton을 제공합니다.
         *
         * ```kotlin
         * val serializer = JacksonBinary.Smile.defaultSerializer
         * // serializer.serialize(mapOf("id" to 1)).isNotEmpty() == true
         * ```
         */
        val defaultSerializer: SmileJacksonSerializer by lazy {
            SmileJacksonSerializer(Smile.defaultMapper)
        }
    }
}
