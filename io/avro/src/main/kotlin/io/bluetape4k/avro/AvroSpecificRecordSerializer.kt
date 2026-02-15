package io.bluetape4k.avro

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import org.apache.avro.specific.SpecificRecord

/**
 * Avro [SpecificRecord] 인스턴스를 직렬화/역직렬화하는 인터페이스입니다.
 *
 * [SpecificRecord]는 Avro 스키마(.avdl, .avsc)로부터 코드 생성된 타입이 지정된 레코드입니다.
 * 컴파일 타임에 타입 안전성을 보장하며, 스키마 진화(Schema Evolution)를 지원합니다.
 *
 * ```
 * val serializer = DefaultAvroSpecificRecordSerializer()
 * val employee: Employee = ...
 *
 * // 단일 객체 직렬화/역직렬화
 * val bytes = serializer.serialize(employee)
 * val deserialized = serializer.deserialize<Employee>(bytes)
 *
 * // 리스트 직렬화/역직렬화
 * val list = listOf(employee1, employee2)
 * val listBytes = serializer.serializeList(list)
 * val deserializedList = serializer.deserializeList<Employee>(listBytes)
 * ```
 *
 * @see AvroGenericRecordSerializer
 * @see AvroReflectSerializer
 */
interface AvroSpecificRecordSerializer {

    /**
     * Avro [SpecificRecord] 인스턴스를 바이너리 형식으로 직렬화합니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param graph 직렬화할 Avro [SpecificRecord] 객체
     * @return 직렬화된 [ByteArray], [graph]가 null이면 null 반환
     */
    fun <T: SpecificRecord> serialize(graph: T?): ByteArray?

    /**
     * Avro 바이너리 데이터를 [SpecificRecord] 인스턴스로 역직렬화합니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param avroBytes 직렬화된 데이터
     * @param clazz 대상 타입의 [Class] 정보
     * @return 역직렬화된 인스턴스, [avroBytes]가 null이거나 실패 시 null 반환
     */
    fun <T: SpecificRecord> deserialize(avroBytes: ByteArray?, clazz: Class<T>): T?

    /**
     * Avro [SpecificRecord]를 바이너리로 직렬화한 뒤, Base64로 인코딩된 문자열로 반환합니다.
     *
     * 네트워크 전송이나 텍스트 기반 저장소에 Avro 데이터를 저장할 때 유용합니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param graph 직렬화할 Avro [SpecificRecord] 객체
     * @return Base64로 인코딩된 문자열, [graph]가 null이면 null 반환
     */
    fun <T: SpecificRecord> serializeAsString(graph: T?): String? {
        return graph?.run { serialize(this)?.encodeBase64String() }
    }

    /**
     * Base64로 인코딩된 Avro 직렬화 문자열을 [SpecificRecord] 인스턴스로 역직렬화합니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param avroText Base64로 인코딩된 직렬화 데이터
     * @param clazz 대상 타입의 [Class] 정보
     * @return 역직렬화된 인스턴스, [avroText]가 null이거나 실패 시 null 반환
     */
    fun <T: SpecificRecord> deserializeFromString(avroText: String?, clazz: Class<T>): T? {
        return avroText?.run { deserialize(this.decodeBase64ByteArray(), clazz) }
    }

    /**
     * Avro [SpecificRecord] 인스턴스의 컬렉션을 바이너리 형식으로 직렬화합니다.
     *
     * 여러 레코드를 하나의 [ByteArray]로 직렬화하여, 배치 전송이나 저장에 효율적입니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param collection 직렬화할 [SpecificRecord] 리스트
     * @return 직렬화된 [ByteArray], [collection]이 null이거나 비어있으면 null 반환
     */
    fun <T: SpecificRecord> serializeList(collection: List<T>?): ByteArray?

    /**
     * Avro 바이너리 데이터를 [SpecificRecord] 인스턴스의 리스트로 역직렬화합니다.
     *
     * @param T [SpecificRecord]를 구현한 Avro 타입
     * @param avroBytes 직렬화된 데이터
     * @param clazz 컬렉션 요소의 [Class] 정보
     * @return 역직렬화된 리스트, [avroBytes]가 null이거나 비어있으면 빈 리스트 반환
     */
    fun <T: SpecificRecord> deserializeList(avroBytes: ByteArray?, clazz: Class<T>): List<T>
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
 * @param T [SpecificRecord]를 구현한 Avro 타입
 * @param avroBytes 직렬화된 데이터
 * @return 역직렬화된 인스턴스, 실패 시 null 반환
 */
inline fun <reified T: SpecificRecord> AvroSpecificRecordSerializer.deserialize(avroBytes: ByteArray?): T? {
    return deserialize(avroBytes, T::class.java)
}

/**
 * Base64로 인코딩된 Avro 직렬화 문자열을 reified 타입 파라미터를 활용하여 역직렬화합니다.
 *
 * ```
 * val employee = serializer.deserializeFromString<Employee>(base64Text)
 * ```
 *
 * @param T [SpecificRecord]를 구현한 Avro 타입
 * @param avroText Base64로 인코딩된 직렬화 데이터
 * @return 역직렬화된 인스턴스, 실패 시 null 반환
 */
inline fun <reified T: SpecificRecord> AvroSpecificRecordSerializer.deserializeFromString(avroText: String?): T? {
    return deserializeFromString(avroText, T::class.java)
}

/**
 * Avro 바이너리 데이터를 reified 타입 파라미터를 활용하여 [SpecificRecord] 리스트로 역직렬화합니다.
 *
 * ```
 * val employees = serializer.deserializeList<Employee>(bytes)
 * ```
 *
 * @param T [SpecificRecord]를 구현한 Avro 타입
 * @param avroBytes 직렬화된 데이터
 * @return 역직렬화된 리스트, [avroBytes]가 null이거나 비어있으면 빈 리스트 반환
 */
inline fun <reified T: SpecificRecord> AvroSpecificRecordSerializer.deserializeList(avroBytes: ByteArray?): List<T> {
    return deserializeList(avroBytes, T::class.java)
}
