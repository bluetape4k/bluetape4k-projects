package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

abstract class AbstractVirtualThreadTest {

    companion object: KLogging() {
        @JvmStatic
        val faker = Fakers.faker
    }

    protected fun sleep(millis: Int) {
        TimeUnit.MILLISECONDS.sleep(millis.toLong())
    }

    protected fun <T> sleepAndGet(millis: Int, value: T): T {
        log.debug { "[BLOCK] $value started" }
        sleep(millis)
        log.debug { "[BLOCK] $value finished" }
        return value
    }

    protected suspend fun <T> sleepAndAwait(millis: Int, value: T): T {
        log.debug { "[SUSPEND] $value started" }
        delay(millis.toLong())
        log.debug { "[SUSPEND] $value finished" }
        return value
    }
}
