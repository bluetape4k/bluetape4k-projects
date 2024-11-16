package io.bluetape4k.junit5.output

import io.github.oshai.kotlinlogging.KotlinLogging
import org.amshove.kluent.shouldContain
import org.amshove.kluent.shouldNotContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder

@OutputCapture
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class OutputCaptureExtensionTest {

    companion object {
        private val log = KotlinLogging.logger { }
    }

    @BeforeEach
    fun beforeEach(capturer: OutputCapturer) {
        verifyOutput(capturer, "@#BeforeEach")
    }

    @AfterEach
    fun afterEach(capturer: OutputCapturer) {
        verifyOutput(capturer, "@#AfterEach")
    }

    @Test
    @Order(1)
    fun `capture system output`(capturer: OutputCapturer) {
        verifyOutput(capturer, "SYS OUT #1")
    }

    @Test
    @Order(2)
    fun `capture system error`(output: OutputCapturer) {
        verifyError(output, "SYS ERR #2")
    }

    @Test
    @Order(3)
    fun `capture system out and err`(output: OutputCapturer) {
        verifyOutput(output, "SYS OUT #3")
        verifyError(output, "SYS ERR #4")
    }

    private fun verifyOutput(capturer: OutputCapturer, expected: String) {
        capturer.capture() shouldNotContain expected

        println(expected)

        capturer.expect { it shouldContain expected }
        capturer.expect { it shouldNotContain expected.lowercase() }
    }

    private fun verifyError(capturer: OutputCapturer, expected: String) {
        capturer.capture() shouldNotContain expected

        System.err.println(expected)

        capturer.expect { it shouldContain expected }
        capturer.expect { it shouldNotContain expected.lowercase() }
    }
}
