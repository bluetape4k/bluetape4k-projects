package io.bluetape4k.concurrent.virtualthread

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class CompletableFutureSupportTest {

    companion object: KLogging()

    @Test
    fun `virtualFutureOf returns non-null result`() {
        val future = virtualFutureOf {
            Thread.sleep(100)
            42
        }
        future.get() shouldBeEqualTo 42
    }

    @Test
    fun `virtualFutureOf returns string result`() {
        val future = virtualFutureOf {
            "hello"
        }
        future.get() shouldBeEqualTo "hello"
    }

    @Test
    fun `virtualFutureOfNullable returns non-null result`() {
        val future = virtualFutureOfNullable {
            Thread.sleep(100)
            42
        }
        future.get() shouldBeEqualTo 42
    }

    @Test
    fun `virtualFutureOfNullable returns null result`() {
        val future = virtualFutureOfNullable<Int> {
            Thread.sleep(100)
            null
        }
        future.get().shouldBeNull()
    }

    @Test
    fun `virtualFutureOfNullable with string returns null`() {
        val future = virtualFutureOfNullable<String> {
            null
        }
        future.get().shouldBeNull()
    }

    @Test
    fun `virtualFutureOfNullable with string returns value`() {
        val future = virtualFutureOfNullable {
            "hello"
        }
        future.get() shouldBeEqualTo "hello"
    }
}
