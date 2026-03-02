package io.bluetape4k.money

import javax.money.CurrencyUnit
import javax.money.convert.MonetaryConversions

/**
 * 통화 코드가 환전 가능한지 확인합니다.
 *
 * ## 동작/계약
 * - 통화 코드가 유효하고([isAvailableCurrency]) 변환 공급자가 존재할 때만 `true`입니다.
 * - 빈 문자열/잘못된 코드는 `false`를 반환하며 예외를 던지지 않습니다.
 *
 * ```kotlin
 * "USD".isCurrencyConversionAvailable  // true
 * "KRW".isCurrencyConversionAvailable  // true
 * "".isCurrencyConversionAvailable     // false
 * ```
 */
val String.isCurrencyConversionAvailable: Boolean
    get() = this.isAvailableCurrency() &&
            MonetaryConversions.isConversionAvailable(this)

/**
 * 통화 단위가 환전 가능한지 확인합니다.
 *
 * ## 동작/계약
 * - `currencyCode` 유효성과 변환 가능 여부를 함께 확인합니다.
 * - 수신 객체를 변경하지 않고 조회만 수행합니다.
 *
 * ```kotlin
 * CurrencyUnit.of("USD").isCurrencyConversionAvailable  // true
 * CurrencyUnit.of("KRW").isCurrencyConversionAvailable  // true
 * CurrencyUnit.of("JPY").isCurrencyConversionAvailable  // true
 * ```
 */
val CurrencyUnit.isCurrencyConversionAvailable: Boolean
    get() = currencyCode.isAvailableCurrency() &&
            MonetaryConversions.isConversionAvailable(this)
