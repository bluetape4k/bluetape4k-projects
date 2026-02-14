package io.bluetape4k.hibernate.converter

import io.bluetape4k.hibernate.converters.LocaleAsStringConverter
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import java.util.Locale

class LocaleAsStringConverterTest {

    private val converter = LocaleAsStringConverter()

    @Test
    fun `locale는 language tag로 저장하고 복원한다`() {
        val locale = Locale.KOREA

        val dbValue = converter.convertToDatabaseColumn(locale)
        dbValue shouldBeEqualTo "ko-KR"

        val restored = converter.convertToEntityAttribute(dbValue)
        restored shouldBeEqualTo locale
    }

    @Test
    fun `underscore legacy locale 문자열도 복원한다`() {
        val restored = converter.convertToEntityAttribute("en_US")

        restored?.language shouldBeEqualTo "en"
        restored?.country shouldBeEqualTo "US"
    }

    @Test
    fun `blank 입력은 null을 반환한다`() {
        converter.convertToEntityAttribute("   ").shouldBeNull()
    }
}
