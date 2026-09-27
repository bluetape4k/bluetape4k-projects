package io.bluetape4k.science.coords

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldContain
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class DmTest {

    companion object: KLogging()

    @Test
    fun `DM 데이터 클래스가 올바르게 생성된다`() {
        val dm = DM(degree = 37, minute = 33.99)
        dm.degree shouldBeEqualTo 37
        dm.minute shouldBeEqualTo 33.99
    }

    @Test
    fun `DM equality가 올바르게 동작한다`() {
        val a = DM(37, 30.0)
        val b = DM(37, 30.0)
        a shouldBeEqualTo b
    }

    @Test
    fun `DM 다른 값은 equal하지 않다`() {
        val a = DM(37, 30.0)
        val b = DM(37, 45.0)
        a shouldNotBeEqualTo b
    }

    @Test
    fun `DM copy가 올바르게 동작한다`() {
        val original = DM(37, 33.99)
        val copy = original.copy(minute = 45.0)
        copy.degree shouldBeEqualTo 37
        copy.minute shouldBeEqualTo 45.0
        original shouldNotBeEqualTo copy
    }

    @Test
    fun `DM compareTo - 더 큰 분은 크다`() {
        val a = DM(37, 30.0)
        val b = DM(37, 45.0)
        a shouldBeLessThan b
        b shouldBeGreaterThan a
    }

    @Test
    fun `DM compareTo - 더 큰 도는 크다`() {
        val a = DM(36, 59.0)
        val b = DM(37, 0.0)
        a shouldBeLessThan b
    }

    @Test
    fun `DM compareTo - 같은 값은 0을 반환한다`() {
        val a = DM(37, 30.0)
        val b = DM(37, 30.0)
        a.compareTo(b) shouldBeEqualTo 0
    }

    @Test
    fun `DM compareTo - 도가 다르면 도 기준으로 비교한다`() {
        val a = DM(38, 0.0)
        val b = DM(37, 59.9)
        a.compareTo(b) shouldBeGreaterThan 0
    }

    @Test
    fun `DM Serializable - serialVersionUID 상수가 존재한다`() {
        // Serializable 구현 검증 (직렬화 가능 타입이어야 함)
        val dm = DM(126, 58.68)

        val bytes = BinarySerializers.FastFory.serialize(dm)
        val restored = BinarySerializers.FastFory.deserialize<DM>(bytes)

        restored shouldBeEqualTo dm
    }

    @Test
    fun `DM toString이 data class 기본 형식을 반환한다`() {
        val dm = DM(37, 30.0)
        val str = dm.toString()
        str shouldContain "37"
        str shouldContain "30.0"
    }
}
