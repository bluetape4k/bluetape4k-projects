package io.bluetape4k.hibernate.converter

import io.bluetape4k.hibernate.converters.AESStringConverter
import io.bluetape4k.hibernate.converters.DeterministicAESStringConverter
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [AESStringConverter]와 [DeterministicAESStringConverter]에 대한 단위 테스트입니다.
 */
class EncryptedStringConverterTest {

    companion object: KLogging()

    private val aesConverter = AESStringConverter()
    private val deterministicConverter = DeterministicAESStringConverter()

    @Test
    fun `AESStringConverter는 문자열을 암호화하고 복호화한다`() {
        val plainText = "Hello, Bluetape4k!"

        val encrypted = aesConverter.convertToDatabaseColumn(plainText)
        encrypted.shouldNotBeNull()
        encrypted shouldNotBeEqualTo plainText

        val decrypted = aesConverter.convertToEntityAttribute(encrypted)
        decrypted shouldBeEqualTo plainText
    }

    @Test
    fun `AESStringConverter는 null 입력 시 null을 반환한다`() {
        aesConverter.convertToDatabaseColumn(null).shouldBeNull()
        aesConverter.convertToEntityAttribute(null).shouldBeNull()
    }

    @Test
    fun `AESStringConverter는 동일 평문에 대해 매번 다른 암호문을 생성한다`() {
        val plainText = "same input"

        val encrypted1 = aesConverter.convertToDatabaseColumn(plainText)
        val encrypted2 = aesConverter.convertToDatabaseColumn(plainText)

        encrypted1.shouldNotBeNull()
        encrypted2.shouldNotBeNull()
        // AES-GCM은 비결정적이므로 같은 평문이라도 암호문이 다르다
        encrypted1 shouldNotBeEqualTo encrypted2
    }

    @Test
    fun `AESStringConverter는 빈 문자열도 암호화하고 복호화한다`() {
        val plainText = ""

        val encrypted = aesConverter.convertToDatabaseColumn(plainText)
        encrypted.shouldNotBeNull()

        val decrypted = aesConverter.convertToEntityAttribute(encrypted)
        decrypted shouldBeEqualTo plainText
    }

    @Test
    fun `DeterministicAESStringConverter는 문자열을 암호화하고 복호화한다`() {
        val plainText = "Secret Password 123!"

        val encrypted = deterministicConverter.convertToDatabaseColumn(plainText)
        encrypted.shouldNotBeNull()
        encrypted shouldNotBeEqualTo plainText

        val decrypted = deterministicConverter.convertToEntityAttribute(encrypted)
        decrypted shouldBeEqualTo plainText
    }

    @Test
    fun `DeterministicAESStringConverter는 null 입력 시 null을 반환한다`() {
        deterministicConverter.convertToDatabaseColumn(null).shouldBeNull()
        deterministicConverter.convertToEntityAttribute(null).shouldBeNull()
    }

    @Test
    fun `DeterministicAESStringConverter는 동일 평문에 대해 항상 같은 암호문을 생성한다`() {
        val plainText = "deterministic input"

        val encrypted1 = deterministicConverter.convertToDatabaseColumn(plainText)
        val encrypted2 = deterministicConverter.convertToDatabaseColumn(plainText)

        encrypted1.shouldNotBeNull()
        encrypted2.shouldNotBeNull()
        // AES-SIV는 결정적이므로 같은 평문이면 항상 같은 암호문을 생성한다
        encrypted1 shouldBeEqualTo encrypted2
    }

    @Test
    fun `DeterministicAESStringConverter는 서로 다른 평문에 대해 다른 암호문을 생성한다`() {
        val plainText1 = "password-1"
        val plainText2 = "password-2"

        val encrypted1 = deterministicConverter.convertToDatabaseColumn(plainText1)
        val encrypted2 = deterministicConverter.convertToDatabaseColumn(plainText2)

        encrypted1.shouldNotBeNull()
        encrypted2.shouldNotBeNull()
        encrypted1 shouldNotBeEqualTo encrypted2
    }

    @Test
    fun `DeterministicAESStringConverter는 한국어를 포함한 문자열을 처리한다`() {
        val plainText = "특수!@#$%^&*()문자열 테스트"

        val encrypted = deterministicConverter.convertToDatabaseColumn(plainText)
        encrypted.shouldNotBeNull()

        val decrypted = deterministicConverter.convertToEntityAttribute(encrypted)
        decrypted shouldBeEqualTo plainText
    }
}
