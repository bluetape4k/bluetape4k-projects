package io.bluetape4k.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class SilentSupervisorTest {

    @Test
    fun `SilentSupervisor는 한 자식의 실패가 다른 자식을 취소하지 않는다`() = runTest {
        val scope = CoroutineScope(coroutineContext + SilentSupervisor())
        val counter = AtomicInteger(0)

        val failed = scope.launch {
            throw IllegalStateException("boom")
        }
        val succeeded = scope.launch {
            delay(10)
            counter.incrementAndGet()
        }

        joinAll(failed, succeeded)
        counter.get() shouldBeEqualTo 1
    }
}

