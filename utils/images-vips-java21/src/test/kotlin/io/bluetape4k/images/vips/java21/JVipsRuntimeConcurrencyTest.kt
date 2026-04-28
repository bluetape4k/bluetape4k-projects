package io.bluetape4k.images.vips.java21

import io.bluetape4k.images.vips.java21.internal.DefaultJVipsNativeRuntime
import io.bluetape4k.images.vips.java21.internal.JVipsNativeRuntime
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * `JVipsRuntime.init()` 동시 호출이 정확히 1회만 native init을 실행하는지 검증합니다.
 *
 * `JVipsNativeRuntime` 어댑터 심을 사용하여 실제 libvips 없이 테스트합니다.
 */
class JVipsRuntimeConcurrencyTest {

    companion object : KLogging()

    private val initCount = AtomicInteger(0)

    private val testAdapter = object : JVipsNativeRuntime {
        override fun nativeInit(concurrency: Int) { initCount.incrementAndGet() }
        override fun nativeShutdown() {}
    }

    @BeforeEach
    fun setup() {
        JVipsRuntime.resetForTest()
        JVipsRuntime.nativeRuntime = testAdapter
        initCount.set(0)
    }

    @AfterEach
    fun teardown() {
        JVipsRuntime.resetForTest()
        JVipsRuntime.nativeRuntime = DefaultJVipsNativeRuntime
    }

    @Test
    fun `concurrent init calls native init exactly once`() {
        val concurrency = 10

        runBlocking(Dispatchers.Default) {
            repeat(concurrency) {
                launch { JVipsRuntime.init() }
            }
        }

        initCount.get() shouldBeEqualTo 1
        JVipsRuntime.isInitialized.shouldBeTrue()
    }

    @Test
    fun `repeated sequential init is idempotent`() {
        JVipsRuntime.init()
        JVipsRuntime.init()
        JVipsRuntime.init()

        initCount.get() shouldBeEqualTo 1
        JVipsRuntime.isInitialized.shouldBeTrue()
    }
}
