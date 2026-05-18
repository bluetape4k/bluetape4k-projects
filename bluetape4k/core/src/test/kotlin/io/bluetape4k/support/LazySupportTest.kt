package io.bluetape4k.support

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class LazySupportTest {

    @Test
    fun `unsafeLazy는 동일 인스턴스에서 값을 캐시한다`() {
        val initialized = AtomicInteger(0)
        val lazyValue by unsafeLazy {
            initialized.incrementAndGet()
            "value"
        }

        lazyValue shouldBeEqualTo "value"
        lazyValue shouldBeEqualTo "value"
        initialized.get() shouldBeEqualTo 1
    }

    @Test
    fun `publicLazy는 동시 접근 시 최종 값은 동일하다`() {
        val initialized = AtomicInteger(0)
        val lazyValue by publicLazy {
            initialized.incrementAndGet()
            "value"
        }

        MultithreadingTester()
            .workers(8)
            .rounds(2)
            .add { lazyValue shouldBeEqualTo "value" }
            .run()

        initialized.get() shouldBeGreaterOrEqualTo 1
    }
}

