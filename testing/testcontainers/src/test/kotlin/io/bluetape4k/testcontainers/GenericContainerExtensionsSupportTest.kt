package io.bluetape4k.testcontainers

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
