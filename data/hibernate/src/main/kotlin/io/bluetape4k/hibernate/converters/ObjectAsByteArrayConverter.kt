package io.bluetape4k.hibernate.converters

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64ByteArray
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * 객체를 직렬화하여 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 *
 * ```
 * @Entity
 * class User {
 *    @Id
 *    @GeneratedValue
 *    var id:Long? = null
 *
 *    @Lob
 *    @Convert(converter=JdkObjectAsByteArrayConverter::class)
 *    var data: Any? = null
 * }
 *
 * @property serializer 바이너리 직렬화
 */
@Converter
abstract class AbstractObjectAsByteArrayConverter(
    private val serializer: BinarySerializer,
): AttributeConverter<Any?, ByteArray?> {

    override fun convertToDatabaseColumn(attribute: Any?): ByteArray? {
        return attribute?.run { serializer.serialize(this).encodeBase64ByteArray() }
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): Any? {
        return dbData?.run { serializer.deserialize(this.decodeBase64ByteArray()) }
    }
}

/**
 * 객체를 Jdk 직렬화하여 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Converter
class JdkObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.Jdk)

/**
 * 객체를 Jdk 직렬화, LZ4로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Converter
class LZ4JdkObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.LZ4Jdk)

/**
 * 객체를 Jdk 직렬화, Snappy로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Converter
class SnappyJdkObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.SnappyJdk)

/**
 * 객체를 Jdk 직렬화, Zstd로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Converter
class ZstdJdkObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.ZstdJdk)

/**
 * 객체를 Kryo 직렬화하여 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Converter
class KryoObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.Kryo)

/**
 * 객체를 Kryo 직렬화, LZ4로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Converter
class LZ4KryoObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.LZ4Kryo)

/**
 * 객체를 Kryo 직렬화, Snappy로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Converter
class SnappyKryoObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.SnappyKryo)

/**
 * 객체를 Kryo 직렬화, Zstd로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Converter
class ZstdKryoObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.ZstdKryo)

@Converter
class ForyObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.Fory)

@Converter
class LZ4ForyObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.LZ4Fory)

@Converter
class SnappyForyObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.SnappyFory)

@Converter
class ZstdForyObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.ZstdFory)
