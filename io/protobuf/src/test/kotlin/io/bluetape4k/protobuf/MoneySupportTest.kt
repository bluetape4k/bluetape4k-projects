package io.bluetape4k.protobuf

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.javamoney.moneta.Money
import org.junit.jupiter.api.Test

class MoneySupportTest {
    companion object: KLogging()

    @Test
    fun `toJavaMoney - ProtoMoney를 JavaMoney로 변환한다`() {
        val protoMoney =
            ProtoMoney
                .newBuilder()
                .setCurrencyCode("USD")
                .setUnits(12L)
                .setNanos(340_000_000)
                .build()

        val javaMoney = protoMoney.toJavaMoney()

        javaMoney.shouldNotBeNull()
        javaMoney.currency.currencyCode shouldBeEqualTo "USD"
    }

    @Test
    fun `toProtoMoney - JavaMoney를 ProtoMoney로 변환한다`() {
        val javaMoney = Money.of(java.math.BigDecimal("9.99"), "EUR")

        val protoMoney = javaMoney.toProtoMoney()

        protoMoney.shouldNotBeNull()
        protoMoney.currencyCode shouldBeEqualTo "EUR"
        protoMoney.units shouldBeEqualTo 9L
    }

    @Test
    fun `왕복 변환 - ProtoMoney → JavaMoney → ProtoMoney`() {
        val original =
            ProtoMoney
                .newBuilder()
                .setCurrencyCode("KRW")
                .setUnits(1000L)
                .setNanos(0)
                .build()

        val restored = original.toJavaMoney().toProtoMoney()

        restored.currencyCode shouldBeEqualTo original.currencyCode
        restored.units shouldBeEqualTo original.units
    }

    @Test
    fun `왕복 변환 - JavaMoney → ProtoMoney → JavaMoney`() {
        val original = Money.of(java.math.BigDecimal("5.00"), "JPY")

        val restored = original.toProtoMoney().toJavaMoney()

        restored.currency.currencyCode shouldBeEqualTo original.currency.currencyCode
    }
}
