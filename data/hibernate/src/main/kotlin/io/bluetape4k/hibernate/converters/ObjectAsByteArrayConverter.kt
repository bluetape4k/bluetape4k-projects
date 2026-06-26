@file:Suppress("DEPRECATION")

package io.bluetape4k.hibernate.converters

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64ByteArray
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.io.serializer.JdkBinarySerializer
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

private const val TRUSTED_ONLY_BYTE_ARRAY_CONVERTER =
    "Generic object converters deserialize arbitrary payloads and are trusted-storage-only. " +
            "Define an AbstractTypedObjectAsByteArrayConverter subclass with a target type and a secure serializer."

/**
 * 객체를 직렬화하여 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 *
 * ```kotlin
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
 * ```
 *
 * @property serializer 바이너리 직렬화
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
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
 * Typed object converter that rejects deserialized values outside [targetType].
 *
 * Use this base class for persistent columns that may cross a trust boundary. The serializer is still
 * pluggable, so callers can combine the typed converter boundary with secure serializers such as
 * `KryoBinarySerializer.secure(...)` or `ForyBinarySerializer.secureFory(...)`.
 *
 * ```kotlin
 * class UserProfileAsByteArrayConverter: AbstractTypedObjectAsByteArrayConverter<UserProfile>(
 *     targetType = UserProfile::class.java,
 *     serializer = KryoBinarySerializer.secure(UserProfile::class.java),
 * )
 * ```
 */
@Converter
abstract class AbstractTypedObjectAsByteArrayConverter<T: Any>(
    private val targetType: Class<T>,
    private val serializer: BinarySerializer,
): AttributeConverter<T?, ByteArray?> {

    override fun convertToDatabaseColumn(attribute: T?): ByteArray? {
        return attribute?.run { serializer.serialize(this).encodeBase64ByteArray() }
    }

    override fun convertToEntityAttribute(dbData: ByteArray?): T? {
        return try {
            val value = dbData?.run { serializer.deserialize<Any>(this.decodeBase64ByteArray()) }
            requireExpectedType(value, targetType)
        } catch (e: BinarySerializationException) {
            throw e
        } catch (e: Throwable) {
            throw BinarySerializationException(
                "Fail to deserialize Hibernate ByteArray converter payload. targetType=${targetType.name}",
                e,
            )
        }
    }
}

internal fun <T: Any> requireExpectedType(value: Any?, targetType: Class<T>): T? {
    if (value == null) {
        return null
    }
    if (targetType.isInstance(value)) {
        return targetType.cast(value)
    }

    throw BinarySerializationException(
        "Unexpected Hibernate converter payload type. expected=${targetType.name}, actual=${value.javaClass.name}",
    )
}

/**
 * 객체를 Jdk 직렬화하여 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class JdkObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(JdkBinarySerializer())

/**
 * 객체를 Jdk 직렬화, LZ4로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class LZ4JdkObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.LZ4Jdk)

/**
 * 객체를 Jdk 직렬화, Snappy로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class SnappyJdkObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.SnappyJdk)

/**
 * 객체를 Jdk 직렬화, Zstd로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class ZstdJdkObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.ZstdJdk)

/**
 * 객체를 Kryo 직렬화하여 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class KryoObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.Kryo)

/**
 * 객체를 Kryo 직렬화, LZ4로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class LZ4KryoObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.LZ4Kryo)

/**
 * 객체를 Kryo 직렬화, Snappy로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class SnappyKryoObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.SnappyKryo)

/**
 * 객체를 Kryo 직렬화, Zstd로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class ZstdKryoObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.ZstdKryo)

/**
 * 객체를 Apache Fory 직렬화하여 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class ForyObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.Fory)

/**
 * 객체를 Apache Fory 직렬화, LZ4로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class LZ4ForyObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.LZ4Fory)

/**
 * 객체를 Apache Fory 직렬화, Snappy로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class SnappyForyObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.SnappyFory)

/**
 * 객체를 Apache Fory 직렬화, Zstd로 압축 한 후 Base64 인코딩을 거쳐 ByteArray 로 변환해서 DB에 저장합니다.
 */
@Deprecated(TRUSTED_ONLY_BYTE_ARRAY_CONVERTER)
@Converter
class ZstdForyObjectAsByteArrayConverter: AbstractObjectAsByteArrayConverter(BinarySerializers.ZstdFory)
