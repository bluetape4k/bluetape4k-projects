package io.bluetape4k.money

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.format.MonetaryFormats

class FastMoneySupportTest {

    companion object: KLogging() {
        private val Currencies: Collection<CurrencyUnit> = Monetary.getCurrencies()

        private val defaultConversion = CurrencyConvertor.DefaultConversion
        private val usdConversion = CurrencyConvertor.USDConversion
    }

    @Test
    fun `BigDecimal을 Money로 변환`() {
        val m = fastMoneyOf(10.0.toBigDecimal(), currencyUnitOf("EUR"))
        val number = m.number.numberValue(BigDecimal::class.java)
        number shouldBeEqualTo 10.0.toBigDecimal()

        val decimal = m.numberValue<BigDecimal>()
        decimal shouldBeEqualTo BigDecimal.TEN
    }

    @Test
    fun `Money로부터 다양한 수형의 값 추출`() {
        val m = fastMoneyOf(12L, currencyUnitOf("USD"))
        m.toString() shouldBeEqualTo "USD 12.00"

        // FastMoney의 기본 SCALE이 5이다.
        //
        val fastMoney = fastMoneyOf(2, USD)
        fastMoney.toString() shouldBeEqualTo "USD 2.00"

        Monetary.getDefaultAmountFactory().setCurrency(USD).setNumber(200).create()

        val fastMoneyMinor = fastMoneyMinorOf(USD, 200L, 2)
        fastMoneyMinor.precision
        fastMoneyMinor.toString() shouldBeEqualTo "USD 2.00"
    }

    @Test
    fun `통화량 계산`() {
        val a = 10L.toFastMoney()
        val b = 20L.toFastMoney()

        a - b shouldBeEqualTo -a

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        b / 2 shouldBeEqualTo a

        2 * a shouldBeEqualTo b
    }

    @Test
    fun `금액 반올림하기`() {
        val usd = 1.31.toFastMoney(USD)

        usd.toString() shouldBeEqualTo "USD 1.31"
        usd.with(Monetary.getDefaultRounding()).toString() shouldBeEqualTo "USD 1.31"
        usd.round().toString() shouldBeEqualTo "USD 1.31"
        usd.defaultRound().toString() shouldBeEqualTo "USD 1.31"

        // KRW의 Currency 의 scale 이 0 입니다.
        val krw = 131.47.toFastMoney(KRW)
        krw.toString() shouldBeEqualTo "KRW 131.00"
        krw.round().toString() shouldBeEqualTo "KRW 131.00"
        krw.defaultRound().toString() shouldBeEqualTo "KRW 131.00"
    }

    @Test
    fun `외화 환전하기`() {
        val usd = 1000L.toFastMoneyMinor(USD, 2)
        val eur = usd.convertTo(EUR)
        // val krw = usd.convertTo(KRW)

        eur.convertTo(USD).doubleValue.shouldBeNear(usd.doubleValue, 1e-2)

        // NOTE: fast moeny 는 기본 scale 이 5 이므로, 환전 시 문제가 소수점 5자리 이하의 값은 변환을 못한다
        // krw.convertTo(USD).doubleValue.shouldBeNear(usd.doubleValue, 1e-2)
    }

    @Test
    fun `금액을 Locale에 따라 문자열로 변환 - USD`() {
        val oneDollar = 1L.inFastUSD()
        oneDollar.toString() shouldBeEqualTo "USD 1.00"

        val formatUSD = MonetaryFormats.getAmountFormat(Locale.US)
        val usFormatted = formatUSD.format(oneDollar)
        usFormatted shouldBeEqualTo "USD1.00"
    }

    @Test
    fun `금액을 Locale에 따라 문자열로 변환 - KRW`() {
        val tenkKRW = 1000L.inFastKRW()
        tenkKRW.toString() shouldBeEqualTo "KRW 1000.00"

        val formatKRW = MonetaryFormats.getAmountFormat(Locale.KOREA)
        formatKRW.format(tenkKRW) shouldBeEqualTo "KRW1,000"
    }
}
