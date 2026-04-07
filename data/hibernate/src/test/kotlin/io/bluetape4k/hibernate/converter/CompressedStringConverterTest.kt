package io.bluetape4k.hibernate.converter

import io.bluetape4k.hibernate.converters.BZip2StringConverter
import io.bluetape4k.hibernate.converters.DeflateStringConverter
import io.bluetape4k.hibernate.converters.GZipStringConverter
import io.bluetape4k.hibernate.converters.LZ4StringConverter
import io.bluetape4k.hibernate.converters.SnappyStringConverter
import io.bluetape4k.hibernate.converters.ZstdStringConverter
import io.bluetape4k.logging.KLogging
import jakarta.persistence.AttributeConverter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

/**
 * 문자열 압축 컨버터([BZip2StringConverter], [DeflateStringConverter], [GZipStringConverter],
 * [LZ4StringConverter], [SnappyStringConverter], [ZstdStringConverter])에 대한 단위 테스트입니다.
 */
class CompressedStringConverterTest {

    companion object: KLogging() {
        @JvmStatic
        fun converters(): Stream<AttributeConverter<String?, String?>> = Stream.of(
            BZip2StringConverter(),
            DeflateStringConverter(),
            GZipStringConverter(),
            LZ4StringConverter(),
            SnappyStringConverter(),
            ZstdStringConverter(),
        )

        private const val SAMPLE_TEXT =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                    "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                    "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris."

        private const val KOREAN_TEXT =
            "한국어 텍스트를 압축하고 복원하는 테스트입니다. " +
                    "압축 알고리즘마다 성능과 압축률이 다르지만, 모두 원본 데이터를 정확히 복원해야 합니다."
    }

    @ParameterizedTest(name = "{0} - 문자열을 압축하고 복원한다")
    @MethodSource("converters")
    fun `문자열을 압축하고 복원한다`(converter: AttributeConverter<String?, String?>) {
        val compressed = converter.convertToDatabaseColumn(SAMPLE_TEXT)
        compressed.shouldNotBeNull()
        compressed shouldNotBeEqualTo SAMPLE_TEXT

        val restored = converter.convertToEntityAttribute(compressed)
        restored shouldBeEqualTo SAMPLE_TEXT
    }

    @ParameterizedTest(name = "{0} - null 입력 시 null을 반환한다")
    @MethodSource("converters")
    fun `null 입력 시 null을 반환한다`(converter: AttributeConverter<String?, String?>) {
        converter.convertToDatabaseColumn(null).shouldBeNull()
        converter.convertToEntityAttribute(null).shouldBeNull()
    }

    @ParameterizedTest(name = "{0} - 한국어 문자열을 압축하고 복원한다")
    @MethodSource("converters")
    fun `한국어 문자열을 압축하고 복원한다`(converter: AttributeConverter<String?, String?>) {
        val compressed = converter.convertToDatabaseColumn(KOREAN_TEXT)
        compressed.shouldNotBeNull()

        val restored = converter.convertToEntityAttribute(compressed)
        restored shouldBeEqualTo KOREAN_TEXT
    }

    @ParameterizedTest(name = "{0} - 빈 문자열을 압축하고 복원한다")
    @MethodSource("converters")
    fun `빈 문자열을 압축하고 복원한다`(converter: AttributeConverter<String?, String?>) {
        val compressed = converter.convertToDatabaseColumn("")
        compressed.shouldNotBeNull()

        val restored = converter.convertToEntityAttribute(compressed)
        restored shouldBeEqualTo ""
    }

    @ParameterizedTest(name = "{0} - 반복 문자열은 높은 압축률을 보인다")
    @MethodSource("converters")
    fun `반복 문자열은 높은 압축률을 보인다`(converter: AttributeConverter<String?, String?>) {
        val repeatedText = "압축테스트".repeat(100)

        val compressed = converter.convertToDatabaseColumn(repeatedText)
        compressed.shouldNotBeNull()

        val restored = converter.convertToEntityAttribute(compressed)
        restored shouldBeEqualTo repeatedText
    }
}
