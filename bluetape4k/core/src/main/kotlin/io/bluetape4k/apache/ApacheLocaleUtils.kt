package io.bluetape4k.apache

import org.apache.commons.lang3.LocaleUtils
import java.util.*

/**
 * 사용 가능한 [Locale] 목록을 반환합니다.
 *
 * ```kotlin
 * val locales = availableLocaleList()  // JVM이 지원하는 모든 Locale 목록
 * ```
 *
 * @return 사용 가능한 [Locale] 목록
 * @see org.apache.commons.lang3.LocaleUtils.availableLocaleList
 */
fun availableLocaleList(): List<Locale> = LocaleUtils.availableLocaleList()

/**
 * [predicate]를 만족하는 [Locale] 목록을 반환합니다.
 *
 * ```kotlin
 * val koLocales = availableLocaleList { it.language == "ko" }
 * // [ko, ko_KR, ...]
 * ```
 *
 * @param predicate 필터 조건
 * @return 조건을 만족하는 [Locale] 목록
 * @see org.apache.commons.lang3.LocaleUtils.availableLocaleList
 */
fun availableLocaleList(predicate: (Locale) -> Boolean): List<Locale> =
    availableLocaleList().filter(predicate)

/**
 * 사용 가능한 [Locale] Set을 반환합니다.
 *
 * ```kotlin
 * val localeSet = availableLocaleSet()  // 중복 없는 Locale 집합
 * ```
 *
 * @return 사용 가능한 [Locale] Set
 * @see org.apache.commons.lang3.LocaleUtils.availableLocaleSet
 */
fun availableLocaleSet(): Set<Locale> = LocaleUtils.availableLocaleSet()

/**
 * [predicate]를 만족하는 [Locale] Set을 반환합니다.
 *
 * ```kotlin
 * val asiaLocales = availableLocaleSet { it.country in listOf("KR", "JP", "CN") }
 * ```
 *
 * @param predicate 필터 조건
 * @return 조건을 만족하는 [Locale] Set
 * @see org.apache.commons.lang3.LocaleUtils.availableLocaleSet
 */
fun availableLocaleSet(predicate: (Locale) -> Boolean): Set<Locale> =
    LocaleUtils.availableLocaleSet().filter(predicate).toSet()

/**
 * [language] 코드를 사용하는 국가들의 [Locale]을 반환합니다.
 *
 * ```kotlin
 * val koCountries = countriesByLanguage("ko")  // [ko_KR]
 * val enCountries = countriesByLanguage("en")  // [en_US, en_GB, ...]
 * ```
 *
 * @param language BCP 47 언어 코드 (예: "ko", "en")
 * @return 해당 언어를 사용하는 [Locale] 목록
 * @see org.apache.commons.lang3.LocaleUtils.countriesByLanguage
 */
fun countriesByLanguage(language: String): List<Locale> =
    LocaleUtils.countriesByLanguage(language)

/**
 * locale이 JVM에서 사용 가능한지 확인합니다.
 *
 * ```kotlin
 * Locale.KOREAN.isAvailable()  // true
 * Locale("xx").isAvailable()   // false
 * ```
 *
 * @receiver 확인할 [Locale]
 * @return 사용 가능하면 true
 * @see org.apache.commons.lang3.LocaleUtils.isAvailableLocale
 */
fun Locale.isAvailable(): Boolean = LocaleUtils.isAvailableLocale(this)

/**
 * locale의 언어가 결정되지 않은지 확인합니다.
 *
 * ```kotlin
 * Locale("und").isLanguageUndetermined()  // true
 * Locale.KOREAN.isLanguageUndetermined()  // false
 * ```
 *
 * @receiver 확인할 [Locale]
 * @return 언어가 undetermined이면 true
 * @see org.apache.commons.lang3.LocaleUtils.isLanguageUndetermined
 */
fun Locale.isLanguageUndetermined(): Boolean = LocaleUtils.isLanguageUndetermined(this)

/**
 * 국가 코드 [countryCode]를 사용하는 언어들의 [Locale]을 반환합니다.
 *
 * ```kotlin
 * val krLanguages = languageByCountry("KR")  // [ko_KR]
 * val usLanguages = languageByCountry("US")  // [en_US, es_US, ...]
 * ```
 *
 * @param countryCode ISO 3166 국가 코드 (예: "KR", "US")
 * @return 해당 국가에서 사용하는 [Locale] 목록
 * @see org.apache.commons.lang3.LocaleUtils.languagesByCountry
 */
fun languageByCountry(countryCode: String): List<Locale> = LocaleUtils.languagesByCountry(countryCode)

/**
 * 로케일 검색 시 탐색할 로케일 목록을 반환합니다.
 *
 * ```kotlin
 * Locale("fr", "CA", "xxx").localeLookupList()
 * // [Locale("fr", "CA", "xxx"), Locale("fr", "CA"), Locale("fr")]
 * ```
 *
 * @receiver 검색을 시작할 기준 로케일
 * @param defaultLocale 탐색 마지막에 추가할 기본 로케일 (기본: receiver 자신)
 * @return 탐색 순서로 정렬된 [Locale] 목록 (수정 불가)
 * @see org.apache.commons.lang3.LocaleUtils.localeLookupList
 */
fun Locale.localeLookupList(defaultLocale: Locale = this): List<Locale> =
    LocaleUtils.localeLookupList(this, defaultLocale)

/**
 * Locale을 나타내는 문자열을 [Locale]로 변환합니다.
 *
 * ```kotlin
 * "ko_KR".toLocale()  // Locale("ko", "KR")
 * "en_US".toLocale()  // Locale("en", "US")
 * "fr".toLocale()     // Locale("fr")
 * ```
 *
 * @receiver Locale 문자열 (예: "ko_KR", "en_US")
 * @return 변환된 [Locale]
 * @throws IllegalArgumentException 유효하지 않은 형식인 경우
 * @see org.apache.commons.lang3.LocaleUtils.toLocale
 */
fun String.toLocale(): Locale = LocaleUtils.toLocale(this)
