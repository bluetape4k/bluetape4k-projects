package io.bluetape4k.collections

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class PaginatedListTest {

    companion object: KLogging()

    @Test
    fun `기본 생성 및 속성 확인`() {
        val list = SimplePaginatedList(
            contents = listOf("a", "b", "c"),
            pageNo = 0,
            pageSize = 10,
            totalItemCount = 25,
        )

        list.contents shouldBeEqualTo listOf("a", "b", "c")
        list.pageNo shouldBeEqualTo 0
        list.pageSize shouldBeEqualTo 10
        list.totalItemCount shouldBeEqualTo 25L
    }

    @Test
    fun `totalPageCount - 나머지 있는 경우`() {
        val list = SimplePaginatedList(
            contents = listOf("a"),
            totalItemCount = 25,
            pageSize = 10,
        )

        list.totalPageCount shouldBeEqualTo 3L
    }

    @Test
    fun `totalPageCount - 나누어 떨어지는 경우`() {
        val list = SimplePaginatedList(
            contents = listOf("a"),
            totalItemCount = 20,
            pageSize = 10,
        )

        list.totalPageCount shouldBeEqualTo 2L
    }

    @Test
    fun `totalPageCount - 항목이 0개인 경우`() {
        val list = SimplePaginatedList(
            contents = emptyList<String>(),
            totalItemCount = 0,
            pageSize = 10,
        )

        list.totalPageCount shouldBeEqualTo 0L
    }

    @Test
    fun `기본값 적용 확인`() {
        val list = SimplePaginatedList(
            contents = listOf(1, 2, 3),
            totalItemCount = 100,
        )

        list.pageNo shouldBeEqualTo 0
        list.pageSize shouldBeEqualTo 10
        list.totalPageCount shouldBeEqualTo 10L
    }

    @Test
    fun `contents 접근`() {
        val items = (1..5).toList()
        val list = SimplePaginatedList(
            contents = items,
            pageNo = 2,
            pageSize = 5,
            totalItemCount = 50,
        )

        list.contents.size shouldBeEqualTo 5
        list.contents shouldBeEqualTo items
    }
}
