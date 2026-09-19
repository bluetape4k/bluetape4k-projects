package io.bluetape4k.science.coords

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldNotBeEqualTo
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class DmsTest {

    companion object: KLogging()

    @Test
    fun `DMS 데이터 클래스가 올바르게 생성된다`() {
        val dms = DMS(degree = 126, minute = 58, second = 40.8)
        dms.degree shouldBeEqualTo 126
        dms.minute shouldBeEqualTo 58
        dms.second shouldBeEqualTo 40.8
    }

    @Test
    fun `DMS equality가 올바르게 동작한다`() {
        val a = DMS(37, 33, 57.54)
        val b = DMS(37, 33, 57.54)
        a shouldBeEqualTo b
    }

    @Test
    fun `DMS 다른 값은 equal하지 않다`() {
        val a = DMS(37, 33, 57.54)
        val b = DMS(37, 33, 58.00)
        a shouldNotBeEqualTo b
    }

    @Test
    fun `DMS copy가 올바르게 동작한다`() {
        val original = DMS(37, 33, 57.54)
        val copy = original.copy(second = 0.0)
        copy.degree shouldBeEqualTo 37
        copy.minute shouldBeEqualTo 33
        copy.second shouldBeEqualTo 0.0
        original shouldNotBeEqualTo copy
    }

    @Test
    fun `DMS compareTo - 더 큰 초는 크다`() {
        val a = DMS(37, 33, 57.54)
        val b = DMS(37, 33, 58.00)
        a shouldBeLessThan b
        b shouldBeGreaterThan a
    }

    @Test
    fun `DMS compareTo - 더 큰 분은 크다`() {
        val a = DMS(37, 33, 59.9)
        val b = DMS(37, 34, 0.0)
        a shouldBeLessThan b
    }

    @Test
    fun `DMS compareTo - 더 큰 도는 크다`() {
        val a = DMS(36, 59, 59.9)
        val b = DMS(37, 0, 0.0)
        a shouldBeLessThan b
    }

    @Test
    fun `DMS compareTo - 같은 값은 0을 반환한다`() {
        val a = DMS(37, 33, 57.54)
        val b = DMS(37, 33, 57.54)
        a.compareTo(b) shouldBeEqualTo 0
    }

    @Test
    fun `DMS compareTo - 도가 다르면 도 기준으로 비교한다`() {
        val a = DMS(38, 0, 0.0)
        val b = DMS(37, 59, 59.9)
        a shouldBeGreaterThan b
    }

    @Test
    fun `DMS Serializable - 예외 없이 직렬화된다`() {
        val dms = DMS(126, 58, 40.8)
        val bytes = BinarySerializers.FastFory.serialize(dms)
        val restored = BinarySerializers.FastFory.deserialize<DMS>(bytes)
        restored shouldBeEqualTo dms
    }

    @Test
    fun `DMS hashCode가 동등한 객체에서 같다`() {
        val a = DMS(37, 33, 57.54)
        val b = DMS(37, 33, 57.54)
        a.hashCode() shouldBeEqualTo b.hashCode()
    }
}
