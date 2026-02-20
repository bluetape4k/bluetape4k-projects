package io.bluetape4k.support.i18n

import io.bluetape4k.support.requireNotBlank
import java.util.*

fun localeOf(language: String): Locale = Locale.of(language)

fun localeOf(language: String, country: String): Locale = Locale.of(language, country)

fun localeOf(language: String, country: String, variant: String): Locale = Locale.of(language, country, variant)

/**
 * [Locale] 이 시스템 기본 Locale 인지 확인합니다.
 */
fun Locale.isDefault(): Boolean = this == Locale.getDefault()

/**
 * [Locale]을 반환하던가, null 이면 시스템 기본 Locale을 반환합니다.
 */
fun Locale?.orDefault(): Locale = this ?: Locale.getDefault()

/**
 * [Locale] 의 부모 Locale을 구합니다.
 *
 * ```
 * Locale("en", "US", "WIN").getParentOrNull() // en_US
 * Locale("en", "US").getParentOrNull()        // en
 * Locale("en").getParentOrNull()              // null
 * ```
 */
fun Locale.getParentOrNull(): Locale? = when {
    variant.isNotEmpty() && (language.isNotEmpty() || country.isNotEmpty()) -> Locale.of(language, country)
    country.isNotEmpty() -> Locale.of(language)
    else                 -> null
}

/**
 * [Locale] 자신과 모든 부모 Locale 들을 구합니다.
 *
 * ```
 * Locale("en", "US", "WIN").getParentList() // [en_US_WIN, en_US, en]
 * Locale("en", "US").getParentList()        // [en_US, en]
 * Locale("en").getParentList()              // [en]
 * ```
 */
fun Locale.getParentList(): List<Locale> = buildList {
    var current: Locale? = this@getParentList
    while (current != null) {
        add(current)
        current = current.getParentOrNull()
    }
}

/**
 * Locale에 따른 리소스 번들 파일명 목록을 생성합니다.
 * language, country, variant를 조합하여 가장 구체적인 파일명부터 basename까지 반환합니다.
 *
 * ```
 * Locale("en", "US", "WIN").calculateFilenames("messages") // [messages_en_US_WIN, messages_en_US, messages_en, messages]
 * Locale("en", "US").calculateFilenames("messages")        // [messages_en_US, messages_en, messages]
 * Locale("en").calculateFilenames("messages")              // [messages_en, messages]
 * ```
 *
 * @param basename 번들의 기본 이름
 * @return 확인할 파일명 목록 (가장 구체적인 것이 먼저, basename이 마지막)
 */
fun Locale.calculateFilenames(basename: String): List<String> {
    basename.requireNotBlank("basename")
    val results = ArrayList<String>(4)

    val language = this.language
    val country = this.country
    val variant = this.variant

    val temp = StringBuilder(basename).append("_")

    if (language.isNotEmpty()) {
        temp.append(language)
        results.add(temp.toString())
    }
    temp.append("_")

    if (country.isNotEmpty()) {
        temp.append(country)
        results.add(temp.toString())
    }

    if (variant.isNotEmpty() && language.isNotEmpty() && country.isNotEmpty()) {
        temp.append("_").append(variant)
        results.add(temp.toString())
    }
    results.reverse()
    results.add(basename)

    return results
}
