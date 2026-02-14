package io.bluetape4k.exposed.r2dbc

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueryExtensionsTest {

    @Test
    fun `any는 요소가 있으면 true를 반환한다`() = runTest {
        assertTrue(flowOf(1, 2, 3).any())
    }

    @Test
    fun `any는 요소가 없으면 false를 반환한다`() = runTest {
        assertFalse(flowOf<Int>().any())
    }

    @Test
    fun `sorted는 오름차순 정렬된 리스트를 반환한다`() = runTest {
        val result = flowOf(3, 1, 2).sorted()
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun `distinct는 전체 중복을 제거한 리스트를 반환한다`() = runTest {
        val result = flowOf(1, 2, 1, 3, 2, 3, 3).distinct()
        assertEquals(listOf(1, 2, 3), result)
    }
}
