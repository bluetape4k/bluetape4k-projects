package io.bluetape4k.functional

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class DecoratorSupportTest {

    companion object: KLogging()

    @Test
    fun `하나의 문자열에 대해 decorate 를 수행합니다`() {
        fun italic(f: () -> String) = "<i>${f()}</i>"

        val decorated = decorateWith(::italic) { "hello" }

        decorated shouldBeEqualTo "<i>hello</i>"
    }

    @Test
    fun `하나의 문자열에 대해 복수 개의 decorate 를 수행합니다`() {
        fun italic(f: () -> String) = "<i>${f()}</i>"
        fun bold(f: () -> String) = "<b>${f()}</b>"
        fun underline(f: () -> String) = "<u>${f()}</u>"

        val decorated = decorateWith(::italic, ::bold, ::underline) { "hello" }

        decorated shouldBeEqualTo "<u><b><i>hello</i></b></u>"
    }

    @Test
    fun `Int 인자 하나를 이용하여 decorate 하기`() {

        // 짝수면 그대로 반환하고 홀수면 0 을 반환하는 함수
        fun findEvenOrZero(f: () -> Int): Int {
            val value = f()
            return if (value % 2 == 0) value else 0
        }

        fun decorated(x: Int, y: Int) = decorateWith(::findEvenOrZero) { x + y }

        decorated(1, 1) shouldBeEqualTo 2
        decorated(1, 2) shouldBeEqualTo 0
    }
}
