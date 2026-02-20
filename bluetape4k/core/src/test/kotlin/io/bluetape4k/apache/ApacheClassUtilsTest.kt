package io.bluetape4k.apache

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

private class Outer {
    class Inner
}

class ApacheClassUtilsTest {

    @Test
    fun `클래스 이름 관련 헬퍼를 검증한다`() {
        String::class.getShortClassName() shouldBeEqualTo "String"
        String::class.getPackageName() shouldBeEqualTo "java.lang"
    }

    @Test
    fun `상속 계층과 인터페이스 조회`() {
        val interfaces = Number::class.getAllInterfaces()
        interfaces.isNotEmpty().shouldBeTrue()

        val supers = Integer::class.getAllSuperclasses()
        supers.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `내부 클래스와 assignable 확인`() {
        Outer.Inner::class.isInnerClass().shouldBeTrue()
        Integer::class.isAssignable(Number::class).shouldBeTrue()
    }
}
