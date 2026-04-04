package io.bluetape4k.hibernate.converters

import io.bluetape4k.tink.encrypt.TinkEncryptor
import io.bluetape4k.tink.encrypt.TinkEncryptors
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * 문자열을 암호화해서 문자열로 저장하는 JPA Converter 입니다.
 *
 * ```kotlin
 * @Entity
 * class User {
 *      @Id
 *      @GeneratedValue
 *      var id:Long? = null
 *
 *      @Convert(converter=AESStringConverter::class)
 *      var password: String? = null
 * }
 * ```
 */
@Converter
abstract class EncryptedStringConverter(
    private val encryptor: TinkEncryptor = TinkEncryptors.DETERMINISTIC_AES256_SIV,
): AttributeConverter<String?, String?> {

    override fun convertToDatabaseColumn(attribute: String?): String? {
        return attribute?.run { encryptor.encrypt(this) }
    }

    override fun convertToEntityAttribute(dbData: String?): String? {
        return dbData?.run { encryptor.decrypt(this) }
    }
}

/**
 * 문자열을 AES 알고리즘으로 암호화해서 저장하는 JPA Converter 입니다.
 */
@Converter
class AESStringConverter: EncryptedStringConverter(TinkEncryptors.AES256_GCM)

/**
 * 문자열을 결정적 AES 알고리즘으로 암호화해서 저장하는 JPA Converter 입니다.
 */
@Converter
class DeterministicAESStringConverter: EncryptedStringConverter(TinkEncryptors.DETERMINISTIC_AES256_SIV)
