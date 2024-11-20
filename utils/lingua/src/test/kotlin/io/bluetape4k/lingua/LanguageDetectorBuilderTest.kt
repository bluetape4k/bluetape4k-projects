package io.bluetape4k.lingua

import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test

class LanguageDetectorBuilderTest: AbstractLinguaTest() {

    companion object: KLogging()

    @Test
    fun `all language로부터 LanguageDetector를 생성할 수 있다`() {
        val detector = allLanguageDetector {
            withMinimumRelativeDistance(0.1)
            withPreloadedLanguageModels()
        }

        log.debug { "Detector: $detector" }
    }

    @Test
    fun `특정 언어를 제외한 LanguageDetector를 생성할 수 있다`() {
        val exceptLanguages = setOf(Language.ENGLISH, Language.KOREAN)
        val detector = allLanguageWithoutDetector(
            exceptLanguages
        ) {

            withMinimumRelativeDistance(0.1)
            withPreloadedLanguageModels()
            withLowAccuracyMode()
        }

        log.debug { "Detector: $detector" }
    }
}
