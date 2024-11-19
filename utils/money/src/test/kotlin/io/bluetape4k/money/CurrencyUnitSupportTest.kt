package io.bluetape4k.money

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import javax.money.Monetary

class CurrencyUnitSupportTest {

    companion object: KLogging()

    @Test
    fun `사용 가능한 currency code 인지 확인`() {
        val currencies = Monetary.getCurrencies()
        currencies.forEach {
            log.debug { "Currency code=${it.currencyCode}" }
            it.currencyCode.isAvailableCurrency().shouldBeTrue()
        }
    }

    @Test
    fun `사용가능하지 않은 currency code`() {
        "".isAvailableCurrency().shouldBeFalse()
        "AAA".isAvailableCurrency().shouldBeFalse()
        "BBB".isAvailableCurrency().shouldBeFalse()
    }
}
