package io.bluetape4k.functional

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class CurryingSupportTest {

    companion object: KLogging()

    @Test
    fun `currying two input function`() {
        val func: (Int, Int) -> Int = { a, b -> a + b }

        val curried = func.curried()

        curried(1)(2) shouldBeEqualTo 3
    }

    @Test
    fun `currying three input function`() {
        val func: (Int, Int, Int) -> Int = { a, b, c -> a + b + c }

        val curried = func.curried()

        val curried2 = curried(1)(2)

        curried2(3) shouldBeEqualTo 6
        curried2(6) shouldBeEqualTo 9
    }

    @Test
    fun `currying four input function`() {
        val func: (Int, Int, Int, Int) -> Int = { a, b, c, d -> a + b + c + d }

        val curried1 = func.curried()
        val curried2 = curried1(1)
        val curried3 = curried2(2)
        val curried4 = curried3(3)

        curried4(4) shouldBeEqualTo 10
        curried4(6) shouldBeEqualTo 12

        curried3(3)(6) shouldBeEqualTo 12
    }
}
