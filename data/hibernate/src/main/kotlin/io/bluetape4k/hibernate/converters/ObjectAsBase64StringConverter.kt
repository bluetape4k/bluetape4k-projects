package io.bluetape4k.hibernate.converters

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * 객체를 바이너리 직렬화를 통해 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * ```
 * @Entity
 * class User {
 *     @Id
 *     @GeneratedValue
 *     var id:Long? = null
 *
 *     @Clob
 *     @Convert(converter=JdkObjectAsBase64StringConverter::class)
 *     var data: Any? = null
 * }
 *
 * @property serializer
 * @constructor Create empty Abstract object as byte array converter
 */
@Converter
abstract class AbstractObjectAsBase64StringConverter(
    private val serializer: BinarySerializer,
): AttributeConverter<Any?, String?> {

    override fun convertToDatabaseColumn(attribute: Any?): String? {
        return attribute?.run { serializer.serialize(this).encodeBase64String() }
    }

    override fun convertToEntityAttribute(dbData: String?): Any? {
        return dbData?.run { serializer.deserialize(this.decodeBase64ByteArray()) }
    }
}

/**
 * 객체를 Jdk 직렬화를 통해 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.Jdk
 */
@Converter
class JdkObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.Jdk)

/**
 * 객체를 Jdk 직렬화, LZ4로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.LZ4Jdk
 */
@Converter
class LZ4JdkObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.LZ4Jdk)

/**
 * 객체를 Jdk 직렬화, Snappy로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.SnappyJdk
 */
@Converter
class SnappyJdkObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.SnappyJdk)

/**
 * 객체를 Jdk 직렬화, Zstd로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.ZstdJdk
 */
@Converter
class ZstdJdkObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.ZstdJdk)

/**
 * 객체를 Kryo 직렬화를 통해 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.Kryo
 */
@Converter
class KryoObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.Kryo)

/**
 * 객체를 Kryo 직렬화, LZ4로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.LZ4Kryo
 */
@Converter
class LZ4KryoObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.LZ4Kryo)

/**
 * 객체를 Kryo 직렬화, Snappy로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.SnappyKryo
 */
@Converter
class SnappyKryoObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.SnappyKryo)

/**
 * 객체를 Kryo 직렬화, Zstd로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.ZstdKryo
 */
@Converter
class ZstdKryoObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.ZstdKryo)
