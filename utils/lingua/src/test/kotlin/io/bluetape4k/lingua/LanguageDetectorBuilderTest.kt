package io.bluetape4k.lingua

import com.github.pemistahl.lingua.api.IsoCode639_1
import com.github.pemistahl.lingua.api.IsoCode639_3
import com.github.pemistahl.lingua.api.Language
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

class LanguageDetectorBuilderTest: AbstractLinguaTest() {

    companion object: KLogging()

    @Test
    fun `all language로부터 LanguageDetector를 생성할 수 있다`() {
        val detector = allLanguageDetector {
            withMinimumRelativeDistance(0.1)
            withPreloadedLanguageModels()
        }

        detector.shouldNotBeNull()
        log.debug { "Detector: $detector" }
    }

    @Test
    fun `특정 언어를 제외한 LanguageDetector를 생성할 수 있다`() {
        val exceptLanguages = setOf(Language.ENGLISH, Language.KOREAN)
        val detector = allLanguageWithoutDetector(exceptLanguages) {
            withMinimumRelativeDistance(0.1)
            withPreloadedLanguageModels()
            withLowAccuracyMode()
        }

        detector.shouldNotBeNull()
        log.debug { "Detector: $detector" }
    }

    @Test
    fun `Language Set으로 LanguageDetector를 생성할 수 있다`() {
        val languages = setOf(Language.ENGLISH, Language.KOREAN, Language.JAPANESE)
        val detector = languageDetectorOf(languages) {
            withMinimumRelativeDistance(0.0)
            withPreloadedLanguageModels()
        }

        detector.shouldNotBeNull()
        log.debug { "Detector: $detector" }
    }

    @Test
    fun `IsoCode639_1 Set으로 LanguageDetector를 생성할 수 있다`() {
        val isoCodes = setOf(IsoCode639_1.EN, IsoCode639_1.KO, IsoCode639_1.JA)
        val detector = languageDetectorOf(isoCodes) {
            withMinimumRelativeDistance(0.0)
            withPreloadedLanguageModels()
        }

        detector.shouldNotBeNull()
        log.debug { "Detector: $detector" }
    }

    @Test
    fun `IsoCode639_3 Set으로 LanguageDetector를 생성할 수 있다`() {
        val isoCodes = setOf(IsoCode639_3.ENG, IsoCode639_3.KOR, IsoCode639_3.JPN)
        val detector = languageDetectorOf(isoCodes) {
            withMinimumRelativeDistance(0.0)
            withPreloadedLanguageModels()
        }

        detector.shouldNotBeNull()
        log.debug { "Detector: $detector" }
    }

    @Test
    fun `convenience overload으로 LanguageDetector를 생성할 수 있다`() {
        val languages = setOf(Language.ENGLISH, Language.KOREAN)
        val detector = languageDetectorOf(
            languages = languages,
            minimumRelativeDistance = 0.0,
            isEveryLanguageModelPreloaded = true,
            isLowAccuracyModeEnabled = false
        )

        detector.shouldNotBeNull()
        log.debug { "Detector: $detector" }
    }
}
