package io.bluetape4k.hibernate.converter

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import io.bluetape4k.hibernate.converters.LocaleAsStringConverter
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.util.*

class LocaleAsStringConverterTest {

    companion object: KLogging()

    private val converter = LocaleAsStringConverter()

    @Test
    fun `locale는 language tag로 저장하고 복원한다`() {
        val locale = Locale.KOREA

        val dbValue = converter.convertToDatabaseColumn(locale).shouldNotBeNull()
        dbValue shouldBeEqualTo "ko-KR"

        val restored = converter.convertToEntityAttribute(dbValue).shouldNotBeNull()
        restored shouldBeEqualTo locale
    }

    @Test
    fun `underscore legacy locale 문자열도 복원한다`() {
        val restored = converter.convertToEntityAttribute("en_US").shouldNotBeNull()

        restored.language shouldBeEqualTo "en"
        restored.country shouldBeEqualTo "US"
    }

    @Test
    fun `blank 입력은 null을 반환한다`() {
        converter.convertToEntityAttribute("   ").shouldBeNull()
    }
}
