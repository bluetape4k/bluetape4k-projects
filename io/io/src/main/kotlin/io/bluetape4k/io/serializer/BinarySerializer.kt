package io.bluetape4k.io.serializer

/**
 * 객체를 바이너리 형식으로 직렬화/역직렬화하는 최상위 인터페이스입니다.
 *
 * ## Null 처리 정책
 * - `serialize(null)`: 빈 [ByteArray]를 반환합니다.
 * - `deserialize(null)` 또는 `deserialize(emptyByteArray)`: `null`을 반환합니다.
 * - 그 외 직렬화/역직렬화 실패: 구현체에 따라 예외를 던질 수 있습니다.
 *
 * ## 사용 예시
 * ```kotlin
 * val serializer = BinarySerializers.Kryo
 * val bytes = serializer.serialize("Hello, World!")
 * val text = serializer.deserialize<String>(bytes)  // "Hello, World!"
 * ```
 *
 * @see BinarySerializers
 * @see AbstractBinarySerializer
 */
interface BinarySerializer {

    /**
     * 객체를 Binary 방식으로 직렬화합니다.
     *
     * @param graph 직렬화할 객체
     * @return 직렬화된 데이터
     */
    fun serialize(graph: Any?): ByteArray

    /**
     * 직렬화된 데이터를 읽어 대상 객체로 역직렬화합니다.
     *
     * @param T     역직렬화할 객체 수형
     * @param bytes 직렬화된 데이터
     * @return 역직렬화한 객체
     */
    fun <T: Any> deserialize(bytes: ByteArray?): T?

}
