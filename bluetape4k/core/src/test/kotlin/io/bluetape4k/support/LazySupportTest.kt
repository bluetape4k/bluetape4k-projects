package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.junit.jupiter.api.Test
import java.util.concurrent.Callable
import java.util.concurrent.Executors
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

        val executor = Executors.newFixedThreadPool(8)
        try {
            val tasks = List(16) { Callable { lazyValue } }
            val results = executor.invokeAll(tasks).map { it.get() }.distinct()

            results.size shouldBeEqualTo 1
            results.first() shouldBeEqualTo "value"
            initialized.get() shouldBeGreaterOrEqualTo 1
        } finally {
            executor.shutdownNow()
        }
    }
}

