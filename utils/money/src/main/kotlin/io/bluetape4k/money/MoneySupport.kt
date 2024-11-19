package io.bluetape4k.money

import org.javamoney.moneta.Money
import javax.money.CurrencyUnit

/**
 * [currency] 통화단위를 가지는 [amount] 금액의 [Money]를 빌드합니다.
 *
 * ```
 * val won = moneyOf(1024L, currencyOf("KRW"))      // 1,024 KRW
 * val dollar = moneyOf(1.05, currencyOf("USD"))    // 1.05 USD
 * ```
 *
 * @param amount       통화량
 * @param currency     통화 단위 ([CurrencyUnit])
 * @return [Money] instance
 */
fun moneyOf(amount: Number, currency: CurrencyUnit = DefaultCurrencyUnit): Money = Money.of(amount, currency)

/**
 * [currency] 통화단위를 가지는 [amount] 금액의 [Money]를 빌드합니다.
 *
 * ```
 * val won = moneyOf(1024L, "KRW")      // 1,024 KRW
 * val dollar = moneyOf(1.05, "USD")    // 1.05 USD
 * ```
 *
 * @param amount       통화량
 * @param currencyCode 통화 단위 코드 (eg. "USD", "KRW")
 * @return [Money] instance
 */
fun moneyOf(amount: Number, currencyCode: String): Money = Money.of(amount, currencyUnitOf(currencyCode))

/**
 * 숫자를 해당 통화단위의 [Money] 인스턴스로 생성합니다.
 *
 * ```
 * val won = 1024L.toMoney(currencyOf("KRW"))  // 1,024 KRW
 * val dollar = 1.05.toMoney(currencyOf("USD"))  // 1.05 USD
 * ```
 *
 * @param currency 통화 단위
 */
fun Number.toMoney(currency: CurrencyUnit = DefaultCurrencyUnit): Money = moneyOf(this, currency)

/**
 * 숫자를 해당 통화단위의 [Money] 인스턴스로 생성합니다.
 *
 * ```
 * val won = 1024L.toMoney("KRW")  // 1,024 KRW
 * val dollar = 1.05.toMoney("USD")  // 1.05 USD
 * ```
 *
 * @param currency 통화단위 코드 ("KRW", "USD", "EUR")
 */
fun Number.toMoney(currencyCode: String): Money = moneyOf(this, currencyCode)

/**
 * 숫자를 원화로 표현하는 [Money]로 빌드합니다.
 *
 * ```
 * val won = 1024L.inKRW()  // 1,024 KRW
 * ```
 */
fun Number.inKRW(): Money = toMoney(KRW)

/**
 * 숫자를 US Dollar 로 표현하는 [Money]로 빌드합니다.
 *
 * ```
 * val dollar = 1.05.inUSD()  // 1.05 USD
 * ```
 */
fun Number.inUSD(): Money = toMoney(USD)

/**
 * 숫자를 EURO 화로 표현하는 [Money]로 빌드합니다.
 *
 * ```
 * val usd = 1.05.inEUR()  // 1.05 EUR
 * ```
 */
fun Number.inEUR(): Money = toMoney(EUR)
