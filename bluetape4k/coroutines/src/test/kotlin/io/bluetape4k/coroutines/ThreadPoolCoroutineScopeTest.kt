package io.bluetape4k.coroutines

import io.bluetape4k.utils.ShutdownQueue
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ThreadPoolCoroutineScopeTest: AbstractCoroutineScopeTest() {

    override val coroutineScope: CoroutineScope = ThreadPoolCoroutineScope()
        .apply {
            ShutdownQueue.register(this)
        }

    @Test
    fun `pool size는 0보다 커야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            ThreadPoolCoroutineScope(poolSize = 0)
        }
    }
}
