package io.bluetape4k.lingua

import com.github.pemistahl.lingua.api.IsoCode639_1
import com.github.pemistahl.lingua.api.IsoCode639_3
import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.LanguageDetector
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder

private val mixedLanguageTokenRegex = Regex("\\p{L}+(?:['’-]\\p{L}+)*")
private val mixedLanguagePhraseDelimiterRegex = Regex("[.!?\n]+")

private val preferredLatinLanguages: Set<Language> = setOf(
    Language.ENGLISH,
    Language.SPANISH,
    Language.FRENCH,
    Language.GERMAN,
    Language.PORTUGUESE,
    Language.ITALIAN,
    Language.DUTCH,
)

private const val preferredLatinCandidateScanDepth = 10
private const val shortLatinTokenLength = 5
private const val preferredLatinConfidenceThreshold = 0.80


/**
 * 모든 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ## 동작/계약
 * - [LanguageDetectorBuilder.fromAllLanguages] 경로로 detector를 생성합니다.
 * - [builder] 설정을 적용한 새 detector 인스턴스를 반환합니다.
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
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromAllLanguages()
        .apply(builder)
        .build()

/**
 * 지정된 [languages]를 제외한 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ## 동작/계약
 * - [languages]를 제외한 전체 언어 집합으로 detector를 구성합니다.
 * - [builder] 설정을 적용한 새 detector 인스턴스를 반환합니다.
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
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromAllLanguagesWithout(*languages.toTypedArray())
        .apply(builder)
        .build()


/**
 * 모든 말로된 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ## 동작/계약
 * - [LanguageDetectorBuilder.fromAllSpokenLanguages] 경로를 사용합니다.
 * - [builder]를 적용한 새 detector를 반환합니다.
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
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder.fromAllSpokenLanguages().apply(builder).build()

/**
 * 지정된 [languages] 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ## 동작/계약
 * - 전달된 언어 집합만 대상으로 detector를 구성합니다.
 * - [builder] 설정을 반영한 새 detector를 반환합니다.
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
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromLanguages(*languages.toTypedArray())
        .apply(builder)
        .build()

/**
 * 지정된 [languages] 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ## 동작/계약
 * - [languageDetectorOf] DSL 버전에 위임해 detector를 생성합니다.
 * - preload/low-accuracy 옵션은 boolean 인자에 따라 선택 적용됩니다.
 *
 * ```
 * val languages = setOf(Language.ENGLISH, Language.KOREAN)
 * val detector = languageDetectorOf(
 *      languages,
 *      minimumRelativeDistance = 0.0,
 *      isEveryLanguageModelPreloaded = true,
 *      isLowAccuracyModeEnabled = false
 * )
 *
 * detector.detectLanguageOf("Hello, World") shouldBeEqualTo Language.ENGLISH
 * detector.detectLanguageOf("안녕하세요.") shouldBeEqualTo Language.KOREAN
 * ```
 *
 * @param languages 검출할 언어
 * @param minimumRelativeDistance 최소 상대 거리
 * @param isEveryLanguageModelPreloaded 모든 언어 모델을 미리 로드할지 여부
 * @param isLowAccuracyModeEnabled 저 정확도 모드를 사용할지 여부
 * @return [LanguageDetector] 인스턴스
 *
 */
fun languageDetectorOf(
    languages: Set<Language> = Language.all().toSet(),
    minimumRelativeDistance: Double = 0.0,
    isEveryLanguageModelPreloaded: Boolean = true,
    isLowAccuracyModeEnabled: Boolean = false,
): LanguageDetector =
    languageDetectorOf(languages) {
        withMinimumRelativeDistance(minimumRelativeDistance)
        if (isEveryLanguageModelPreloaded) {
            withPreloadedLanguageModels()
        }
        if (isLowAccuracyModeEnabled) {
            withLowAccuracyMode()
        }
    }

/**
 * 지정된 [isoCodes] 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ## 동작/계약
 * - ISO 639-1 코드 집합으로 detector를 구성합니다.
 * - [builder] 설정을 적용한 새 detector를 반환합니다.
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
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromIsoCodes639_1(*isoCodes.toTypedArray())
        .apply(builder)
        .build()

/**
 * 지정된 [isoCodes] 언어를 검출하는 [LanguageDetector]를 생성합니다.
 *
 * ## 동작/계약
 * - ISO 639-3 코드 집합으로 detector를 구성합니다.
 * - [builder] 설정을 적용한 새 detector를 반환합니다.
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
    builder: LanguageDetectorBuilder.() -> Unit,
): LanguageDetector =
    LanguageDetectorBuilder
        .fromIsoCodes639_3(*isoCodes.toTypedArray())
        .apply(builder)
        .build()

/**
 * 텍스트에서 검출된 모든 언어를 집합으로 반환합니다.
 *
 * ## 동작/계약
 * - 공백 입력은 `emptySet()`을 반환합니다.
 * - Unicode letter 토큰별 [detectLanguageOf] 결과에서 `UNKNOWN`을 제거한 집합을 반환합니다.
 * - 짧은 Latin 토큰은 Lingua가 `Hello -> SOTHO`처럼 오탐할 수 있어, 상위 confidence 후보가 모호할 때만 제한적으로 보정합니다.
 * - usable token 결과가 없으면 전체 입력에 대한 [detectLanguageOf] 결과를 singleton set으로 반환합니다.
 * - detector 생성 비용이 있으므로 호출마다 새 detector를 만들지 말고 재사용하는 것을 권장합니다.
 *
 * ```
 * val detector = allLanguageDetector {
 *     withMinimumRelativeDistance(0.0)
 * }
 *
 * detector.detectAllLanguagesOf("Hello 안녕") shouldBeEqualTo setOf(Language.ENGLISH, Language.KOREAN)
 * ```
 */
fun LanguageDetector.detectAllLanguagesOf(text: String): Set<Language> {
    if (text.isBlank()) {
        return emptySet()
    }

    val detected = linkedSetOf<Language>()

    mixedLanguagePhraseDelimiterRegex
        .split(text)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { phrase ->
            if (phrase.isLatinPhrase()) {
                detectLanguageOfLatinPhrase(phrase)?.let { detected += it }
            } else {
                mixedLanguageTokenRegex
                    .findAll(phrase)
                    .map { it.value }
                    .mapNotNull { token -> detectLanguageOfToken(token) }
                    .forEach { detected += it }
            }
        }

    if (detected.isNotEmpty()) {
        return detected
    }

    return detectLanguageOf(text)
        .takeIf { it != Language.UNKNOWN }
        ?.let { setOf(it) }
        ?: emptySet()
}

private fun LanguageDetector.detectLanguageOfLatinPhrase(phrase: String): Language? {
    val detected = detectLanguageOf(phrase)
    if (detected in preferredLatinLanguages) {
        return detected
    }

    return computeLanguageConfidenceValues(phrase).entries.asSequence()
        .take(preferredLatinCandidateScanDepth)
        .firstOrNull { it.key in preferredLatinLanguages && it.value >= preferredLatinConfidenceThreshold }
        ?.key
        ?: detected.takeIf { it != Language.UNKNOWN }
}

private fun LanguageDetector.detectLanguageOfToken(token: String): Language? {
    val detected = detectLanguageOf(token)
    if (detected == Language.UNKNOWN) {
        return null
    }
    if (!token.isLatinToken()) {
        return detected
    }
    if (token.length == 1) {
        return null
    }
    if (!token.isShortLatinToken()) {
        return detected.takeIf { it in preferredLatinLanguages }
    }
    if (detected in preferredLatinLanguages) {
        return detected
    }

    val candidates = computeLanguageConfidenceValues(token).entries
        .take(preferredLatinCandidateScanDepth)
        .toList()
    val englishCandidate = candidates.firstOrNull { it.key == Language.ENGLISH }
    if (token.isAsciiWord() && englishCandidate != null && englishCandidate.value >= preferredLatinConfidenceThreshold) {
        return Language.ENGLISH
    }

    return candidates.firstOrNull { it.key in preferredLatinLanguages }?.key
}

private fun String.isShortLatinToken(): Boolean =
    length <= shortLatinTokenLength && isLatinToken()

private fun String.isLatinToken(): Boolean = all { it.isLatin }

private fun String.isLatinPhrase(): Boolean =
    filterNot(Char::isWhitespace).all { it.isLatin || it == '\'' || it == '’' || it == '-' }

private fun String.isAsciiWord(): Boolean = all(Char::isAscii)
