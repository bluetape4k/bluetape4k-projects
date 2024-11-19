package io.bluetape4k.money

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.publicLazy
import io.bluetape4k.support.unsafeLazy
import javax.money.CurrencyUnit
import javax.money.convert.CurrencyConversion
import javax.money.convert.MonetaryConversions

/**
 * 통화 (Curreny) 변환 (환전)을 위한 [CurrencyConversion]을 제공합니다.
 */
object CurrencyConvertor: KLogging() {

    val DefaultConversion: CurrencyConversion by publicLazy { getConversion(DefaultCurrencyUnit) }

    val KRWConversion: CurrencyConversion by unsafeLazy { getConversion(KRW) }
    val USDConversion: CurrencyConversion by unsafeLazy { getConversion(USD) }
    val EURConversion: CurrencyConversion by unsafeLazy { getConversion(EUR) }
    val JPYConversion: CurrencyConversion by unsafeLazy { getConversion(JPY) }

    /**
     * 통화 단위에 대한 환율을 가져옵니다.
     *
     * ```
     * val conversion = CurrencyConvertor.getConversion(CurrencyUnit.of("USD"))
     * val result = conversion.convert(Money.of(100, "USD"), CurrencyUnit.of("KRW"))
     * ```
     *
     * @param currency 통화 단위
     */
    fun getConversion(currency: CurrencyUnit): CurrencyConversion {
        log.info { "Retrieve currency conversion ratio. currency=$currency" }
        return MonetaryConversions.getConversion(currency, "ECB", "IMF")
    }
}
