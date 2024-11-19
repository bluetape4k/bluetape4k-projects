package io.bluetape4k.money

import io.bluetape4k.support.requireNotBlank
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.money.CurrencyUnit
import javax.money.Monetary

private val currencyCache: MutableMap<String, CurrencyUnit> = ConcurrentHashMap()
private val currencyCacheByLocale: MutableMap<Locale, CurrencyUnit> = ConcurrentHashMap()

/**
 * 시스템 기본 통화단위([CurrencyUnit])
 */
@JvmField
val DefaultCurrencyUnit: CurrencyUnit = Monetary.getCurrency(Locale.getDefault())

/**
 * 시스템 기본 통화 코드 (예: KRW, USD, EUR, CNY ...)
 */
@JvmField
val DefaultCurrencyCode: String = DefaultCurrencyUnit.currencyCode

/**
 * 한국 원화 통화단위([CurrencyUnit])
 */
val KRW: CurrencyUnit = currencyUnitOf("KRW")

/**
 * 미국 달러 통화단위([CurrencyUnit])
 */
val USD: CurrencyUnit = currencyUnitOf("USD")

/**
 * 유럽 유로 통화단위([CurrencyUnit])
 */
val EUR: CurrencyUnit = currencyUnitOf("EUR")

/**
 * 중국 위안 통화단위([CurrencyUnit])
 */
val CNY: CurrencyUnit = currencyUnitOf("CNY")

/**
 * 일본 엔화 통화단위([CurrencyUnit])
 */
val JPY: CurrencyUnit = currencyUnitOf("JPY")

/**
 * 사용 가능한 Currency 인가 확인합니다.
 *
 * @param providers
 * @return
 */
fun String.isAvailableCurrency(vararg providers: String): Boolean =
    Monetary.isCurrencyAvailable(this, *providers)

/**
 * 해당 통화 코드의 [CurrencyUnit]을 생성합니다.
 *
 * ```
 * val KRW = currencyUnitOf("KRW")
 * val USD = currencyUnitOf("USD")
 * val EUR = currencyUnitOf("EUR")
 * val CNY = currencyUnitOf("CNY")
 * ```
 *
 * @param  currencyCode 통화 코드 (예: KRW, USD, EUR, CNY ...)
 * @return [CurrencyUnit] 인스턴스
 */
@JvmOverloads
fun currencyUnitOf(currencyCode: String = DefaultCurrencyCode): CurrencyUnit {
    currencyCode.requireNotBlank("currencyCode")
    return currencyCache.getOrPut(currencyCode) { Monetary.getCurrency(currencyCode) }
}

/**
 * [locale]에서 사용하는 [CurrencyUnit]을 생성합니다.
 *
 * ```
 * val USD = currencyUnitOf(Locale.US)
 * val KRW = currencyUnitOf(Locale.KOREA)
 * val EUR = currencyUnitOf(Locale.FRANCE)
 * ```
 *
 * @param  locale [Locale] 인스턴스
 * @return [CurrencyUnit] 인스턴스
 */
fun currencyUnitOf(locale: Locale): CurrencyUnit {
    return currencyCacheByLocale.getOrPut(locale) { Monetary.getCurrency(locale) }
}
