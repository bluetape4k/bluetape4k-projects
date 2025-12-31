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
 * Jackson Binary JSON Serializer 관련 Serializer, Factory, Mapper 를 제공합니다.
 *
 * ```
 * val serializer = JacksonBinary.CBOR.defaultJsonSerializer
 * val bytes = serializer.serialize(obj)
 * val obj = serializer.deserialize(bytes, type)
 * // or
 * val obj = serializer.deserialize<ObjectType>(bytes)
 * ```
 */
object JacksonBinary: KLogging() {

    private val enabledSerializationFeatures = setOf(
//        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
//        SerializationFeature.WRITE_ENUMS_USING_TO_STRING,
//        SerializationFeature.WRITE_ENUMS_USING_INDEX,
//        SerializationFeature.WRITE_NULL_MAP_VALUES,
        SerializationFeature.WRITE_EMPTY_JSON_ARRAYS
    )
    private val disabledSerializationFeatures = setOf(
        SerializationFeature.FAIL_ON_EMPTY_BEANS,
//        SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN
    )

    private val enabledDeserializationFeatures = setOf(
        DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
        DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
//        DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
//        DeserializationFeature.READ_ENUMS_USING_TO_STRING
    )
    private val disabledDeserializationFeatures = setOf(
        DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
        DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
        DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES
    )

    /**
     * CBOR 알고리즘을 사용하는 Jackson Binary JSON Serializer 관련 Serializer, Factory, Mapper 를 제공합니다.
     */
    object CBOR {
        /**
         * CBOR 알고리즘을 사용하는 [CBORMapper] 인스턴스
         */
        val defaultMapper: CBORMapper by lazy {
            CBORMapper.builder()
                .findAndAddModules()
                .enable(
                    CBORWriteFeature.WRITE_TYPE_HEADER,
                    CBORWriteFeature.STRINGREF
                )
                .enable(*enabledSerializationFeatures.toTypedArray())
                .disable(*disabledSerializationFeatures.toTypedArray())
                .enable(*enabledDeserializationFeatures.toTypedArray())
                .disable(*disabledDeserializationFeatures.toTypedArray())
                .build()
        }

        /**
         * CBOR 알고리즘을 사용하는 [CBORFactory] 인스턴스
         */
        val defaultFactory: CBORFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * CBOR 알고리즘을 사용하는 Jackson Binary JSON Serializer
         */
        val defaultJsonSerializer: CborJsonSerializer by lazy {
            CborJsonSerializer(defaultMapper)
        }
    }

    /**
     * ION 알고리즘을 사용하는 Jackson Binary JSON Serializer 관련 Serializer, Factory, Mapper 를 제공합니다.
     */
    object ION {

        /**
         * ION 알고리즘을 사용하는 [IonObjectMapper] 인스턴스
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
         * ION 알고리즘을 사용하는 [IonFactory] 인스턴스
         */
        val defaultFactory: IonFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * ION 알고리즘을 사용하는 Jackson Binary JSON Serializer
         */
        val defaultJsonSerializer: IonJsonSerializer by lazy {
            IonJsonSerializer(defaultMapper)
        }
    }

    /**
     * Smile 알고리즘을 사용하는 Jackson Binary JSON Serializer 관련 Serializer, Factory, Mapper 를 제공합니다.
     */
    object Smile {

        /**
         * Smile 알고리즘을 사용하는 [SmileMapper] 인스턴스
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
         * Smile 알고리즘을 사용하는 [SmileFactory] 인스턴스
         */
        val defaultFactory: SmileFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * Smile 알고리즘을 사용하는 Jackson Binary JSON Serializer
         */
        val defaultJsonSerializer: SmileJsonSerializer by lazy {
            SmileJsonSerializer(defaultMapper)
        }
    }
}
