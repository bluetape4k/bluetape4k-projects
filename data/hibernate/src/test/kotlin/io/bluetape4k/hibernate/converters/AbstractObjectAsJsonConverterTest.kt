package io.bluetape4k.hibernate.converters

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.assertions.shouldNotContain
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import java.io.Serializable

class AbstractObjectAsJsonConverterTest {

    companion object: KLogging()

    data class Address(
        val street: String,
        val city: String,
        val zip: String
    ): Serializable

    class AddressConverter: AbstractObjectAsJsonConverter<Address>(Address::class.java)

    private val converter = AddressConverter()
    private val address = Address("123 Main St", "Springfield", "62701")

    @Test
    fun `convertToDatabaseColumn은 null 입력 시 null을 반환한다`() {
        converter.convertToDatabaseColumn(null).shouldBeNull()
    }

    @Test
    fun `convertToEntityAttribute은 null 입력 시 null을 반환한다`() {
        converter.convertToEntityAttribute(null).shouldBeNull()
    }

    @Test
    fun `객체를 JSON 문자열로 직렬화한다`() {
        val json = converter.convertToDatabaseColumn(address).shouldNotBeNull()
        log.debug { "json=$json" }

        json shouldContain Address::street.name
        json shouldContain address.street
        json shouldContain Address::city.name
        json shouldContain address.city
        json shouldContain Address::zip.name
        json shouldContain address.zip

        json shouldNotContain "Address"
        json shouldNotContain "address"
    }

    @Test
    fun `JSON 문자열을 객체로 역직렬화한다`() {
        val json = converter.convertToDatabaseColumn(address).shouldNotBeEmpty()
        val restored = converter.convertToEntityAttribute(json).shouldNotBeNull()
        restored shouldBeEqualTo address
    }

    @Test
    fun `왕복 변환 후 원본과 동일해야 한다`() {
        val json = converter.convertToDatabaseColumn(address).shouldNotBeEmpty()
        val restored = converter.convertToEntityAttribute(json).shouldNotBeNull()
        restored shouldBeEqualTo address
    }

    @Test
    fun `잘못된 JSON 입력 시 null을 반환한다`() {
        converter.convertToEntityAttribute("invalid json {{{").shouldBeNull()
    }

    @Test
    fun `중첩 객체를 포함한 복잡한 데이터도 처리한다`() {
        val address2 = Address("456 Oak Ave", "Shelbyville", "62565")
        val json = converter.convertToDatabaseColumn(address2).shouldNotBeEmpty()
        val restored = converter.convertToEntityAttribute(json).shouldNotBeNull()
        restored shouldBeEqualTo address2
    }
}
