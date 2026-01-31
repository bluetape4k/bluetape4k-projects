package io.bluetape4k.lingua

import com.github.pemistahl.lingua.api.IsoCode639_1
import com.github.pemistahl.lingua.api.IsoCode639_3
import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetector
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import io.bluetape4k.collections.eclipse.toUnifiedSet

/**
 * 모든 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ```
 * val detector = allLanguageDetector {
 *      withPreloadedLanguageModels()
 *      withMinimumRelativeDistance(0.0)
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param builder [LanguageDetectorBuilder] 초기화 람다
 * @return [LanguageDetector] 인스턴스
 */
inline fun allLanguageDetector(
    @BuilderInference builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromAllLanguages()
        .apply(builder)
        .build()

/**
 * 지정된 [languages]를 제외한 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ```
 * val exceptLanguages = setOf(Language.GERMAN, Language.THAI)
 * val detector = allLanguageWithoutDetector(exceptLanguages) {
 *    withPreloadedLanguageModels()
 *    withMinimumRelativeDistance(0.0)
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param languages 제외할 언어
 * @param builder [LanguageDetectorBuilder] 초기화 람다
 * @return [LanguageDetector] 인스턴스
 */
inline fun allLanguageWithoutDetector(
    languages: Set<Language>,
    @BuilderInference builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromAllLanguagesWithout(*languages.toTypedArray())
        .apply(builder)
        .build()


/**
 * 모든 말로된 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ```
 * val detector = allSpokenLanguageDetector {
 *     withPreloadedLanguageModels()
 *     withMinimumRelativeDistance(0.0)
 *     withLowAccuracyMode()
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param builder [LanguageDetectorBuilder] 초기화 람다
 * @return [LanguageDetector] 인스턴스
 */
inline fun allSpokenLanguageDetector(
    @BuilderInference builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder.fromAllSpokenLanguages().apply(builder).build()

/**
 * 지정된 [languages] 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ```
 * val languages = setOf(Language.ENGLISH, Language.KOREAN)
 * val detector = languageDetectorOf(languages) {
 *    withPreloadedLanguageModels()
 *    withMinimumRelativeDistance(0.0)
 *    withLowAccuracyMode()
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param languages 검출할 언어
 * @param builder [LanguageDetectorBuilder] 초기화 람다
 * @return [LanguageDetector] 인스턴스
 *
 */
@JvmName("languageDetectorOfLanguage")
inline fun languageDetectorOf(
    languages: Set<Language>,
    @BuilderInference builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromLanguages(*languages.toTypedArray())
        .apply(builder)
        .build()

/**
 * 지정된 [languages] 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ```
 * val languages = setOf(Language.ENGLISH, Language.KOREAN)
 * val detector = languageDetectorOf(
 *      languages,
 *      minimulRelativeDistance = 0.0,
 *      isEveryLangageModelPreloaded = true,
 *      isLowAccuracyModeEnabled = false
 * )
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param languages 검출할 언어
 * @param minimulRelativeDistance 최소 상대 거리
 * @param isEveryLangageModelPreloaded 모든 언어 모델을 미리 로드할지 여부
 * @param isLowAccuracyModeEnabled 저 정확도 모드를 사용할지 여부
 * @return [LanguageDetector] 인스턴스
 *
 */
fun languageDetectorOf(
    languages: Set<Language> = Language.all().toUnifiedSet(),
    minimulRelativeDistance: Double = 0.0,
    isEveryLangageModelPreloaded: Boolean = true,
    isLowAccuracyModeEnabled: Boolean = false,
): LanguageDetector =
    languageDetectorOf(languages) {
        withMinimumRelativeDistance(minimulRelativeDistance)
        if (isEveryLangageModelPreloaded) {
            withPreloadedLanguageModels()
        }
        if (isLowAccuracyModeEnabled) {
            withLowAccuracyMode()
        }
    }

/**
 * 지정된 [isoCodes] 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ```
 * val isoCodes = setOf(IsoCode639_1.EN, IsoCode639_1.KO)
 * val detector = languageDetectorOf(isoCodes) {
 *    withPreloadedLanguageModels()
 *    withMinimumRelativeDistance(0.0)
 *    withLowAccuracyMode()
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param isoCodes 검출할 언어
 * @param builder [LanguageDetectorBuilder] 초기화 람다
 * @return [LanguageDetector] 인스턴스
 */
@JvmName("languageDetectorOfIsoCode639_1")
inline fun languageDetectorOf(
    isoCodes: Set<IsoCode639_1>,
    @BuilderInference builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromIsoCodes639_1(*isoCodes.toTypedArray())
        .apply(builder)
        .build()

/**
 * 지정된 [isoCodes] 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ```
 * val isoCodes = setOf(IsoCode639_3.EN, IsoCode639_3.KO)
 * val detector = languageDetectorOf(isoCodes) {
 *    withPreloadedLanguageModels()
 *    withMinimumRelativeDistance(0.0)
 *    withLowAccuracyMode()
 * }
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param isoCodes 검출할 언어
 * @param builder [LanguageDetectorBuilder] 초기화 람다
 * @return [LanguageDetector] 인스턴스
 */
@JvmName("languageDetectorOfIsoCode639_3")
inline fun languageDetectorOf(
    isoCodes: Set<IsoCode639_3>,
    @BuilderInference builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromIsoCodes639_3(*isoCodes.toTypedArray())
        .apply(builder)
        .build()
