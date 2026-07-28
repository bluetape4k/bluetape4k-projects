@file:Suppress("DEPRECATION")

package io.bluetape4k.hibernate.converters

import io.bluetape4k.codec.decodeBase64ByteArray
import io.bluetape4k.codec.encodeBase64String
import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializationException
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.io.serializer.JdkBinarySerializer
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

private const val TRUSTED_ONLY_BASE64_STRING_CONVERTER =
    "Generic object converters deserialize arbitrary payloads and are trusted-storage-only. " +
            "Define an AbstractTypedObjectAsBase64StringConverter subclass with a target type and a secure serializer."

/**
 * 객체를 바이너리 직렬화를 통해 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * ```kotlin
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
 * ```
 *
 * @property serializer 바이너리 직렬화
 */
@Deprecated(TRUSTED_ONLY_BASE64_STRING_CONVERTER)
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
 * Typed Base64 string converter that rejects deserialized values outside [targetType].
 *
 * trust boundary를 넘을 수 있는 persistent column에는 이 base class를 사용합니다. serializer는 계속
 * pluggable, so callers can combine the typed converter boundary with secure serializers such as
 * `KryoBinarySerializer.secure(...)` or `ForyBinarySerializer.secureFory(...)`.
 *
 * ```kotlin
 * class UserProfileAsBase64StringConverter: AbstractTypedObjectAsBase64StringConverter<UserProfile>(
 *     targetType = UserProfile::class.java,
 *     serializer = KryoBinarySerializer.secure(UserProfile::class.java),
 * )
 * ```
 */
@Converter
abstract class AbstractTypedObjectAsBase64StringConverter<T: Any>(
    private val targetType: Class<T>,
    private val serializer: BinarySerializer,
): AttributeConverter<T?, String?> {

    override fun convertToDatabaseColumn(attribute: T?): String? {
        return attribute?.run { serializer.serialize(this).encodeBase64String() }
    }

    override fun convertToEntityAttribute(dbData: String?): T? {
        return try {
            val value = dbData?.run { serializer.deserialize<Any>(this.decodeBase64ByteArray()) }
            requireExpectedType(value, targetType)
        } catch (e: BinarySerializationException) {
            throw e
        } catch (e: Throwable) {
            throw BinarySerializationException(
                "Fail to deserialize Hibernate Base64 string converter payload. targetType=${targetType.name}",
                e,
            )
        }
    }
}

/**
 * 객체를 Jdk 직렬화를 통해 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.Jdk
 */
@Deprecated(TRUSTED_ONLY_BASE64_STRING_CONVERTER)
@Converter
class JdkObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(JdkBinarySerializer())

/**
 * 객체를 Jdk 직렬화, LZ4로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.LZ4Jdk
 */
@Deprecated(TRUSTED_ONLY_BASE64_STRING_CONVERTER)
@Converter
class LZ4JdkObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.LZ4Jdk)

/**
 * 객체를 Jdk 직렬화, Snappy로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.SnappyJdk
 */
@Deprecated(TRUSTED_ONLY_BASE64_STRING_CONVERTER)
@Converter
class SnappyJdkObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.SnappyJdk)

/**
 * 객체를 Jdk 직렬화, Zstd로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.ZstdJdk
 */
@Deprecated(TRUSTED_ONLY_BASE64_STRING_CONVERTER)
@Converter
class ZstdJdkObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.ZstdJdk)

/**
 * 객체를 Kryo 직렬화를 통해 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.Kryo
 */
@Deprecated(TRUSTED_ONLY_BASE64_STRING_CONVERTER)
@Converter
class KryoObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.Kryo)

/**
 * 객체를 Kryo 직렬화, LZ4로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.LZ4Kryo
 */
@Deprecated(TRUSTED_ONLY_BASE64_STRING_CONVERTER)
@Converter
class LZ4KryoObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.LZ4Kryo)

/**
 * 객체를 Kryo 직렬화, Snappy로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.SnappyKryo
 */
@Deprecated(TRUSTED_ONLY_BASE64_STRING_CONVERTER)
@Converter
class SnappyKryoObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.SnappyKryo)

/**
 * 객체를 Kryo 직렬화, Zstd로 압축 한 후 Base64 인코딩된 문자열로 변환해서 DB에 저장합니다.
 *
 * @see BinarySerializers.ZstdKryo
 */
@Deprecated(TRUSTED_ONLY_BASE64_STRING_CONVERTER)
@Converter
class ZstdKryoObjectAsBase64StringConverter: AbstractObjectAsBase64StringConverter(BinarySerializers.ZstdKryo)
