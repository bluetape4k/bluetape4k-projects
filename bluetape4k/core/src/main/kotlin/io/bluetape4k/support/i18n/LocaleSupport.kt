package io.bluetape4k.support.i18n

import io.bluetape4k.logging.KotlinLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.support.assertNotBlank
import java.util.*

private val log = KotlinLogging.logger { }

/**
 * [Locale] 이 시스템 기본 Locale 인지 확인합니다.
 */
fun Locale.isDefault(): Boolean = Locale.getDefault() == this

/**
 * [Locale]을 반환하던가, null 이면 시스템 기본 Locale을 반환합니다.
 */
fun Locale?.orDefault(): Locale = this ?: Locale.getDefault()

/**
 * [Locale] 의 부모 Locale을 구합니다.
 */
@Deprecated("use getParent()", replaceWith = ReplaceWith("getParentOrNull()"))
val Locale.parent: Locale?
    get() = when {
        variant.isNotEmpty() && (language.isNotEmpty() || country.isNotEmpty()) -> Locale.of(language, country)
        country.isNotEmpty()                                                    -> Locale.of(language)
        else                                                                    -> null
    }

/**
 * [Locale] 의 모든 부모 Locale 들을 구합니다.
 */
@Deprecated("Use getParents() methods", replaceWith = ReplaceWith("getParentList()"))
val Locale.parents: List<Locale>
    get() {
        val result = mutableListOf<Locale>()
        var current: Locale? = this
        while (current != null) {
            result.add(current)
            @Suppress("DEPRECATION")
            current = current.parent
        }
        return result
    }

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
    country.isNotEmpty()                                                    -> Locale.of(language)
    else                                                                    -> null
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
fun Locale.getParentList(): List<Locale> {
    val result = mutableListOf<Locale>()
    var current: Locale? = this
    while (current != null) {
        result.add(current)
        current = current.getParentOrNull()
    }
    return result
}


/**
 * Calculate the filenames for the given bundle basename and Locale,
 * appending language code, country code, and variant code.
 * E.g.: basename "messages", Locale "de_AT_oo" -> "messages_de_AT_OO",
 * "messages_de_AT", "messages_de".
 *
 * Follows the rules defined by [Locale.toString]
 *
 * ```
 * Locale("en", "US", "WIN").calculateFilenames("messages") // [messages_en_US_WIN, messages_en_US, messages_en]
 * Locale("en", "US").calculateFilenames("messages")        // [messages_en_US, messages_en]
 * Locale("en").calculateFilenames("messages")              // [messages_en]
 * ```
 *
 * @param basename the basename of the bundle
 * @return the List of filenames to check
 */
fun Locale.calculateFilenames(basename: String): List<String> {

    basename.assertNotBlank("basename")
    log.trace { "Locale에 해당하는 파일명을 조합합니다. basename=$basename, locale=$this" }

    val results = mutableListOf<String>()

    val language = this.language
    val country = this.country
    val variant = this.variant

    log.trace { "language=$language, country=$country, variant=$variant" }

    val temp = StringBuilder(basename)
    temp.append("_")

    if (language.isNotEmpty()) {
        temp.append(language)
        results.add(0, temp.toString())
    }
    temp.append("_")

    if (country.isNotEmpty()) {
        temp.append(country)
        results.add(0, temp.toString())
    }

    if (variant.isNotEmpty() && (language.isNotEmpty() || country.isNotEmpty())) {
        temp.append("_").append(variant)
        results.add(0, temp.toString())
    }
    results.add(basename)

    log.trace { "Locale에 해당하는 파일명을 조합했습니다. basename=$basename, locale=$this, results=$results" }

    return results
}
