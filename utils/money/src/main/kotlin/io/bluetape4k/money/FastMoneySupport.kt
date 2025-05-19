package io.bluetape4k.money

import org.javamoney.moneta.FastMoney
import javax.money.CurrencyUnit
import javax.money.NumberValue

/**
 * 통화량의 수형이 Long 수형만 지원하는 [FastMoney] 인스턴스를 생성합니다.
 *
 * ```
 * fastMoneyOf(1_200L, currencyOf("KRW"))   // 1,200원
 * fastMoneyOf(1.05, currencyOf("USD"))     // USD 1.05
 * ```
 *
 * @param T      통화량을 나타내는 수형
 * @param amount 통화량 (예: 123.59)
 * @param currency 통화 단위 ([CurrencyUnit])
 * @return [FastMoney] instance
 */
fun <T: Number> fastMoneyOf(amount: T, currency: CurrencyUnit = DefaultCurrencyUnit): FastMoney =
    FastMoney.of(amount, currency)

/**
 * 통화량의 수형이 Long 수형만 지원하는 [FastMoney] 인스턴스를 생성합니다.
 *
 * ```
 * fastMoneyOf(1_200L, "KRW")   // 1,200원
 * fastMoneyOf(1.05, "USD")     // USD 1.05
 * ```
 *
 * @param T      통화량을 나타내는 수형
 * @param amount 통화량 (예: 123.59)
 * @param currencyCode 통화 코드 ("KRW", "USD", "EUR", "CNY")
 * @return [FastMoney] 인스턴스
 */
fun <T: Number> fastMoneyOf(amount: T, currencyCode: String): FastMoney =
    FastMoney.of(amount, currencyCode)

/**
 * 통화단위에 맞는 [FastMoney] 인스턴스를 생성합니다.
 *
 * ```
 * fastMoneyOf(DefaultNumberValue.of(123.23), currencyOf("USD"))
 * ```
 *
 * @param numberBinding 금액을 나타내는 [NumberValue] 인스턴스
 * @param currency 통화 단위 (CurrencyUnit)
 * @return [FastMoney] 인스턴스
 */
fun fastMoneyOf(numberBinding: NumberValue, currency: CurrencyUnit): FastMoney =
    FastMoney.of(numberBinding, currency)

/**
 * FastMoney는 통화량을 Long 수형만 지원하므로 [amountMinor]에 소수점 이하의 정보인 [factionDigits]를 같이 제공하여 [FastMoney]를 생성한다
 *
 * ```
 * fastMoneyMinorOf("USD", 1245L, 2) // $12.45
 * ```
 *
 * @param currencyCode 통화 코드 ("KRW", "USD", "EUR", "CNY")
 * @param amountMinor  소숫점이 포함된 실제 통화량에서 소수점을 뺀 Long 수형의 숫자 (123.59 일 경우 12359)
 * @param factionDigits 실제 통화량에 해당하는 소수점 위치를 표현 (123.59 인 경우 2)
 * @return [FastMoney] instance
 */
fun fastMoneyMinorOf(currencyCode: String, amountMinor: Long, factionDigits: Int = 2): FastMoney =
    FastMoney.ofMinor(currencyUnitOf(currencyCode), amountMinor, factionDigits)

/**
 * FastMoney는 통화량을 Long 수형만 지원하므로 [amountMinor]에 소수점 이하의 정보인 [factionDigits]를 같이 제공하여 [FastMoney]를 생성한다
 *
 * ```
 * fastMoneyMinorOf("USD", 1245, 2) // $12.45
 * ```
 *
 * @param currencyCode 통화 코드 ("KRW", "USD", "EUR", "CNY")
 * @param amountMinor  소숫점이 포함된 실제 통화량에서 소수점을 뺀 Long 수형의 숫자 (123.59 일 경우 12359)
 * @param factionDigits 실제 통화량에 해당하는 소수점 위치를 표현 (123.59 인 경우 2)
 * @return [FastMoney] instance
 */
fun fastMoneyMinorOf(currency: CurrencyUnit, amountMinor: Long, factionDigits: Int = 2): FastMoney =
    FastMoney.ofMinor(currency, amountMinor, factionDigits)

/**
 * 숫자를 [currency] 통화 단위를 사용하는 [FastMoney] 인스턴스로 빌드합니다.
 *
 * ```
 * 1.05.toFastMoney(currencyOf("USD"))  // USD 1.05
 * 1245.toFastMoney(currencyOf("KRW"))  // 1,245원
 * ```
 *
 * @param currency 통화 단위
 */
fun Number.toFastMoney(currency: CurrencyUnit = DefaultCurrencyUnit): FastMoney = fastMoneyOf(this, currency)

/**
 * 숫자를 [currencyCode] 통화 단위를 사용하는 [FastMoney] 인스턴스로 빌드합니다.
 *
 * ```
 * 1.05.toFastMoney("USD")  // USD 1.05
 * 1245.toFastMoney("KRW")  // 1,245원
 * ```
 *
 * @param currencyCode 통화 코드 ("KRW", "USD", "EUR", "CNY")
 * @return [FastMoney] instance
 */
fun Number.toFastMoney(currencyCode: String): FastMoney = fastMoneyOf(this, currencyCode)

/**
 * FastMoney는 통화량을 Long 수형만 지원하므로 [amountMinor]에 소수점 이하의 정보인 [factionDigits]를 같이 제공하여 [FastMoney]를 생성한다
 *
 * ```
 * 1245L.toFastMoneyMinor(currencyOf("USD"), 2)  // USD 12.45
 * ```
 * @param receiver      소숫점이 포함된 실제 통화량에서 소수점을 뺀 Long 수형의 숫자 (123.59 일 경우 12359)
 * @param currency      통화 단위 ([CurrencyUnit])
 * @param factionDigits 실제 통화량에 해당하는 소수점 위치를 표현 (123.59 인 경우 2)
 * @return [FastMoney] instance
 */
fun Long.toFastMoneyMinor(currency: CurrencyUnit = DefaultCurrencyUnit, factionDigits: Int = 5): FastMoney =
    fastMoneyMinorOf(currency, this, factionDigits)

/**
 * FastMoney는 통화량을 Long 수형만 지원하므로 [amountMinor]에 소수점 이하의 정보인 [factionDigits]를 같이 제공하여 [FastMoney]를 생성한다
 *
 * ```
 * 1245L.toFastMoneyMinor(currencyOf("USD"), 2)  // USD 12.45
 * ```
 * @param receiver      소숫점이 포함된 실제 통화량에서 소수점을 뺀 Long 수형의 숫자 (123.59 일 경우 12359)
 * @param currencyCode  통화 코드 ("KRW", "USD", "EUR", "CNY")
 * @param factionDigits 실제 통화량에 해당하는 소수점 위치를 표현 (123.59 인 경우 2)
 * @return [FastMoney] instance
 */
fun Long.toFastMoneyMinor(currencyCode: String, factionDigits: Int = 2): FastMoney =
    fastMoneyMinorOf(currencyCode, this, factionDigits)


/**
 * 숫자를 원화로 표현하는 [FastMoney]로 빌드합니다.
 *
 * ```
 * 1_200.inFastKRW()  // 1,200원
 * ```
 */
fun Number.inFastKRW(): FastMoney = toFastMoney("KRW")

/**
 * 숫자를 US Dollar 로 표현하는 [FastMoney]로 빌드합니다.
 *
 * ```
 * 1.05.inFastUSD()  // USD 1.05
 * ```
 */
fun Number.inFastUSD(): FastMoney = toFastMoney("USD")

/**
 * 숫자를 EURO 화로 표현하는 [FastMoney]로 빌드합니다.
 *
 * ```
 * 1.05.inFastEUR()  // EUR 1.05
 * ```
 */
fun Number.inFastEUR(): FastMoney = toFastMoney("EUR")
