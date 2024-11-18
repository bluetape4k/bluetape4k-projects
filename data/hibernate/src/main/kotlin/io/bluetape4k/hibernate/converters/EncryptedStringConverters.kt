package io.bluetape4k.hibernate.converters

import io.bluetape4k.crypto.encrypt.Encryptor
import io.bluetape4k.crypto.encrypt.Encryptors
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * 문자열을 암호화해서 문자열로 저장하는 JPA Converter 입니다.
 *
 * ```
 * @Entity
 * class User {
 *      @Id
 *      @GeneratedValue
 *      var id:Long? = null
 *
 *      @Convert(converter=AESStringConverter::class)
 *      var password: String? = null
 * }
 */
@Converter
abstract class EncryptedStringConverter(
    private val encryptor: Encryptor,
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
class AESStringConverter: EncryptedStringConverter(Encryptors.AES)

/**
 * 문자열을 DES 알고리즘으로 암호화해서 저장하는 JPA Converter 입니다.
 */
@Converter
class DESStringConverter: EncryptedStringConverter(Encryptors.DES)

/**
 * 문자열을 RC2 알고리즘으로 암호화해서 저장하는 JPA Converter 입니다.
 */
@Converter
class RC2StringConverter: EncryptedStringConverter(Encryptors.RC2)

/**
 * 문자열을 RC4 알고리즘으로 암호화해서 저장하는 JPA Converter 입니다.
 */
@Converter
class RC4StringConverter: EncryptedStringConverter(Encryptors.RC4)

/**
 * 문자열을 TripleDES 알고리즘으로 암호화해서 저장하는 JPA Converter 입니다.
 */
@Converter
class TripleDESStringConverter: EncryptedStringConverter(Encryptors.TripleDES)
