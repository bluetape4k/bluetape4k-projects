package io.bluetape4k.money

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*
import javax.money.CurrencyUnit
import javax.money.Monetary
import javax.money.UnknownCurrencyException
import javax.money.format.MonetaryFormats
import kotlin.test.assertFailsWith

class MoneySupportTest {

    companion object: KLogging() {
        private const val EPSILON = 1e-2
        private val Currencies: Collection<CurrencyUnit> = Monetary.getCurrencies()

        private val defaultConversion = CurrencyConvertor.DefaultConversion
        private val usdConversion = CurrencyConvertor.USDConversion
    }

    @Test
    fun `가능한 Currency Unit 조회`() {
        printCurrencyUnit(KRW)
        printCurrencyUnit(USD)
        printCurrencyUnit(EUR)
        printCurrencyUnit(CNY)

        Currencies.forEach { printCurrencyUnit(it) }

        assertFailsWith<UnknownCurrencyException> {
            currencyUnitOf("AAA")
        }
    }

    private fun printCurrencyUnit(unit: CurrencyUnit) {
        log.debug { "code=${unit.currencyCode}, fraction digits=${unit.defaultFractionDigits}" }
    }

    @Test
    fun `get currency unit by locale`() {
        currencyUnitOf() shouldBeEqualTo DefaultCurrencyUnit

        currencyUnitOf(Locale.FRANCE) shouldBeEqualTo currencyUnitOf("EUR")
        currencyUnitOf(Locale.US) shouldBeEqualTo currencyUnitOf("USD")
        currencyUnitOf(Locale.CHINA) shouldBeEqualTo currencyUnitOf("CNY")
    }

    @Test
    fun `BigDecimal을 Money로 변환`() {
        val m = moneyOf(10.0.toBigDecimal(), currencyUnitOf("EUR"))
        val number = m.number.numberValue(BigDecimal::class.java)
        number shouldBeEqualTo 10.0.toBigDecimal()

        val decimal = m.numberValue<BigDecimal>()
        decimal shouldBeEqualTo BigDecimal.TEN
    }

    @Test
    fun `Money로부터 다양한 수형의 값 추출`() {
        val m = moneyOf(12L, currencyUnitOf("USD"))
        m.toString() shouldBeEqualTo "USD 12"

        // FastMoney의 기본 SCALE이 5이다.
        //
        val fastMoney = fastMoneyOf(2, USD)
        fastMoney.toString() shouldBeEqualTo "USD 2"

        Monetary.getDefaultAmountFactory().setCurrency(USD).setNumber(200).create()

        val fastMoneyMinor = fastMoneyMinorOf(USD, 200L, 2)
        fastMoneyMinor.precision shouldBeEqualTo 1
        fastMoneyMinor.toString() shouldBeEqualTo "USD 2"
    }

    @Test
    fun `통화량 계산`() {
        val a = 10L.toMoney()
        val b = 20L.toMoney()

        a - b shouldBeEqualTo -a

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        b / 2 shouldBeEqualTo a

        2 * a shouldBeEqualTo b
    }

    @Test
    fun `금액 반올림하기`() {
        val usd = 1.31473908.toMoney(USD)

        usd.toString() shouldBeEqualTo "USD 1.31473908"
        usd.with(Monetary.getDefaultRounding()).toString() shouldBeEqualTo "USD 1.31"
        usd.round().toString() shouldBeEqualTo "USD 1.31"
        usd.defaultRound().toString() shouldBeEqualTo "USD 1.31"

        // KRW의 Currency 의 scale 이 0 입니다.
        val krw = 131.473908.toMoney(KRW)
        krw.toString() shouldBeEqualTo "KRW 131.473908"
        krw.round().toString() shouldBeEqualTo "KRW 131"
        krw.defaultRound().toString() shouldBeEqualTo "KRW 131"
    }

    @Test
    fun `외화 환전하기`() {
        val usd = 1.0.toMoney(USD)
        val eur = usd.convertTo(EUR)
        val krw = usd.convertTo(KRW)

        eur.convertTo(USD).doubleValue.shouldBeNear(usd.doubleValue, EPSILON)
        krw.convertTo(USD).doubleValue.shouldBeNear(usd.doubleValue, EPSILON)
    }

    @Test
    fun `금액을 Locale에 따라 문자열로 변환 - USD`() {
        val oneDollar: Money = 1L.inUSD()

        val formatUSD = MonetaryFormats.getAmountFormat(Locale.US)
        val usFormatted = formatUSD.format(oneDollar)

        oneDollar.toString() shouldBeEqualTo "USD 1"
        usFormatted shouldBeEqualTo "USD1.00"
    }

    @Test
    fun `금액을 Locale에 따라 문자열로 변환 - KRW`() {
        val tenkKRW: Money = 1000L.inKRW()

        val formatKRW = MonetaryFormats.getAmountFormat(Locale.KOREA)
        val krFormatted = formatKRW.format(tenkKRW)

        tenkKRW.toString() shouldBeEqualTo "KRW 1000"
        krFormatted shouldBeEqualTo "KRW1,000"
    }
}
