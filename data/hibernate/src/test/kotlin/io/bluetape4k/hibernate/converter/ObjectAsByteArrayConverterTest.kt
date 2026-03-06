package io.bluetape4k.hibernate.converter

import io.bluetape4k.hibernate.converters.ForyObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.JdkObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.KryoObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.LZ4ForyObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.LZ4JdkObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.LZ4KryoObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.SnappyForyObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.SnappyJdkObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.SnappyKryoObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.ZstdForyObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.ZstdJdkObjectAsByteArrayConverter
import io.bluetape4k.hibernate.converters.ZstdKryoObjectAsByteArrayConverter
import io.bluetape4k.logging.KLogging
import jakarta.persistence.AttributeConverter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.Serializable
import java.util.stream.Stream

/**
 * 객체 직렬화 바이트 배열 컨버터에 대한 단위 테스트입니다.
 *
 * Jdk, Kryo, Apache Fory 직렬화와 LZ4/Snappy/Zstd 압축 조합을 검증합니다.
 */
class ObjectAsByteArrayConverterTest {

    companion object: KLogging() {
        @JvmStatic
        fun converters(): Stream<AttributeConverter<Any?, ByteArray?>> = Stream.of(
            JdkObjectAsByteArrayConverter(),
            LZ4JdkObjectAsByteArrayConverter(),
            SnappyJdkObjectAsByteArrayConverter(),
            ZstdJdkObjectAsByteArrayConverter(),
            KryoObjectAsByteArrayConverter(),
            LZ4KryoObjectAsByteArrayConverter(),
            SnappyKryoObjectAsByteArrayConverter(),
            ZstdKryoObjectAsByteArrayConverter(),
            ForyObjectAsByteArrayConverter(),
            LZ4ForyObjectAsByteArrayConverter(),
            SnappyForyObjectAsByteArrayConverter(),
            ZstdForyObjectAsByteArrayConverter(),
        )
    }

    /**
     * 테스트에 사용할 직렬화 가능한 데이터 클래스입니다.
     */
    data class SampleData(
        val name: String,
        val value: Int,
        val tags: List<String> = emptyList(),
    ): Serializable

    @ParameterizedTest(name = "{0} - 객체를 직렬화하고 역직렬화한다")
    @MethodSource("converters")
    fun `객체를 직렬화하고 역직렬화한다`(converter: AttributeConverter<Any?, ByteArray?>) {
        val original = SampleData("test", 42, listOf("tag1", "tag2"))

        val serialized = converter.convertToDatabaseColumn(original)
        serialized.shouldNotBeNull()

        val deserialized = converter.convertToEntityAttribute(serialized)
        deserialized shouldBeEqualTo original
    }

    @ParameterizedTest(name = "{0} - null 입력 시 null을 반환한다")
    @MethodSource("converters")
    fun `null 입력 시 null을 반환한다`(converter: AttributeConverter<Any?, ByteArray?>) {
        converter.convertToDatabaseColumn(null).shouldBeNull()
        converter.convertToEntityAttribute(null).shouldBeNull()
    }

    @ParameterizedTest(name = "{0} - 중첩 객체를 직렬화하고 역직렬화한다")
    @MethodSource("converters")
    fun `중첩 객체를 직렬화하고 역직렬화한다`(converter: AttributeConverter<Any?, ByteArray?>) {
        val inner = SampleData("inner", 10)
        val outer = SampleData("outer", 20, listOf(inner.name))

        val serialized = converter.convertToDatabaseColumn(outer)
        serialized.shouldNotBeNull()

        val deserialized = converter.convertToEntityAttribute(serialized)
        deserialized shouldBeEqualTo outer
    }

    @ParameterizedTest(name = "{0} - 빈 컬렉션을 가진 객체를 직렬화하고 역직렬화한다")
    @MethodSource("converters")
    fun `빈 컬렉션을 가진 객체를 직렬화하고 역직렬화한다`(converter: AttributeConverter<Any?, ByteArray?>) {
        val obj = SampleData("empty-tags", 0, emptyList())

        val serialized = converter.convertToDatabaseColumn(obj)
        serialized.shouldNotBeNull()

        val deserialized = converter.convertToEntityAttribute(serialized)
        deserialized shouldBeEqualTo obj
    }

    @ParameterizedTest(name = "{0} - 한국어 문자열을 포함한 객체를 직렬화하고 역직렬화한다")
    @MethodSource("converters")
    fun `한국어 문자열을 포함한 객체를 직렬화하고 역직렬화한다`(converter: AttributeConverter<Any?, ByteArray?>) {
        val obj = SampleData("한국어이름", 99, listOf("태그1", "태그2"))

        val serialized = converter.convertToDatabaseColumn(obj)
        serialized.shouldNotBeNull()

        val deserialized = converter.convertToEntityAttribute(serialized)
        deserialized shouldBeEqualTo obj
    }
}
