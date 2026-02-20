package io.bluetape4k.apache

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

private enum class SampleEnum {
    ONE,
    TWO
}

class ApacheEnumUtilsTest {

    @Test
    fun `getEnumIgnoreCase 는 대소문자 무시 조회`() {
        SampleEnum::class.getEnumIgnoreCase("one").shouldNotBeNull() shouldBeEqualTo SampleEnum.ONE
        SampleEnum::class.getEnumIgnoreCase("missing", SampleEnum.TWO) shouldBeEqualTo SampleEnum.TWO
    }

    @Test
    fun `isValidEnumIgnoreCase 는 존재 여부를 판단`() {
        SampleEnum::class.isValidEnumIgnoreCase("two").shouldBeTrue()
        SampleEnum::class.isValidEnumIgnoreCase("three").not().shouldBeTrue()
    }

    @Test
    fun `getEnumList 와 getEnumMap 은 모든 값을 반환`() {
        SampleEnum::class.getEnumList() shouldBeEqualTo listOf(SampleEnum.ONE, SampleEnum.TWO)
        SampleEnum::class.getEnumMap()["ONE"] shouldBeEqualTo SampleEnum.ONE
    }
}
