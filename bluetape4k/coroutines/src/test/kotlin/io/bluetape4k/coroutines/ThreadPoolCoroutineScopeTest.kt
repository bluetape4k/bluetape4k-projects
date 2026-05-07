package io.bluetape4k.coroutines

import org.junit.jupiter.api.Test
import io.bluetape4k.assertions.assertFailsWith

class ThreadPoolCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override fun getCoroutineScope(): CloseableCoroutineScope =
        ThreadPoolCoroutineScope()

    @Test
    fun `pool size는 0보다 커야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            ThreadPoolCoroutineScope(poolSize = 0)
        }
    }
}
