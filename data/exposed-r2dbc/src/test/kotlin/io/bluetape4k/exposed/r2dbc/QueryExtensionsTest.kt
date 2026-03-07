package io.bluetape4k.exposed.r2dbc

import kotlinx.coroutines.flow.emptyFlow
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
    fun `any는 단일 요소 Flow에서 true를 반환한다`() = runTest {
        assertTrue(flowOf(42).any())
    }

    @Test
    fun `sorted는 오름차순 정렬된 리스트를 반환한다`() = runTest {
        val result = flowOf(3, 1, 2).sorted()
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun `sorted는 빈 Flow에서 빈 리스트를 반환한다`() = runTest {
        val result = emptyFlow<Int>().sorted()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `sorted는 단일 요소 Flow에서 동일한 리스트를 반환한다`() = runTest {
        val result = flowOf(7).sorted()
        assertEquals(listOf(7), result)
    }

    @Test
    fun `distinct는 전체 중복을 제거한 리스트를 반환한다`() = runTest {
        val result = flowOf(1, 2, 1, 3, 2, 3, 3).distinct()
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun `distinct는 빈 Flow에서 빈 리스트를 반환한다`() = runTest {
        val result = emptyFlow<Int>().distinct()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `distinct는 중복 없는 Flow에서 원본 순서를 유지한다`() = runTest {
        val result = flowOf("b", "a", "c").distinct()
        assertEquals(listOf("b", "a", "c"), result)
    }
}
