package io.bluetape4k.exposed.core.serializable

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import org.jetbrains.exposed.v1.core.BinaryColumnType
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnTransformer
import org.jetbrains.exposed.v1.core.ColumnWithTransform
import org.jetbrains.exposed.v1.core.Table

/**
 * 엔티티의 속성으로 사용하는 객체를 [BinarySerializer] 를 이용해 직렬화/역직렬화하여 Binary Column 에 저장할 수 있는 Column 을 생성합니다.
 */
fun <T: Any> Table.binarySerializedBinary(
    name: String,
    length: Int,
    serializer: BinarySerializer = BinarySerializers.LZ4Fory,
): Column<T> = registerColumn(name, BinarySerializedBinaryColumnType(serializer, length))

/**
 * 객체를 바이너리로 직렬화하여 `VARBINARY` 컬럼에 저장하는 컬럼 타입입니다.
 */
class BinarySerializedBinaryColumnType<T: Any>(serializer: BinarySerializer, length: Int):
    ColumnWithTransform<ByteArray, T>(BinaryColumnType(length), BinarySerializedBinaryTransformer(serializer))

/**
 * 엔티티 속성 객체를 직렬화/역직렬화합니다.
 */
class BinarySerializedBinaryTransformer<T>(
    private val serializer: BinarySerializer,
): ColumnTransformer<ByteArray, T> {
    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: T): ByteArray = serializer.serialize(value)

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ByteArray): T = serializer.deserialize(value)!!
}
