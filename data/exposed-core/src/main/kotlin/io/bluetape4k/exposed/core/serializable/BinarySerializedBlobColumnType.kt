package io.bluetape4k.exposed.core.serializable

import io.bluetape4k.exposed.core.statements.api.toExposedBlob
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.jetbrains.exposed.v1.core.BlobColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.statements.api.ExposedBlob

/**
 * 객체를 바이너리 직렬화해 `BLOB`로 저장하는 컬럼을 등록합니다.
 *
 * ## 동작/계약
 * - 저장 시 직렬화 결과를 [ExposedBlob]으로 감싸서 저장합니다.
 * - 조회 시 blob bytes를 역직렬화하며 결과가 `null`이면 [IllegalStateException]을 던집니다.
 *
 * ```kotlin
 * val payload = table.binarySerializedBlob<MyDto>("payload")
 * // payload.columnType.sqlType().contains("BLOB")
 * ```
 */
fun <T: Any> Table.binarySerializedBlob(
    name: String,
    serializer: BinarySerializer = BinarySerializers.LZ4Fory,
): Column<T> = registerColumn(name, BinarySerializedBlobColumnType(serializer))

/** `BLOB` + 바이너리 직렬화 변환기를 결합한 컬럼 타입입니다. */
class BinarySerializedBlobColumnType<T: Any>(
    serializer: BinarySerializer,
): ColumnWithTransform<ExposedBlob, T>(BlobColumnType(), BinarySerializedBlobTransformer(serializer))

/** 객체와 [ExposedBlob] 간 직렬화/역직렬화 변환기입니다. */
class BinarySerializedBlobTransformer<T>(
    private val serializer: BinarySerializer,
): ColumnTransformer<ExposedBlob, T> {
    /** 엔티티 객체를 직렬화해 blob으로 변환합니다. */
    override fun unwrap(value: T): ExposedBlob = serializer.serialize(value).toExposedBlob()

    /** DB blob을 역직렬화해 엔티티 객체로 변환합니다. */
    override fun wrap(value: ExposedBlob): T =
        serializer.deserialize(value.bytes)
            ?: error("역직렬화 결과가 null입니다. 데이터가 손상되었거나 직렬화 형식이 맞지 않습니다. (size: ${value.bytes.size} bytes)")
}
