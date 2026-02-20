package io.bluetape4k.utils

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.junit.jupiter.api.Test
import java.io.Closeable

class ShutdownQueueTest {

    companion object: KLogging()

    class CleanUp(private val name: String): Closeable {
        override fun close() {
            log.debug { "Close CleanUp instance. [$name]" }
            Thread.sleep(100L)
        }

        override fun toString(): String = "CleanUp($name)"
    }

    @Test
    fun `regist closeable to shutdown queue`() {
        ShutdownQueue.register(CleanUp("First"))
        ShutdownQueue.register(CleanUp("Second"))
        ShutdownQueue.register(CleanUp("Third"))
    }

    @Test
    fun `중복 등록은 한 번만 처리된다`() {
        val closeable = CleanUp("Dup")
        ShutdownQueue.register(closeable)
        ShutdownQueue.register(closeable)
    }

    @Test
    fun `regist custom statement`() {
        ShutdownQueue.register { println("Close Anonymous Closeable instance.") }
    }

    @Test
    fun `regist no drived AutoCloseable 도 함수로 등록하면 실행된다`() {
        val shutdownable = Shutdownable {
            log.debug { "Shutdown instance." }
        }
        ShutdownQueue.register {
            shutdownable.shutdown()
        }
    }

    /**
     * Shutdownable
     *
     * 참고 [Functional interface](https://kotlinlang.org/docs/fun-interfaces.html)
     */
    fun interface Shutdownable {
        fun shutdown()
    }
}
