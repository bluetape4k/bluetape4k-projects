package io.bluetape4k.collections

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class ListSupportTest {

    @Test
    fun `int list by range`() {
        intRangeOf(1, 10) shouldBeEqualTo List(10) { it + 1 }
        intRangeOf(1, 0) shouldBeEqualTo emptyList()
    }

    @Test
    fun `intRangeOf make int list`() {
        intRangeOf(1..10) shouldBeEqualTo List(10) { it + 1 }
        intRangeOf(1 until 10) shouldBeEqualTo List(9) { it + 1 }
    }

    @Test
    fun `long list by range`() {
        longRangeOf(1, 10) shouldBeEqualTo List(10) { it + 1L }
        longRangeOf(1, 0) shouldBeEqualTo emptyList()
    }

    @Test
    fun `longRangeOf make int list`() {
        longRangeOf(1L..10) shouldBeEqualTo List(10) { it + 1L }
        longRangeOf(1L until 10) shouldBeEqualTo List(9) { it + 1L }
    }
}
