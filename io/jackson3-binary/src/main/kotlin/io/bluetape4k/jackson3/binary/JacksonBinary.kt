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
 * Jackson 3.x 바이너리 데이터 포맷(CBOR, ION, Smile)을 위한 Mapper와 Serializer를 제공하는 싱글턴 오브젝트입니다.
 *
 * 바이너리 포맷은 JSON 대비 크기가 작고 파싱 속도가 빠르며, 네트워크 전송 및 스토리지 효율이 우수합니다.
 *
 * ```
 * val serializer = JacksonBinary.CBOR.defaultSerializer
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
     * CBOR(Concise Binary Object Representation)는 JSON과 호환되는 바이너리 데이터 포맷으로,
     * JSON 대비 크기가 작고 파싱 속도가 빠릅니다.
     *
     * CBOR는 RFC 8949에 정의된 표준 포맷으로, IoT 디바이스와 네트워크 프로토콜에서 널리 사용됩니다.
     */
    object CBOR {
        /**
         * 기본 구성된 CBOR 전용 [CBORMapper] 인스턴스입니다.
         *
         * Kotlin 모듈 자동 감지가 활성화되어 있으며, 타입 헤더 기록([CBORWriteFeature.WRITE_TYPE_HEADER])을 지원합니다.
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
         * 기본 구성된 CBOR 전용 [CBORFactory] 인스턴스입니다.
         */
        val defaultFactory: CBORFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * 기본 구성된 CBOR 바이너리 포맷용 Jackson Serializer입니다.
         */
        val defaultSerializer: CborJacksonSerializer by lazy {
            CborJacksonSerializer(defaultMapper)
        }
    }

    /**
     * Amazon Ion은 리치 타입 시스템을 제공하는 바이너리/텍스트 겸용 데이터 포맷입니다.
     *
     * Ion은 타임스탬프, 정밀 소수점, 바이너리 데이터 등 다양한 네이티브 타입을 지원하며,
     * JSON의 상위 집합으로 동작합니다.
     */
    object ION {

        /**
         * 기본 구성된 Ion 전용 [IonObjectMapper] 인스턴스입니다.
         *
         * Kotlin 모듈 자동 감지가 활성화되어 있으며, 네이티브 타입 ID 사용([IonWriteFeature.USE_NATIVE_TYPE_ID])을 지원합니다.
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
         * 기본 구성된 Ion 전용 [IonFactory] 인스턴스입니다.
         */
        val defaultFactory: IonFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * 기본 구성된 Ion 바이너리 포맷용 Jackson Serializer입니다.
         */
        val defaultSerializer: IonJacksonSerializer by lazy {
            IonJacksonSerializer(defaultMapper)
        }
    }

    /**
     * Smile은 JSON과 1:1 대응하는 바이너리 포맷으로, JSON 대비 인코딩/디코딩 속도가 빠르고 크기가 작습니다.
     *
     * Smile은 JSON 스키마와 완전히 호환되며, 스트리밍 처리에 최적화되어 있습니다.
     */
    object Smile {

        /**
         * 기본 구성된 Smile 전용 [SmileMapper] 인스턴스입니다.
         *
         * Kotlin 모듈 자동 감지가 활성화되어 있으며, 헤더 기록([SmileWriteFeature.WRITE_HEADER])과
         * 종료 마커 기록([SmileWriteFeature.WRITE_END_MARKER])을 지원합니다.
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
         * 기본 구성된 Smile 전용 [SmileFactory] 인스턴스입니다.
         */
        val defaultFactory: SmileFactory by lazy { defaultMapper.tokenStreamFactory() }

        /**
         * 기본 구성된 Smile 바이너리 포맷용 Jackson Serializer입니다.
         */
        val defaultSerializer: SmileJacksonSerializer by lazy {
            SmileJacksonSerializer(defaultMapper)
        }
    }
}
