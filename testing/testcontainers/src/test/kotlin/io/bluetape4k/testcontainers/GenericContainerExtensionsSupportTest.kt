package io.bluetape4k.testcontainers

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GenericContainerExtensionsSupportTest {

    @Test
    fun `resolvePortBindings 는 중복 포트를 제거한다`() {
        val bindings = resolvePortBindings(listOf(8080, 8080, 9090))

        assertEquals(2, bindings.size)
        assertEquals("8080", bindings[0].binding.hostPortSpec)
        assertEquals("9090", bindings[1].binding.hostPortSpec)
    }

    @Test
    fun `resolvePortBindings 는 0 이하 포트를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            resolvePortBindings(listOf(0, 8080))
        }
    }

    @Test
    fun `resolvePortBindings 는 빈 입력이면 빈 바인딩을 반환한다`() {
        val bindings = resolvePortBindings(emptyList())
        assertTrue(bindings.isEmpty())
    }

    @Test
    fun `resolvePortBindings 는 첫 등장 순서를 유지하며 중복을 제거한다`() {
        val bindings = resolvePortBindings(listOf(9092, 8080, 9092, 8080, 19092))

        assertEquals(3, bindings.size)
        assertEquals("9092", bindings[0].binding.hostPortSpec)
        assertEquals("8080", bindings[1].binding.hostPortSpec)
        assertEquals("19092", bindings[2].binding.hostPortSpec)
    }
}
