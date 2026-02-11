package io.bluetape4k.support

import io.bluetape4k.AbstractCoreTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test

class ThrowableSupportTest: AbstractCoreTest() {

    companion object: KLogging()

    @Test
    fun `build throwable message`() {
        val ex = IllegalStateException("Something went wrong")
        val message = ex.buildMessage("Failed to do something")

        log.debug { "error message: $message" }
        message shouldBeEqualTo "Failed to do something; nested exception is not exists"
    }

    @Test
    fun `get root cause exception`() {
        val innerEx = IllegalStateException("Exception 1")
        val outerEx = RuntimeException("Exception 2", innerEx)

        innerEx.getRootCause().shouldBeNull()
        outerEx.getRootCause() shouldBeEqualTo innerEx
    }

    @Test
    fun `get most specific cause exception`() {
        val innerEx = IllegalStateException("Exception 1")
        val outerEx = RuntimeException("Exception 2", innerEx)

        innerEx.getMostSpecificCause() shouldBeEqualTo innerEx
        outerEx.getMostSpecificCause() shouldBeEqualTo innerEx
    }
}
