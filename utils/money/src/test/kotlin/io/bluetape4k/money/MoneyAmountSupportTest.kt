package io.bluetape4k.money

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class MoneyAmountSupportTest {

    companion object: KLogging()

    @Test
    fun `MonetaryAmount 생성 - 통화코드 문자열`() {
        val krw = monetaryAmountOf(1000, "KRW")
        krw.currency.currencyCode shouldBeEqualTo "KRW"
        krw.numberValue<Long>() shouldBeEqualTo 1000L

        val usd = monetaryAmountOf(1.05, "USD")
        usd.currency.currencyCode shouldBeEqualTo "USD"
        usd.numberValue<Double>().shouldBeNear(1.05, 1e-10)
    }

    @Test
    fun `MonetaryAmount 생성 - CurrencyUnit`() {
        val krw = monetaryAmountOf(500, KRW)
        krw.currency shouldBeEqualTo KRW
        krw.numberValue<Int>() shouldBeEqualTo 500

        val usd = monetaryAmountOf(99.99, USD)
        usd.currency shouldBeEqualTo USD
    }

    @Test
    fun `Number toMonetaryAmount 확장 함수`() {
        val krw = 1000.toMonetaryAmount(KRW)
        krw.currency shouldBeEqualTo KRW
        krw.numberValue<Int>() shouldBeEqualTo 1000

        val usd = 50.5.toMonetaryAmount("USD")
        usd.currency shouldBeEqualTo USD
    }

    @Test
    fun `unaryMinus 연산자`() {
        val usd = 10.toMoney(USD)
        val negated = -usd
        negated.numberValue<Int>() shouldBeEqualTo -10
        negated.currency shouldBeEqualTo USD
    }

    @Test
    fun `plus 연산자 - MonetaryAmount`() {
        val a = 10.toMoney(USD)
        val b = 20.toMoney(USD)
        val sum = a + b
        sum.numberValue<Int>() shouldBeEqualTo 30
    }

    @Test
    fun `plus 연산자 - 스칼라`() {
        val a = 10.toMoney(USD)
        val result = a + 5
        result.numberValue<Int>() shouldBeEqualTo 15
    }

    @Test
    fun `minus 연산자 - MonetaryAmount`() {
        val a = 20.toMoney(USD)
        val b = 10.toMoney(USD)
        val diff = a - b
        diff.numberValue<Int>() shouldBeEqualTo 10
    }

    @Test
    fun `minus 연산자 - 스칼라`() {
        val a = 20.toMoney(USD)
        val result = a - 5
        result.numberValue<Int>() shouldBeEqualTo 15
    }

    @Test
    fun `times 연산자`() {
        val a = 10.toMoney(USD)
        val result = a * 3
        result.numberValue<Int>() shouldBeEqualTo 30
    }

    @Test
    fun `times 교환법칙`() {
        val a = 10.toMoney(USD)
        val result = 3 * a
        result.numberValue<Int>() shouldBeEqualTo 30
    }

    @Test
    fun `div 연산자`() {
        val a = 30.toMoney(USD)
        val result = a / 3
        result.numberValue<Int>() shouldBeEqualTo 10
    }

    @Test
    fun `numberValue로 다양한 수형 변환`() {
        val m = moneyOf(12.5, USD)

        val intVal: Int = m.numberValue()
        intVal shouldBeEqualTo 12

        val longVal: Long = m.numberValue()
        longVal shouldBeEqualTo 12L

        val doubleVal: Double = m.numberValue()
        doubleVal.shouldBeNear(12.5, 1e-10)

        val bdVal: BigDecimal = m.numberValue()
        bdVal shouldBeEqualTo 12.5.toBigDecimal()
    }

    @Test
    fun `프로퍼티 확장으로 금액 값 추출`() {
        val m = moneyOf(12.5, USD)

        m.intValue shouldBeEqualTo 12
        m.longValue shouldBeEqualTo 12L
        m.doubleValue.shouldBeNear(12.5, 1e-10)
        m.floatValue.shouldBeNear(12.5f, 1e-5f)
        m.bigDecimalValue shouldBeEqualTo 12.5.toBigDecimal()
        m.bigIntValue shouldBeEqualTo BigInteger.valueOf(12)
    }

    @Test
    fun `round 함수`() {
        val usd = 1.31473908.toMoney(USD)
        usd.round().toString() shouldBeEqualTo "USD 1.31"

        val krw = 131.473908.toMoney(KRW)
        krw.round().toString() shouldBeEqualTo "KRW 131"
    }

    @Test
    fun `defaultRound 함수`() {
        val usd = 1.31473908.toMoney(USD)
        usd.defaultRound().toString() shouldBeEqualTo "USD 1.31"
    }

    @Test
    fun `convertTo - 통화 코드 문자열`() {
        val usd = 1.0.toMoney(USD)
        val eur = usd.convertTo("EUR")
        eur.currency.currencyCode shouldBeEqualTo "EUR"

        // 역변환 시 원래 값 근사
        eur.convertTo("USD").doubleValue.shouldBeNear(usd.doubleValue, 1e-2)
    }

    @Test
    fun `convertTo - CurrencyUnit`() {
        val usd = 1.0.toMoney(USD)
        val krw = usd.convertTo(KRW)
        krw.currency shouldBeEqualTo KRW

        krw.convertTo(USD).doubleValue.shouldBeNear(usd.doubleValue, 1e-2)
    }

    @Test
    fun `Collection sum - 같은 통화`() {
        val amounts = listOf(
            100.toMonetaryAmount(KRW),
            200.toMonetaryAmount(KRW),
            300.toMonetaryAmount(KRW)
        )
        val total = amounts.sum(KRW)
        total.numberValue<Int>() shouldBeEqualTo 600
        total.currency shouldBeEqualTo KRW
    }

    @Test
    fun `Collection sum - 빈 컬렉션`() {
        val amounts = emptyList<javax.money.MonetaryAmount>()
        val total = amounts.sum(USD)
        total.numberValue<Long>() shouldBeEqualTo 0L
        total.currency shouldBeEqualTo USD
    }

    @Test
    fun `복합 산술 연산`() {
        val a = 100.toMoney(USD)
        val b = 50.toMoney(USD)

        val result = (a + b) * 2 - 100
        log.debug { "result=$result" }
        result.numberValue<Int>() shouldBeEqualTo 200

        // (100 + 50) * 2 - 100 = 200
        result shouldBeEqualTo 200.toMoney(USD)
    }

    @Test
    fun `MonetaryAmount 비교 연산`() {
        val a = 100.toMonetaryAmount(USD)
        val b = 200.toMonetaryAmount(USD)

        a.compareTo(b) shouldBeLessThan 0
        b.compareTo(a) shouldBeGreaterThan 0
        a.compareTo(100.toMonetaryAmount(USD)) shouldBeEqualTo 0
    }
}
