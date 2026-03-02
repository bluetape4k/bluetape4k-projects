package io.bluetape4k.lingua

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import java.util.*

/**
 * 유니코드 블록 규칙으로 문자열의 언어별 문자 포함 여부를 판정하는 도우미입니다.
 *
 * ## 동작/계약
 * - [SupportedLanguages]에 포함된 로케일만 필터링 대상으로 처리합니다.
 * - ASCII 문자는 모든 로케일에서 통과됩니다.
 * - 필터링은 새 배열을 반환하며 입력 문자열을 mutate하지 않습니다.
 *
 * ```kotlin
 * val detector = UnicodeDetector()
 * val chars = detector.filterString("안녕하세요 hello", Locale.KOREAN)
 * // chars.isNotEmpty() == true
 * ```
 */
class UnicodeDetector {

    companion object: KLogging() {
        val SupportedLanguages: List<Locale> = listOf(
            Locale.KOREAN,
            Locale.JAPANESE,
            Locale.ENGLISH,
            Locale.CHINESE,
            Locale.of("th")
        )
    }

    /**
     * 문자열에서 [locale]에 해당하는 문자들만 필터링합니다.
     *
     * ## 동작/계약
     * - 각 문자를 [filterChar]로 판정해 null이 아닌 문자만 반환합니다.
     * - 결과는 새 [CharArray]로 생성됩니다.
     */
    fun filterString(text: String, locale: Locale): CharArray {
        log.debug { "filter language[${locale.language}] chars..." }
        return text.mapNotNull { filterChar(it, locale) }.toCharArray()
    }

    /**
     * 문자가 [locale]에 해당하는 문자라라면 반환하고 아니면 null 을 반환합니다.
     *
     * ## 동작/계약
     * - ASCII 문자는 즉시 반환합니다.
     * - 지원하지 않는 [locale]이면 null을 반환합니다.
     * - 로케일 언어 코드별 유니코드 범위 판정 결과를 반환합니다.
     */
    fun filterChar(char: Char, locale: Locale): Char? {
        if (char.isAscii) {
            return char
        }

        if (locale !in SupportedLanguages) {
            return null
        }

        val c = when (locale.language) {
            "ko" if char.isKorean -> char
            "ja" if char.isJapanese -> char
            "en" if char.isAscii -> char
            "zh" if char.isChinese -> char
            "th" if char.isThai -> char
            else -> null
        }
        log.trace { "char=$char, languge=${locale.language}, c=$c" }

        return c
    }

    /**
     * 텍스트에 [locale] 문자 하나라도 포함되면 `true`를 반환합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [filterString] 결과 길이로 판정합니다.
     */
    fun containsAny(text: String, locale: Locale): Boolean {
        return filterString(text, locale).isNotEmpty()
    }

    /**
     * 텍스트의 모든 문자가 [locale] 필터를 통과하면 `true`를 반환합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [filterString] 결과 길이와 원문 길이를 비교합니다.
     */
    fun containsAll(text: String, locale: Locale): Boolean {
        return filterString(text, locale).size == text.length
    }
}
