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
import io.bluetape4k.logging.KLogging

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
                    CBORGenerator.Feature.WRITE_TYPE_HEADER,
                    CBORGenerator.Feature.STRINGREF,
                )
                .disable(
                    SerializationFeature.FAIL_ON_EMPTY_BEANS
                )
                .enable(
                    DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                    DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                    DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                    DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
                    DeserializationFeature.READ_ENUMS_USING_TO_STRING,
                )
                .disable(
                    DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                )
                .build()
        }

        /**
         * CBOR 알고리즘을 사용하는 [CBORFactory] 인스턴스
         */
        val defaultFactory: CBORFactory by lazy { CBOR.defaultMapper.factory }

        /**
         * CBOR 알고리즘을 사용하는 Jackson Binary JSON Serializer
         */
        val defaultJsonSerializer: CborJsonSerializer by lazy {
            CborJsonSerializer(CBOR.defaultMapper)
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
                    IonGenerator.Feature.USE_NATIVE_TYPE_ID,
                )
                .disable(
                    SerializationFeature.FAIL_ON_EMPTY_BEANS
                )
                .enable(
                    DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                    DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                    DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                    DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
                    DeserializationFeature.READ_ENUMS_USING_TO_STRING,
                )
                .disable(
                    DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                )
                .build()
        }

        /**
         * ION 알고리즘을 사용하는 [IonFactory] 인스턴스
         */
        val defaultFactory: IonFactory by lazy { ION.defaultMapper.factory }

        /**
         * ION 알고리즘을 사용하는 Jackson Binary JSON Serializer
         */
        val defaultJsonSerializer: IonJsonSerializer by lazy {
            IonJsonSerializer(ION.defaultMapper)
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
                    SmileGenerator.Feature.WRITE_HEADER,
                    SmileGenerator.Feature.WRITE_END_MARKER,
                )
                .disable(
                    SerializationFeature.FAIL_ON_EMPTY_BEANS
                )
                .enable(
                    DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                    DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY,
                    DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT,
                    DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
                    DeserializationFeature.READ_ENUMS_USING_TO_STRING,
                )
                .disable(
                    DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES,
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES,
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                )
                .build()
        }

        /**
         * Smile 알고리즘을 사용하는 [SmileFactory] 인스턴스
         */
        val defaultFactory: SmileFactory by lazy { Smile.defaultMapper.factory }

        /**
         * Smile 알고리즘을 사용하는 Jackson Binary JSON Serializer
         */
        val defaultJsonSerializer: SmileJsonSerializer by lazy {
            SmileJsonSerializer(Smile.defaultMapper)
        }
    }
}
