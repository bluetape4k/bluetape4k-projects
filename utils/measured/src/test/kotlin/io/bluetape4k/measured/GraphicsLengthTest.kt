package io.bluetape4k.measured

import io.bluetape4k.junit5.random.RandomizedTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

@RandomizedTest
class GraphicsLengthTest {
    @Test
    fun `픽셀 단위가 동작한다`() {
        (10.pixels() `in` GraphicsLength.pixels) shouldBeEqualTo 10.0
    }

    @Test
    fun `픽셀 toHuman 이 표시된다`() {
        10.pixels().toHuman() shouldBeEqualTo "10.0 px"
    }
}
