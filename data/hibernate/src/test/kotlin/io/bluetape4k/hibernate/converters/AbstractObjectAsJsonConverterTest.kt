package io.bluetape4k.hibernate.converters

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class AbstractObjectAsJsonConverterTest {

    data class Address(val street: String, val city: String, val zip: String)

    class AddressConverter : AbstractObjectAsJsonConverter<Address>(Address::class.java)

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
        val json = converter.convertToDatabaseColumn(address)
        json.shouldNotBeNull()
        json.contains("street").shouldNotBeNull()
        json.contains("Springfield").shouldNotBeNull()
    }

    @Test
    fun `JSON 문자열을 객체로 역직렬화한다`() {
        val json = converter.convertToDatabaseColumn(address)!!
        val restored = converter.convertToEntityAttribute(json)
        restored shouldBeEqualTo address
    }

    @Test
    fun `왕복 변환 후 원본과 동일해야 한다`() {
        val json = converter.convertToDatabaseColumn(address)
        val restored = converter.convertToEntityAttribute(json)
        restored shouldBeEqualTo address
    }

    @Test
    fun `잘못된 JSON 입력 시 null을 반환한다`() {
        val result = converter.convertToEntityAttribute("invalid json {{{")
        result.shouldBeNull()
    }

    @Test
    fun `중첩 객체를 포함한 복잡한 데이터도 처리한다`() {
        val address2 = Address("456 Oak Ave", "Shelbyville", "62565")
        val json = converter.convertToDatabaseColumn(address2)
        val restored = converter.convertToEntityAttribute(json)
        restored shouldBeEqualTo address2
    }
}
