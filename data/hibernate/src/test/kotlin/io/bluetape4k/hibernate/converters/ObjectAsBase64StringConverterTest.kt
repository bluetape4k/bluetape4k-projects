package io.bluetape4k.hibernate.converters

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.io.Serializable

class ObjectAsBase64StringConverterTest {

    data class SampleData(val name: String, val value: Int) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    private val sample = SampleData("hello", 42)

    @Test
    fun `JdkObjectAsBase64StringConverter는 null을 null로 변환한다`() {
        val converter = JdkObjectAsBase64StringConverter()
        converter.convertToDatabaseColumn(null).shouldBeNull()
        converter.convertToEntityAttribute(null).shouldBeNull()
    }

    @Test
    fun `JdkObjectAsBase64StringConverter는 객체를 Base64로 직렬화하고 역직렬화한다`() {
        val converter = JdkObjectAsBase64StringConverter()
        val encoded = converter.convertToDatabaseColumn(sample)
        encoded.shouldNotBeNull()

        val decoded = converter.convertToEntityAttribute(encoded)
        decoded shouldBeEqualTo sample
    }

    @Test
    fun `LZ4JdkObjectAsBase64StringConverter는 객체를 압축 직렬화하고 역직렬화한다`() {
        val converter = LZ4JdkObjectAsBase64StringConverter()
        val encoded = converter.convertToDatabaseColumn(sample)
        encoded.shouldNotBeNull()

        val decoded = converter.convertToEntityAttribute(encoded)
        decoded shouldBeEqualTo sample
    }

    @Test
    fun `SnappyJdkObjectAsBase64StringConverter는 객체를 압축 직렬화하고 역직렬화한다`() {
        val converter = SnappyJdkObjectAsBase64StringConverter()
        val encoded = converter.convertToDatabaseColumn(sample)
        encoded.shouldNotBeNull()

        val decoded = converter.convertToEntityAttribute(encoded)
        decoded shouldBeEqualTo sample
    }

    @Test
    fun `ZstdJdkObjectAsBase64StringConverter는 객체를 압축 직렬화하고 역직렬화한다`() {
        val converter = ZstdJdkObjectAsBase64StringConverter()
        val encoded = converter.convertToDatabaseColumn(sample)
        encoded.shouldNotBeNull()

        val decoded = converter.convertToEntityAttribute(encoded)
        decoded shouldBeEqualTo sample
    }

    @Test
    fun `KryoObjectAsBase64StringConverter는 객체를 Kryo 직렬화하고 역직렬화한다`() {
        val converter = KryoObjectAsBase64StringConverter()
        val encoded = converter.convertToDatabaseColumn(sample)
        encoded.shouldNotBeNull()

        val decoded = converter.convertToEntityAttribute(encoded)
        decoded shouldBeEqualTo sample
    }

    @Test
    fun `LZ4KryoObjectAsBase64StringConverter는 객체를 압축 직렬화하고 역직렬화한다`() {
        val converter = LZ4KryoObjectAsBase64StringConverter()
        val encoded = converter.convertToDatabaseColumn(sample)
        encoded.shouldNotBeNull()

        val decoded = converter.convertToEntityAttribute(encoded)
        decoded shouldBeEqualTo sample
    }

    @Test
    fun `SnappyKryoObjectAsBase64StringConverter는 객체를 압축 직렬화하고 역직렬화한다`() {
        val converter = SnappyKryoObjectAsBase64StringConverter()
        val encoded = converter.convertToDatabaseColumn(sample)
        encoded.shouldNotBeNull()

        val decoded = converter.convertToEntityAttribute(encoded)
        decoded shouldBeEqualTo sample
    }

    @Test
    fun `ZstdKryoObjectAsBase64StringConverter는 객체를 압축 직렬화하고 역직렬화한다`() {
        val converter = ZstdKryoObjectAsBase64StringConverter()
        val encoded = converter.convertToDatabaseColumn(sample)
        encoded.shouldNotBeNull()

        val decoded = converter.convertToEntityAttribute(encoded)
        decoded shouldBeEqualTo sample
    }

    @Test
    fun `다양한 타입의 객체를 직렬화하고 역직렬화한다`() {
        val converter = JdkObjectAsBase64StringConverter()

        // String
        val strEncoded = converter.convertToDatabaseColumn("test string")
        strEncoded.shouldNotBeNull()
        converter.convertToEntityAttribute(strEncoded) shouldBeEqualTo "test string"

        // Integer
        val intEncoded = converter.convertToDatabaseColumn(12345)
        intEncoded.shouldNotBeNull()
        converter.convertToEntityAttribute(intEncoded) shouldBeEqualTo 12345

        // List
        val listData = listOf("a", "b", "c")
        val listEncoded = converter.convertToDatabaseColumn(listData)
        listEncoded.shouldNotBeNull()
        converter.convertToEntityAttribute(listEncoded) shouldBeEqualTo listData
    }

    @Test
    fun `LZ4JdkObjectAsBase64StringConverter는 null을 null로 변환한다`() {
        val converter = LZ4JdkObjectAsBase64StringConverter()
        converter.convertToDatabaseColumn(null).shouldBeNull()
        converter.convertToEntityAttribute(null).shouldBeNull()
    }
}
