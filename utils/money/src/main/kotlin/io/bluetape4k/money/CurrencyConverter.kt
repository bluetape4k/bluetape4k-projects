package io.bluetape4k.money

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.info
import io.bluetape4k.support.publicLazy
import javax.money.CurrencyUnit
import javax.money.convert.CurrencyConversion
import javax.money.convert.MonetaryConversions

/**
 * 통화 변환기([CurrencyConversion])를 캐시해 제공합니다.
 *
 * ## 동작/계약
 * - 기본/주요 통화 변환기는 lazy 초기화 후 재사용됩니다.
 * - 변환 비율 조회는 `MonetaryConversions` 공급자(`ECB`, `IMF`)를 사용합니다.
 *
 * ```kotlin
 * val conversion = CurrencyConvertor.USDConversion
 * val converted = 1.0.inUSD().with(conversion)
 * // converted.currency.currencyCode == "USD"
 * ```
 */
object CurrencyConvertor: KLogging() {

    /**
     * 시스템 기본 통화 변환기를 지연 생성해 반환합니다.
     *
     * ## 동작/계약
     * - [DefaultCurrencyUnit] 기준으로 1회 생성 후 재사용됩니다.
     *
     * ```kotlin
     * val conversion = CurrencyConvertor.DefaultConversion
     * // conversion.currency.currencyCode == DefaultCurrencyCode
     * ```
     */
    val DefaultConversion: CurrencyConversion by publicLazy { getConversion(DefaultCurrencyUnit) }

    /** 원화 기준 변환기입니다. */
    val KRWConversion: CurrencyConversion by lazy { getConversion(KRW) }
    /** 미국 달러 기준 변환기입니다. */
    val USDConversion: CurrencyConversion by lazy { getConversion(USD) }
    /** 유로 기준 변환기입니다. */
    val EURConversion: CurrencyConversion by lazy { getConversion(EUR) }
    /** 엔화 기준 변환기입니다. */
    val JPYConversion: CurrencyConversion by lazy { getConversion(JPY) }

    /**
     * 통화 단위에 대한 환율을 가져옵니다.
     *
     * ## 동작/계약
     * - 지정 통화를 목표 통화로 하는 변환기를 매번 새로 조회합니다.
     * - 통화 코드/공급자 문제 시 `MonetaryConversions` 예외가 전파될 수 있습니다.
     *
     * ```kotlin
     * val conversion = CurrencyConvertor.getConversion(CurrencyUnit.of("USD"))
     * val result = 100.toMoney("KRW").with(conversion)
     * // result.currency.currencyCode == "USD"
     * ```
     *
     * @param currency 통화 단위
     */
    fun getConversion(currency: CurrencyUnit): CurrencyConversion {
        log.info { "Retrieve currency conversion ratio. currency=$currency" }
        return MonetaryConversions.getConversion(currency, "ECB", "IMF")
    }
}
