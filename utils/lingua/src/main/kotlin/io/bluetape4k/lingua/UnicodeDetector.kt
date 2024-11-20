package io.bluetape4k.lingua

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import java.util.*

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
     */
    fun filterString(text: String, locale: Locale): CharArray {
        log.debug { "filter language[${locale.language}] chars..." }
        return text.mapNotNull { filterChar(it, locale) }.toCharArray()
    }

    /**
     * 문자가 [locale]에 해당하는 문자라라면 반환하고 아니면 null 을 반환합니다.
     */
    fun filterChar(char: Char, locale: Locale): Char? {
        if (char.isAscii) {
            return char
        }

        if (locale !in SupportedLanguages) {
            return null
        }

        val c = when {
            locale.language == "ko" && char.isKorean   -> char
            locale.language == "ja" && char.isJapanese -> char
            locale.language == "en" && char.isAscii    -> char
            locale.language == "zh" && char.isChinese  -> char
            locale.language == "th" && char.isThai     -> char
            else                                       -> null
        }
        log.trace { "char=$char, languge=${locale.language}, c=$c" }

        return c
    }

    fun containsAny(text: String, locale: Locale): Boolean {
        return filterString(text, locale).isNotEmpty()
    }

    fun containsAll(text: String, locale: Locale): Boolean {
        return filterString(text, locale).size == text.length
    }
}
