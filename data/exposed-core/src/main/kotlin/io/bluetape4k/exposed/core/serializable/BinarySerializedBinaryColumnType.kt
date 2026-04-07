package io.bluetape4k.exposed.core.serializable

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table

/**
 * 객체를 바이너리 직렬화해 `VARBINARY`로 저장하는 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 시 `serializer.serialize`, 조회 시 `serializer.deserialize`를 사용합니다.
 * - `deserialize` 결과가 `null`이면 [IllegalStateException]을 던집니다.
 *
 * ```kotlin
 * val payload = table.binarySerializedBinary<MyDto>("payload", length = 4096)
 * // payload.columnType.sqlType().contains("VARBINARY")
 * ```
 */
fun <T: Any> Table.binarySerializedBinary(
    name: String,
    length: Int,
    serializer: BinarySerializer = BinarySerializers.LZ4Fory,
): Column<T> = registerColumn(name, BinarySerializedBinaryColumnType(serializer, length))

/** `VARBINARY` + 바이너리 직렬화 변환기를 결합한 컬럼 타입입니다. */
class BinarySerializedBinaryColumnType<T: Any>(
    serializer: BinarySerializer,
    length: Int,
): ColumnWithTransform<ByteArray, T>(BinaryColumnType(length), BinarySerializedBinaryTransformer(serializer))

/** 객체 직렬화/역직렬화 변환기입니다. */
class BinarySerializedBinaryTransformer<T>(
    private val serializer: BinarySerializer,
): ColumnTransformer<ByteArray, T> {
    /** 엔티티 객체를 DB 저장용 바이트 배열로 직렬화합니다. */
    override fun unwrap(value: T): ByteArray = serializer.serialize(value)

    /** DB 바이트 배열을 엔티티 객체로 역직렬화합니다. */
    override fun wrap(value: ByteArray): T =
        serializer.deserialize(value)
            ?: error("역직렬화 결과가 null입니다. 데이터가 손상되었거나 직렬화 형식이 맞지 않습니다. (size: ${value.size} bytes)")
}
