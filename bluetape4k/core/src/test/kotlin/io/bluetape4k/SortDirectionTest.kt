package io.bluetape4k

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class SortDirectionTest {

    @Test
    fun `of는 양수는 ASC 음수와 0은 DESC를 반환한다`() {
        SortDirection.of(1) shouldBeEqualTo SortDirection.ASC
        SortDirection.of(Int.MAX_VALUE) shouldBeEqualTo SortDirection.ASC
        SortDirection.of(0) shouldBeEqualTo SortDirection.DESC
        SortDirection.of(-1) shouldBeEqualTo SortDirection.DESC
        SortDirection.of(Int.MIN_VALUE) shouldBeEqualTo SortDirection.DESC
    }

    @Test
    fun `direction 값은 1 또는 -1이다`() {
        SortDirection.ASC.direction shouldBeEqualTo 1
        SortDirection.DESC.direction shouldBeEqualTo -1
    }
}
