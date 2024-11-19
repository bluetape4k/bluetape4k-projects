package io.bluetape4k.money

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test

class CurrencyConverterTest {

    companion object: KLogging() {
        private val defaultConversion = CurrencyConvertor.DefaultConversion
        private val usdConversion = CurrencyConvertor.USDConversion
    }

    @Test
    fun `시스템 기본 Conversion 정보`() {
        val usd = 124.59.inUSD()

        val exchangeRate = defaultConversion.getExchangeRate(usd)
        log.debug { "exchange rate=$exchangeRate" }
        exchangeRate.baseCurrency.currencyCode shouldBeEqualTo "USD"

        val krw = usd.convertTo("KRW")
        krw.convertTo("USD").doubleValue.shouldBeNear(usd.doubleValue, 1e-2)
    }
}
