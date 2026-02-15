package io.bluetape4k.avro

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String

/**
 * Reflection을 이용하여 Avro 객체를 직렬화하고 역직렬화하는 인터페이스입니다.
 *
 * Avro 스키마(.avdl, .avsc)로부터 코드 생성 없이도, Java/Kotlin 클래스의 필드 정보를
 * Reflection으로 분석하여 Avro 직렬화/역직렬화를 수행합니다.
 * 기존 POJO/데이터 클래스를 변경 없이 Avro로 직렬화할 수 있어 편리하지만,
 * Reflection 오버헤드로 인해 [AvroSpecificRecordSerializer]보다 성능이 낮을 수 있습니다.
 *
 * ```
 * val serializer = DefaultAvroReflectSerializer()
 * val employee: Employee = ...
 *
 * // ByteArray로 직렬화/역직렬화
 * val bytes = serializer.serialize(employee)
 * val deserialized = serializer.deserialize<Employee>(bytes)
 *
 * // Base64 문자열로 직렬화/역직렬화
 * val text = serializer.serializeAsString(employee)
 * val fromText = serializer.deserializeFromString<Employee>(text)
 * ```
 *
 * @see AvroGenericRecordSerializer
 * @see AvroSpecificRecordSerializer
 */
interface AvroReflectSerializer {

    /**
     * 객체를 Avro Reflection 기반으로 바이너리 형식으로 직렬화합니다.
     *
     * @param T 직렬화할 객체의 타입
     * @param graph 직렬화할 객체
     * @return 직렬화된 [ByteArray], [graph]가 null이면 null 반환
     */
    fun <T> serialize(graph: T?): ByteArray?

    /**
     * Avro 바이너리 데이터를 Reflection 기반으로 지정된 타입의 인스턴스로 역직렬화합니다.
     *
     * @param T 역직렬화할 타입
     * @param avroBytes 직렬화된 데이터
     * @param clazz 대상 타입의 [Class] 정보
     * @return 역직렬화된 인스턴스, [avroBytes]가 null이거나 실패 시 null 반환
     */
    fun <T> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T?

    /**
     * 객체를 Avro Reflection 기반으로 직렬화한 뒤, Base64로 인코딩된 문자열로 반환합니다.
     *
     * 네트워크 전송이나 텍스트 기반 저장소에 Avro 데이터를 저장할 때 유용합니다.
     *
     * @param T 직렬화할 객체의 타입
     * @param graph 직렬화할 객체
     * @return Base64로 인코딩된 문자열, [graph]가 null이면 null 반환
     */
    fun <T> serializeAsString(graph: T?): String? {
        return graph?.run { serialize(this)?.encodeBase64String() }
    }

    /**
     * Base64로 인코딩된 Avro 직렬화 문자열을 지정된 타입의 인스턴스로 역직렬화합니다.
     *
     * @param T 역직렬화할 타입
     * @param avroText Base64로 인코딩된 직렬화 데이터
     * @param clazz 대상 타입의 [Class] 정보
     * @return 역직렬화된 인스턴스, [avroText]가 null이거나 실패 시 null 반환
     */
    fun <T> deserializeFromString(avroText: String?, clazz: Class<T>): T? {
        return avroText?.run { deserialize(this.decodeBase64ByteArray(), clazz) }
    }
}

/**
 * Avro 바이너리 데이터를 reified 타입 파라미터를 활용하여 역직렬화합니다.
 *
 * 명시적으로 [Class] 객체를 전달하지 않아도 되므로, 보다 간결한 Kotlin API를 제공합니다.
 *
 * ```
 * val employee = serializer.deserialize<Employee>(bytes)
 * ```
 *
 * @param T 역직렬화할 타입
 * @param avroBytes 직렬화된 데이터
 * @return 역직렬화된 인스턴스, 실패 시 null 반환
 */
inline fun <reified T: Any> AvroReflectSerializer.deserialize(avroBytes: ByteArray?): T? {
    return deserialize(avroBytes, T::class.java)
}

/**
 * Base64로 인코딩된 Avro 직렬화 문자열을 reified 타입 파라미터를 활용하여 역직렬화합니다.
 *
 * ```
 * val employee = serializer.deserializeFromString<Employee>(base64Text)
 * ```
 *
 * @param T 역직렬화할 타입
 * @param avroText Base64로 인코딩된 직렬화 데이터
 * @return 역직렬화된 인스턴스, 실패 시 null 반환
 */
inline fun <reified T: Any> AvroReflectSerializer.deserializeFromString(avroText: String?): T? {
    return deserializeFromString(avroText, T::class.java)
}
