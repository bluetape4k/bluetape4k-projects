package io.bluetape4k.exposed.core

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

/**
 * [ExposedPage] 단위 테스트입니다.
 */
class ExposedPageTest {
    @Test
    fun `totalPages는 totalCount와 pageSize로 올림 나눗셈을 계산한다`() {
        ExposedPage(listOf(1, 2), totalCount = 10L, pageNumber = 0, pageSize = 2).totalPages shouldBeEqualTo 5
        ExposedPage(listOf(1), totalCount = 11L, pageNumber = 0, pageSize = 2).totalPages shouldBeEqualTo 6
        ExposedPage(listOf(1), totalCount = 1L, pageNumber = 0, pageSize = 10).totalPages shouldBeEqualTo 1
    }

    @Test
    fun `pageSize가 0이면 totalPages는 0이다`() {
        ExposedPage(emptyList<Int>(), totalCount = 100L, pageNumber = 0, pageSize = 0).totalPages shouldBeEqualTo 0
    }

    @Test
    fun `totalCount가 0이면 totalPages는 0이다`() {
        ExposedPage(emptyList<Int>(), totalCount = 0L, pageNumber = 0, pageSize = 10).totalPages shouldBeEqualTo 0
    }

    @Test
    fun `isFirst는 첫 페이지일 때 true다`() {
        ExposedPage(listOf(1), totalCount = 10L, pageNumber = 0, pageSize = 5).isFirst.shouldBeTrue()
        ExposedPage(listOf(1), totalCount = 10L, pageNumber = 1, pageSize = 5).isFirst.shouldBeFalse()
    }

    @Test
    fun `isLast는 마지막 페이지일 때 true다`() {
        ExposedPage(listOf(1), totalCount = 10L, pageNumber = 1, pageSize = 5).isLast.shouldBeTrue()
        ExposedPage(listOf(1), totalCount = 10L, pageNumber = 0, pageSize = 5).isLast.shouldBeFalse()
    }

    @Test
    fun `hasNext는 다음 페이지가 있을 때 true다`() {
        ExposedPage(listOf(1), totalCount = 10L, pageNumber = 0, pageSize = 5).hasNext.shouldBeTrue()
        ExposedPage(listOf(1), totalCount = 10L, pageNumber = 1, pageSize = 5).hasNext.shouldBeFalse()
    }

    @Test
    fun `hasPrevious는 이전 페이지가 있을 때 true다`() {
        ExposedPage(listOf(1), totalCount = 10L, pageNumber = 1, pageSize = 5).hasPrevious.shouldBeTrue()
        ExposedPage(listOf(1), totalCount = 10L, pageNumber = 0, pageSize = 5).hasPrevious.shouldBeFalse()
    }

    @Test
    fun `단일 페이지면 isFirst, isLast 모두 true다`() {
        val page = ExposedPage(listOf(1, 2), totalCount = 2L, pageNumber = 0, pageSize = 10)
        page.isFirst.shouldBeTrue()
        page.isLast.shouldBeTrue()
        page.hasNext.shouldBeFalse()
        page.hasPrevious.shouldBeFalse()
    }

    @Test
    fun `content는 주어진 리스트를 그대로 보관한다`() {
        val data = listOf("a", "b", "c")
        val page = ExposedPage(data, totalCount = 3L, pageNumber = 0, pageSize = 10)
        page.content shouldBeEqualTo data
    }

    @Test
    fun `totalCount가 0이고 pageNumber가 0이면 isFirst와 isLast 모두 true다`() {
        val page = ExposedPage(emptyList<Int>(), totalCount = 0L, pageNumber = 0, pageSize = 10)
        page.isFirst.shouldBeTrue()
        page.isLast.shouldBeTrue()
        page.hasNext.shouldBeFalse()
        page.hasPrevious.shouldBeFalse()
    }

    @Test
    fun `pageSize가 1이면 totalPages는 totalCount와 같다`() {
        ExposedPage(listOf(1), totalCount = 5L, pageNumber = 0, pageSize = 1).totalPages shouldBeEqualTo 5
    }

    @Test
    fun `totalCount가 pageSize의 정확한 배수이면 나머지 없이 계산된다`() {
        ExposedPage(listOf(1), totalCount = 6L, pageNumber = 0, pageSize = 3).totalPages shouldBeEqualTo 2
    }
}
