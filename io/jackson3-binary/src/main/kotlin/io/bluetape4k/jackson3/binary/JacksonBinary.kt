package io.bluetape4k.jackson3.binary

import io.bluetape4k.logging.KLogging
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.SerializationFeature
import tools.jackson.dataformat.cbor.CBORFactory
import tools.jackson.dataformat.cbor.CBORMapper
import tools.jackson.dataformat.cbor.CBORWriteFeature
import tools.jackson.dataformat.ion.IonFactory
import tools.jackson.dataformat.ion.IonObjectMapper
import tools.jackson.dataformat.ion.IonWriteFeature
import tools.jackson.dataformat.smile.SmileFactory
import tools.jackson.dataformat.smile.SmileMapper
import tools.jackson.dataformat.smile.SmileWriteFeature

/**
 * Jackson 3 바이너리 포맷(CBOR/ION/Smile) 기본 mapper/factory/serializer를 제공합니다.
 *
 * ## 동작/계약
 * - 포맷별 `defaultMapper`, `defaultFactory`, `defaultSerializer`는 lazy singleton으로 재사용됩니다.
 * - 공통 serialization/deserialization feature 집합을 각 mapper에 적용합니다.
 * - 공개 프로퍼티는 불변 레퍼런스입니다.
 *
 * ```kotlin
 * val serializer = JacksonBinary.CBOR.defaultSerializer
 * val bytes = serializer.serialize(mapOf("id" to 1))
 * // bytes.isNotEmpty() == true
 * ```
 */
object JacksonBinary: KLogging() {

    private val enabledSerializationFeatures = setOf(
        SerializationFeature.WRITE_EMPTY_JSON_ARRAYS
    )
    private val disabledSerializationFeatures = setOf(
        SerializationFeature.FAIL_ON_EMPTY_BEANS,
    )

    private val enabledDeserializationFeatures = setOf(
        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
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
     * - [defaultFactory]는 [defaultMapper.tokenStreamFactory] 결과를 재사용합니다.
     * - [defaultSerializer]는 [defaultMapper]를 주입한 singleton입니다.
     *
     * ```kotlin
     * val serializer = JacksonBinary.CBOR.defaultSerializer
     * // serializer.serialize(mapOf("id" to 1)).isNotEmpty() == true
     * ```
     */
    object CBOR {
        /**
         * CBOR 기본 [CBORMapper]입니다.
         *
         * ## 동작/계약
         * - Kotlin 모듈 자동 등록 및 타입 헤더 기록 옵션을 활성화합니다.
         * - 공통 feature 집합을 적용합니다.
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
                    CBORWriteFeature.WRITE_TYPE_HEADER,
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
         * - [defaultMapper.tokenStreamFactory]를 그대로 노출합니다.
         *
         * ```kotlin
         * val same = JacksonBinary.CBOR.defaultFactory === JacksonBinary.CBOR.defaultMapper.tokenStreamFactory()
         * // same == true
         * ```
         */
        val defaultFactory: CBORFactory by lazy { defaultMapper.tokenStreamFactory() }

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
            CborJacksonSerializer(defaultMapper)
        }
    }

    /**
     * Ion 기본 구성 요소를 제공합니다.
     *
     * ## 동작/계약
     * - [defaultMapper]는 `USE_NATIVE_TYPE_ID`와 공통 feature를 적용합니다.
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
         * - Kotlin 모듈 자동 등록 및 native type id 옵션을 활성화합니다.
         * - 공통 feature 집합을 적용합니다.
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
                    IonWriteFeature.USE_NATIVE_TYPE_ID,
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
         * - [defaultMapper.tokenStreamFactory]를 그대로 노출합니다.
         *
         * ```kotlin
         * val same = JacksonBinary.ION.defaultFactory === JacksonBinary.ION.defaultMapper.tokenStreamFactory()
         * // same == true
         * ```
         */
        val defaultFactory: IonFactory by lazy { defaultMapper.tokenStreamFactory() }

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
            IonJacksonSerializer(defaultMapper)
        }
    }

    /**
     * Smile 기본 구성 요소를 제공합니다.
     *
     * ## 동작/계약
     * - [defaultMapper]는 헤더/종료 마커 기록 옵션과 공통 feature를 적용합니다.
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
         * - Kotlin 모듈 자동 등록과 헤더/종료 마커 옵션을 적용합니다.
         * - 공통 feature 집합을 적용합니다.
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
                    SmileWriteFeature.WRITE_HEADER,
                    SmileWriteFeature.WRITE_END_MARKER,
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
         * - [defaultMapper.tokenStreamFactory]를 그대로 노출합니다.
         *
         * ```kotlin
         * val same = JacksonBinary.Smile.defaultFactory === JacksonBinary.Smile.defaultMapper.tokenStreamFactory()
         * // same == true
         * ```
         */
        val defaultFactory: SmileFactory by lazy { defaultMapper.tokenStreamFactory() }

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
            SmileJacksonSerializer(defaultMapper)
        }
    }
}
