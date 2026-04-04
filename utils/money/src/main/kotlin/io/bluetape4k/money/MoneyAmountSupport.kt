@file:Suppress("UNCHECKED_CAST")

package io.bluetape4k.money

import java.math.BigDecimal
import java.math.BigInteger
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.MonetaryAmount
import javax.money.MonetaryRounding
import javax.money.convert.ConversionQuery
import javax.money.convert.ConversionQueryBuilder
import javax.money.convert.MonetaryConversions

/**
 * 통화량을 나타내는 [MonetaryAmount]를 빌드합니다.
 *
 * ```kotlin
 * monetaryAmountOf(100, "KRW")        // 100 원
 * monetaryAmountOf(1.05, "USD")       // USD 1.05
 * ```
 *
 * @param number        통화량
 * @param currencyCode  통화단위 코드 ("KRW", "USD", "EUR")
 * @return 통화량([MonetaryAmount]) instance
 */
fun monetaryAmountOf(number: Number, currencyCode: String): MonetaryAmount =
    monetaryAmountOf(number, currencyUnitOf(currencyCode))

/**
 * 통화량을 나타내는 [MonetaryAmount]를 빌드합니다.
 *
 * ```kotlin
 * monetaryAmountOf(100, "KRW")        // 100 원
 * monetaryAmountOf(1.05, "USD")       // USD 1.05
 * ```
 *
 * @param number        통화량
 * @param currency      통화단위 ([CurrencyUnit]) (Default: [DefaultCurrencyUnit])
 * @return 통화량([MonetaryAmount]) instance
 */
fun monetaryAmountOf(number: Number, currency: CurrencyUnit = DefaultCurrencyUnit): MonetaryAmount =
    Monetary.getDefaultAmountFactory().setCurrency(currency).setNumber(number).create()

/**
 * 통화량을 나타내는 [MonetaryAmount]를 빌드합니다.
 *
 * ```kotlin
 * monetaryAmountOf(100, "KRW")        // 100 원
 * monetaryAmountOf(1.05, "USD")       // USD 1.05
 * ```
 *
 * @param currency      통화단위 ([CurrencyUnit]) (Default: [DefaultCurrencyUnit])
 * @return 통화량([MonetaryAmount]) instance
 */
fun Number.toMonetaryAmount(currency: CurrencyUnit = DefaultCurrencyUnit): MonetaryAmount =
    Monetary.getDefaultAmountFactory().setCurrency(currency).setNumber(this).create()

/**
 * 통화량을 나타내는 [MonetaryAmount]를 빌드합니다.
 *
 * ```kotlin
 * val krw = 1024.toMonetaryAmount("KRW")   // KRW 1024
 * val usd = 9.99.toMonetaryAmount("USD")   // USD 9.99
 * ```
 *
 * @param currencyCode  통화단위 코드 ("KRW", "USD", "EUR")
 * @return 통화량([MonetaryAmount]) instance
 */
fun Number.toMonetaryAmount(currencyCode: String): MonetaryAmount =
    monetaryAmountOf(this, currencyCode)

//
// MonetaryAmount Arithmetic Operators
//

/**
 * 통화 금액의 부호를 반전합니다.
 *
 * ```kotlin
 * val usd = 10.toMoney(USD)
 * val negated = -usd  // USD -10
 * ```
 */
operator fun <T: MonetaryAmount> T.unaryMinus(): T = this.negate() as T

/**
 * 두 통화 금액을 더합니다. 같은 통화 단위여야 합니다.
 *
 * ```kotlin
 * val a = 10.toMoney(USD)
 * val b = 20.toMoney(USD)
 * val sum = a + b  // USD 30
 * ```
 */
operator fun <T: MonetaryAmount> T.plus(other: MonetaryAmount): T = this.add(other) as T

/**
 * 통화 금액에 숫자를 더합니다.
 *
 * ```kotlin
 * val a = 10.toMoney(USD)
 * val result = a + 5  // USD 15
 * ```
 */
operator fun <T: MonetaryAmount> T.plus(scalar: Number): T = this.add(scalar.toMonetaryAmount(currency)) as T

/**
 * 두 통화 금액을 뺍니다. 같은 통화 단위여야 합니다.
 *
 * ```kotlin
 * val a = 20.toMoney(USD)
 * val b = 10.toMoney(USD)
 * val diff = a - b  // USD 10
 * ```
 */
operator fun <T: MonetaryAmount> T.minus(other: MonetaryAmount): T = this.subtract(other) as T

/**
 * 통화 금액에서 숫자를 뺍니다.
 *
 * ```kotlin
 * val a = 20.toMoney(USD)
 * val result = a - 5  // USD 15
 * ```
 */
operator fun <T: MonetaryAmount> T.minus(scalar: Number): T = this.subtract(scalar.toMonetaryAmount(currency)) as T

/**
 * 통화 금액에 스칼라 값을 곱합니다.
 *
 * ```kotlin
 * val a = 10.toMoney(USD)
 * val result = a * 3  // USD 30
 * ```
 */
operator fun <T: MonetaryAmount> T.times(scalar: Number): T = this.multiply(scalar) as T

/**
 * 숫자에 통화 금액을 곱합니다. (교환법칙 지원)
 *
 * ```kotlin
 * val a = 10.toMoney(USD)
 * val result = 3 * a  // USD 30
 * ```
 */
operator fun <T: MonetaryAmount> Number.times(amount: T): T = amount.multiply(this) as T

/**
 * 통화 금액을 스칼라 값으로 나눕니다.
 *
 * ```kotlin
 * val a = 30.toMoney(USD)
 * val result = a / 3  // USD 10
 * ```
 */
operator fun <T: MonetaryAmount> T.div(scalar: Number): T = this.divide(scalar) as T

/**
 * [MonetaryAmount]에서 금액을 원하는 수형으로 가져옵니다.
 *
 * ```kotlin
 * val m = moneyOf(12.5, USD)
 * val bd: BigDecimal = m.numberValue()
 * val d: Double = m.numberValue()
 * ```
 *
 * @param T 변환할 수형 ([Int], [Long], [Double], [BigDecimal] 등)
 * @return 해당 수형의 금액 값
 */
inline fun <reified T: Number> MonetaryAmount.numberValue(): T = number.numberValue(T::class.java)

/**
 * 금액을 [Int] 값으로 가져옵니다.
 *
 * ```kotlin
 * val m = moneyOf(100, "KRW")
 * val v: Int = m.intValue  // 100
 * ```
 */
val MonetaryAmount.intValue: Int get() = numberValue()

/**
 * 금액을 [Long] 값으로 가져옵니다.
 *
 * ```kotlin
 * val m = moneyOf(50000L, "KRW")
 * val v: Long = m.longValue  // 50000L
 * ```
 */
val MonetaryAmount.longValue: Long get() = numberValue()

/**
 * 금액을 [Float] 값으로 가져옵니다.
 *
 * ```kotlin
 * val m = moneyOf(1.5, "USD")
 * val v: Float = m.floatValue  // 1.5f
 * ```
 */
val MonetaryAmount.floatValue: Float get() = numberValue()

/**
 * 금액을 [Double] 값으로 가져옵니다.
 *
 * ```kotlin
 * val m = moneyOf(12.5, "USD")
 * val v: Double = m.doubleValue  // 12.5
 * ```
 */
val MonetaryAmount.doubleValue: Double get() = numberValue()

/**
 * 금액을 [BigDecimal] 값으로 가져옵니다.
 *
 * ```kotlin
 * val m = moneyOf(9.99, "USD")
 * val v: BigDecimal = m.bigDecimalValue  // BigDecimal(9.99)
 * ```
 */
val MonetaryAmount.bigDecimalValue: BigDecimal get() = numberValue()

/**
 * 금액을 [BigInteger] 값으로 가져옵니다.
 *
 * ```kotlin
 * val m = moneyOf(1000, "KRW")
 * val v: BigInteger = m.bigIntValue  // BigInteger(1000)
 * ```
 */
val MonetaryAmount.bigIntValue: BigInteger get() = numberValue()

/**
 * [MonetaryAmount] 수형의 금액을 반올림합니다.
 *
 * ```kotlin
 * 1.05.inUSD().round()        // USD 1.05 를 반올림
 * ```
 */
fun <T: MonetaryAmount> T.round(rounding: MonetaryRounding = Monetary.getRounding(this.currency)): T =
    this.with(rounding) as T

/**
 * 기본 반올림 규칙에 의해 [MonetaryAmount] 수형의 금액을 반올림합니다.
 *
 * ```kotlin
 * val m = moneyOf(1.005, "USD")
 * val rounded = m.defaultRound()  // USD 1.01 (기본 반올림 적용)
 * ```
 */
fun <T: MonetaryAmount> T.defaultRound(): T = this.with(Monetary.getDefaultRounding()) as T

/**
 * EU의 오늘자 환율 정보를 이용하여 @receiver 의 금액을 대상 금액으로 환전합니다
 *
 * ```kotlin
 * 1.05.inUSD().convertTo("KRW")        // USD 1.05 를 원화로 환전
 * ```
 *
 * @param T
 * @param currencyCode 환전할 통화 코드 ("USD", "KRW", "EUR")
 * @return
 */
fun <T: MonetaryAmount> T.convertTo(currencyCode: String): T =
    this.with(MonetaryConversions.getConversion(currencyCode)) as T

/**
 * 오늘자 환율 정보를 이용하여 [T]의 금액을 대상 금액으로 환전합니다
 *
 * ```kotlin
 * 1.05.inUSD().convertTo(currencyOf("KRW"))        // USD 1.05 를 원화로 환전
 * ```
 *
 * @param T 통화량 수형
 * @param currencyUnit 환전할 통화 단위 (기본값: [DefaultCurrencyUnit])
 * @return 환전한 통화량
 */
fun <T: MonetaryAmount> T.convertTo(
    currencyUnit: CurrencyUnit = DefaultCurrencyUnit,
    conversionQuery: ConversionQuery = ConversionQueryBuilder.of().setTermCurrency(currencyUnit).build(),
): T {
    val conversion = MonetaryConversions.getConversion(conversionQuery)
    return this.with(conversion) as T
}


/**
 * 금액을 합산합니다
 *
 * ```kotlin
 * listOf(1.05.inUSD(), 2.05.inUSD()).sum(USD)        // USD 3.10
 * listOf(100L.inKRW(), 200L.inKRW()).sum(KRW)        // KRW 300
 * ```
 *
 * @param currencyUnit 환전할 통화 단위 (Default: [DefaultCurrencyUnit])
 * @return 환전한 통화량([MonetaryAmount])
 */
fun Collection<MonetaryAmount>.sum(currencyUnit: CurrencyUnit = DefaultCurrencyUnit): MonetaryAmount {
    if (isEmpty()) {
        return 0L.toMonetaryAmount(currencyUnit)
    }

    var sum = 0L.toMonetaryAmount(currencyUnit)
    forEach {
        sum += it.convertTo(currencyUnit)
    }
    return sum
}
