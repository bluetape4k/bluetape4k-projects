package io.bluetape4k.protobuf

import org.javamoney.moneta.Money as JavaMoney

/**
 * 구글 grpc의 Money 타입의 객체를 Java의 표준 Money 수형으로 변환합니다.
 *
 * @return [JavaMoney] 인스턴스
 */
fun ProtoMoney.toJavaMoney(): JavaMoney {
    val number = units.toBigDecimal() + (nanos.toDouble() / 1.0e9).toBigDecimal()
    return JavaMoney.of(number, currencyCode)
}

/**
 * Java 표준 Money 수형을 구글 grpc 의 Money 수형으로 변환합니다.
 *
 * @return
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
