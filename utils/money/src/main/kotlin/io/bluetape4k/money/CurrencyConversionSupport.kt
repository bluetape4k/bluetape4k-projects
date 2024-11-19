package io.bluetape4k.money

import javax.money.CurrencyUnit
import javax.money.convert.MonetaryConversions

/**
 * 현재 화폐가 환전이 가능한가?
 *
 * ```
 * "USD".isCurrencyConversionAvailable  // true
 * "KRW".isCurrencyConversionAvailable  // true
 * "JPY".isCurrencyConversionAvailable  // true
 * ```
 */
val String.isCurrencyConversionAvailable: Boolean
    get() = this.isAvailableCurrency() &&
            MonetaryConversions.isConversionAvailable(this)

/**
 * 화폐 단위가 환전이 가능한가?
 *
 * ```
 * CurrencyUnit.of("USD").isCurrencyConversionAvailable  // true
 * CurrencyUnit.of("KRW").isCurrencyConversionAvailable  // true
 * CurrencyUnit.of("JPY").isCurrencyConversionAvailable  // true
 * ```
 */
val CurrencyUnit.isCurrencyConversionAvailable: Boolean
    get() = currencyCode.isAvailableCurrency() &&
            MonetaryConversions.isConversionAvailable(this)
