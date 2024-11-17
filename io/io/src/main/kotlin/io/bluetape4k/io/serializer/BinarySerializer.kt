package io.bluetape4k.io.serializer

/**
 * 객체를 Binary 방식으로 직렬화/역직렬화하는 Serializer 의 최상위 인터페이스
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
