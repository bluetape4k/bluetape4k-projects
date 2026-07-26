package io.bluetape4k.coroutines

import io.bluetape4k.assertions.assertFailsWith
import org.junit.jupiter.api.Test

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
