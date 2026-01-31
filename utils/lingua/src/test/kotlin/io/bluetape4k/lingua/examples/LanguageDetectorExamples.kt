package io.bluetape4k.lingua.examples

import com.github.pemistahl.lingua.api.IsoCode639_1
import com.github.pemistahl.lingua.api.IsoCode639_3
import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.lingua.AbstractLinguaTest
import io.bluetape4k.lingua.allLanguageDetector
import io.bluetape4k.lingua.languageDetectorOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class LanguageDetectorExamples: AbstractLinguaTest() {

    companion object: KLogging()

    fun textAndLanguage(): List<Arguments> = listOf(
        Arguments.of("Hello, World", Language.ENGLISH),
        Arguments.of("안녕하세요. Hello World", Language.KOREAN),
        Arguments.of("こんにちは", Language.JAPANESE),
        Arguments.of("你好", Language.CHINESE),
        Arguments.of("ในทางหลวงหมายเลข", Language.THAI),
        Arguments.of("Bonjour", Language.FRENCH),
        Arguments.of("σταμάτησε", Language.GREEK),
    )

    private val detectorForAllLanguages = allLanguageDetector {
        withPreloadedLanguageModels()
        withMinimumRelativeDistance(0.0)
    }

    private val detectorIsoCode639_1 = languageDetectorOf(
        IsoCode639_1.entries.toUnifiedSet()
    ) {
        withPreloadedLanguageModels()
    }

    private val detectorIsoCode639_3 = languageDetectorOf(
        IsoCode639_3.entries.toUnifiedSet()
    ) {
        withPreloadedLanguageModels()
    }

    @ParameterizedTest
    @MethodSource("textAndLanguage")
    fun `문장으로부터 언어를 검출한다`(text: String, expectedLanguage: Language) {
        log.debug { "Text: $text, expected language=$expectedLanguage" }
        detectorForAllLanguages.detectLanguageOf(text) shouldBeEqualTo expectedLanguage
    }

    @ParameterizedTest
    @MethodSource("textAndLanguage")
    fun `문장으로부터 IsoCode639_1 언어를 검출한다`(text: String, expectedLanguage: Language) {
        log.debug { "Text: $text, expected language=$expectedLanguage" }
        detectorIsoCode639_1.detectLanguageOf(text) shouldBeEqualTo expectedLanguage
    }

    @ParameterizedTest
    @MethodSource("textAndLanguage")
    fun `문장으로부터 IsoCode639_3 언어를 검출한다`(text: String, expectedLanguage: Language) {
        log.debug { "Text: $text, expected language=$expectedLanguage" }
        detectorIsoCode639_1.detectLanguageOf(text) shouldBeEqualTo expectedLanguage
    }

    @Test
    fun `긴 문장의 한국어를 검출한다`() {
        val korean = """
            [Lingua](https://github.com/pemistahl/lingua) 를 이용하여, 문장에서 언어를 자동 감지하는 기능을 제공합니다.
            """.trimIndent()

        detectorForAllLanguages.detectLanguageOf(korean) shouldBeEqualTo Language.KOREAN
    }

    @Test
    fun `긴 문장의 영어를 검출한다`() {
        val english = """
            In order to execute the steps below, you will need Java 8 or greater. 
            Even though the library itself runs on Java >= 6, 
            the FilesWriter classes make use of the java.nio api which was introduced with Java 8.
            """.trimIndent()

        detectorForAllLanguages.detectLanguageOf(english) shouldBeEqualTo Language.ENGLISH
    }

    @Test
    fun `긴 문장의 일본어를 검출한다`() {
        val japanese = """
            すみません。 いまなんじですか。
            """.trimIndent()

        detectorForAllLanguages.detectLanguageOf(japanese) shouldBeEqualTo Language.JAPANESE
    }

    @Test
    fun `긴 문장의 태국어를 검출한다`() {
        val thai = """
            ในทางหลวงหมายเลข 1 มีการจราจรแออัดมาก
            """.trimIndent()

        detectorForAllLanguages.detectLanguageOf(thai) shouldBeEqualTo Language.THAI
    }

    @Test
    fun `한국어와 태국어가 같이 있다면 한국어로 판단한다`() {
        val koreanAndThai = """
            안녕 ในทางหลวงหมายเลข 1 มีการจราจรแออัดมาก
            """.trimIndent()

        detectorForAllLanguages.detectLanguageOf(koreanAndThai) shouldBeEqualTo Language.KOREAN

        val thaiAndKorean = """
            ในทางหลวงหมายเลข 1 มีการจราจรแออัดมาก 안녕
            """.trimIndent()

        detectorForAllLanguages.detectLanguageOf(thaiAndKorean) shouldBeEqualTo Language.KOREAN
    }

    @Test
    fun `한국어와 영어가 섞인 경우 한국어로 판단한다`() {
        val korean = """
            Lingua를 이용하여, 문장에서 Language를 자동 감지하는 기능을 제공합니다.
            """.trimIndent()

        detectorForAllLanguages.detectLanguageOf(korean) shouldBeEqualTo Language.KOREAN
    }

    @Test
    fun `한국어와 일본어가 섞인 경우 일본어로 판단한다`() {
        val japaneseFirst = """
            すみません。 いまなんじですか。 (죄송합니다. 지금 몇 시인가요?)
            """.trimIndent()

        detectorForAllLanguages.detectLanguageOf(japaneseFirst) shouldBeEqualTo Language.JAPANESE

        val koreanFirst = """
            죄송합니다. 지금 몇 시인가요? (すみません。 いまなんじですか。)
            """.trimIndent()

        detectorForAllLanguages.detectLanguageOf(koreanFirst) shouldBeEqualTo Language.JAPANESE
    }
}
