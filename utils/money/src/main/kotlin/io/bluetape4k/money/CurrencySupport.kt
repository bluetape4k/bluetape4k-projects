package io.bluetape4k.money

import io.bluetape4k.support.requireNotBlank
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.money.CurrencyUnit
import javax.money.Monetary

private val currencyCache: MutableMap<String, CurrencyUnit> = ConcurrentHashMap()
private val currencyCacheByLocale: MutableMap<Locale, CurrencyUnit> = ConcurrentHashMap()

/**
 * 시스템 기본 [Locale]의 통화 단위를 반환합니다.
 *
 * ## 동작/계약
 * - JVM 기본 로케일 시점 값으로 초기화됩니다.
 * - 불변 상수처럼 재사용됩니다.
 *
 * ```kotlin
 * val unit = DefaultCurrencyUnit
 * // unit.currencyCode.isNotBlank() == true
 * ```
 */
@JvmField
val DefaultCurrencyUnit: CurrencyUnit = Monetary.getCurrency(Locale.getDefault())

/**
 * 시스템 기본 통화 코드를 제공합니다.
 *
 * ## 동작/계약
 * - [DefaultCurrencyUnit]의 `currencyCode`를 그대로 노출합니다.
 *
 * ```kotlin
 * val code = DefaultCurrencyCode
 * // code == DefaultCurrencyUnit.currencyCode
 * ```
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
 * 통화 코드 사용 가능 여부를 확인합니다.
 *
 * ## 동작/계약
 * - provider를 지정하지 않으면 기본 공급자 기준으로 조회합니다.
 * - 유효하지 않은 통화 코드는 `false`를 반환합니다.
 *
 * ```kotlin
 * val ok = "USD".isAvailableCurrency()
 * val fail = "AAA".isAvailableCurrency()
 * // ok == true
 * // fail == false
 * ```
 */
fun String.isAvailableCurrency(vararg providers: String): Boolean =
    Monetary.isCurrencyAvailable(this, *providers)

/**
 * 해당 통화 코드의 [CurrencyUnit]을 생성합니다.
 *
 * ## 동작/계약
 * - `currencyCode`는 공백이 아니어야 하며 공백이면 `requireNotBlank`에서 예외가 발생합니다.
 * - 동일 코드에 대해서는 내부 캐시된 [CurrencyUnit]을 재사용합니다.
 *
 * ```kotlin
 * val KRW = currencyUnitOf("KRW")
 * val USD = currencyUnitOf("USD")
 * val EUR = currencyUnitOf("EUR")
 * val CNY = currencyUnitOf("CNY")
 * // KRW.currencyCode == "KRW"
 * ```
 *
 * @param  currencyCode 통화 코드 (예: KRW, USD, EUR, CNY ...)
 * @return [CurrencyUnit] 인스턴스
 */
fun currencyUnitOf(currencyCode: String = DefaultCurrencyCode): CurrencyUnit {
    currencyCode.requireNotBlank("currencyCode")
    return currencyCache.computeIfAbsent(currencyCode) {
        Monetary.getCurrency(currencyCode)
    }
}

/**
 * [locale]에서 사용하는 [CurrencyUnit]을 생성합니다.
 *
 * ## 동작/계약
 * - 동일 로케일에 대해서는 내부 캐시된 [CurrencyUnit]을 재사용합니다.
 * - 로케일에 통화가 매핑되지 않은 경우 `Monetary.getCurrency(locale)` 예외가 전파될 수 있습니다.
 *
 * ```kotlin
 * val USD = currencyUnitOf(Locale.US)
 * val KRW = currencyUnitOf(Locale.KOREA)
 * val EUR = currencyUnitOf(Locale.FRANCE)
 * // USD.currencyCode == "USD"
 * ```
 *
 * @param  locale [Locale] 인스턴스
 * @return [CurrencyUnit] 인스턴스
 */
fun currencyUnitOf(locale: Locale): CurrencyUnit {
    return currencyCacheByLocale.computeIfAbsent(locale) {
        Monetary.getCurrency(locale)
    }
}
