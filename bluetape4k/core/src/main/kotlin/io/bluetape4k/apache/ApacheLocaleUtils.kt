package io.bluetape4k.apache

import org.apache.commons.lang3.LocaleUtils
import java.util.*

/**
 * 사용 가능한 [Locale] 목록을 반환합니다.
 */
fun availableLocaleList(): List<Locale> = LocaleUtils.availableLocaleList()

/**
 * [predicate]를 만족하는 [Locale] 목록을 반환합니다.
 */
fun availableLocaleList(predicate: (Locale) -> Boolean): List<Locale> =
    availableLocaleList().filter(predicate)

/**
 * 사용 가능한 [Locale] Set을 반환합니다.
 */
fun availableLocaleSet(): Set<Locale> = LocaleUtils.availableLocaleSet()

/**
 * [predicate]를 만족하는 [Locale] Set을 반환합니다.
 */
fun availableLocaleSet(predicate: (Locale) -> Boolean): Set<Locale> =
    LocaleUtils.availableLocaleSet().filter(predicate).toSet()

/**
 * [language]를 사용하는 국가들의 [Locale]을 반환합니다.
 */
fun countriesByLanguage(language: String): List<Locale> =
    LocaleUtils.countriesByLanguage(language)

/**
 * locale이 사용 가능한지 확인합니다.
 */
fun Locale.isAvailable(): Boolean = LocaleUtils.isAvailableLocale(this)

/**
 * locale이 Language가 결정되지 않은지 확인합니다.
 */
fun Locale.isLanguageUndetermined(): Boolean = LocaleUtils.isLanguageUndetermined(this)

/**
 * 국가 코드 [countryCode]를 사용하는 언어들의 [Locale]을 반환합니다.
 */
fun languageByCountry(countryCode: String): List<Locale> = LocaleUtils.languagesByCountry(countryCode)

/**
 * 로케일 검색 시 탐색할 로케일 목록을 반환합니다.
 *
 * ```
 * localeLookupList(Locale("fr", "CA", "xxx"))
 *   = [Locale("fr", "CA", "xxx"), Locale("fr", "CA"), Locale("fr")]
 * ```
 *
 * @receiver 검색을 시작할 기준 로케일
 * @return 탐색 순서로 정렬된 [Locale] 목록 (수정 불가)
 */
fun Locale.localeLookupList(defaultLocale: Locale = this): List<Locale> =
    LocaleUtils.localeLookupList(this, defaultLocale)

/**
 * Locale을 나타내는 문자열을 [Locale]로 변환합니다.
 */
fun String.toLocale(): Locale = LocaleUtils.toLocale(this)
