package io.bluetape4k.protobuf

import org.javamoney.moneta.Money as JavaMoney

/**
 * Protobuf [ProtoMoney]를 JavaMoney로 변환합니다.
 *
 * ## 동작/계약
 * - `units + nanos/1e9`로 금액을 구성합니다.
 * - 통화 코드는 `currencyCode`를 그대로 사용합니다.
 * - 새 [JavaMoney] 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val money = protoMoney.toJavaMoney()
 * // money.currency.currencyCode == protoMoney.currencyCode
 * ```
 */
fun ProtoMoney.toJavaMoney(): JavaMoney {
    val number = units.toBigDecimal() + (nanos.toDouble() / 1.0e9).toBigDecimal()
    return JavaMoney.of(number, currencyCode)
}

/**
 * [JavaMoney]를 Protobuf [ProtoMoney]로 변환합니다.
 *
 * ## 동작/계약
 * - 금액의 정수부는 `units`, 소수부는 `nanos`로 분해합니다.
 * - 정수부 추출에 `longValueExact()`를 사용하므로 범위를 벗어나면 예외가 발생할 수 있습니다.
 * - 새 protobuf 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val proto = JavaMoney.of(12.34, "USD").toProtoMoney()
 * // proto.currencyCode == "USD"
 * ```
 */
fun JavaMoney.toProtoMoney(): ProtoMoney {
    val units = this.number.longValueExact()
    val nanos = ((this.number.doubleValueExact() - units) * 1.0e9).toInt()

    return ProtoMoney.newBuilder()
        .setCurrencyCode(currency.currencyCode)
        .setUnits(units)
        .setNanos(nanos)
        .build()
}
