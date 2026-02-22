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
 * 엔티티의 속성으로 사용하는 객체를 [BinarySerializer] 를 이용해
 * 직렬화/역직렬화하여 [ExposedBlob] Column에 저장하도록 하는 Column 을 생성합니다.
 */
fun <T: Any> Table.binarySerializedBlob(
    name: String,
    serializer: BinarySerializer = BinarySerializers.LZ4Fory,
): Column<T> =
    registerColumn(name, BinarySerializedBlobColumnType(serializer))

class BinarySerializedBlobColumnType<T: Any>(serializer: BinarySerializer):
    ColumnWithTransform<ExposedBlob, T>(BlobColumnType(), BinarySerializedBlobTransformer(serializer))

/**
 * 엔티티 속성 객체를 [ExposedBlob]로 직렬화/역직렬화합니다.
 */
class BinarySerializedBlobTransformer<T>(
    private val serializer: BinarySerializer,
): ColumnTransformer<ExposedBlob, T> {

    /**
     * Entity Property 를 DB Column 수형으로 변환합니다.
     */
    override fun unwrap(value: T): ExposedBlob = serializer.serialize(value).toExposedBlob()

    /**
     * DB Column 값을 Entity Property 수형으로 변환합니다.
     */
    override fun wrap(value: ExposedBlob): T = serializer.deserialize(value.bytes)!!
}
