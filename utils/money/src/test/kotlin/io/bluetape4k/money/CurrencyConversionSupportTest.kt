package io.bluetape4k.money

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class CurrencyConversionSupportTest {

    companion object: KLogging()

    @Test
    fun `유효한 통화 코드는 환전 가능 여부를 확인할 수 있다`() {
        "USD".isCurrencyConversionAvailable.shouldBeTrue()
        "EUR".isCurrencyConversionAvailable.shouldBeTrue()
        "KRW".isCurrencyConversionAvailable.shouldBeTrue()
        "JPY".isCurrencyConversionAvailable.shouldBeTrue()
        "CNY".isCurrencyConversionAvailable.shouldBeTrue()

        log.debug { "USD conversion available: ${"USD".isCurrencyConversionAvailable}" }
    }

    @Test
    fun `유효하지 않은 통화 코드는 환전 불가`() {
        "".isCurrencyConversionAvailable.shouldBeFalse()
        "AAA".isCurrencyConversionAvailable.shouldBeFalse()
        "ZZZ".isCurrencyConversionAvailable.shouldBeFalse()
    }

    @Test
    fun `CurrencyUnit으로 환전 가능 여부 확인`() {
        USD.isCurrencyConversionAvailable.shouldBeTrue()
        EUR.isCurrencyConversionAvailable.shouldBeTrue()
        KRW.isCurrencyConversionAvailable.shouldBeTrue()
        JPY.isCurrencyConversionAvailable.shouldBeTrue()
        CNY.isCurrencyConversionAvailable.shouldBeTrue()

        log.debug { "USD CurrencyUnit conversion available: ${USD.isCurrencyConversionAvailable}" }
    }
}
