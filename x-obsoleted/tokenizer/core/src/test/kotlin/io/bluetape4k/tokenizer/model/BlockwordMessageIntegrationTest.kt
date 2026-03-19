package io.bluetape4k.tokenizer.model

import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class BlockwordMessageIntegrationTest: AbstractCoreTest() {

    companion object: KLogging()

    @Test
    fun `complete blockword workflow`() {
        // 1. Create options
        val options =
            blockwordOptionsOf(
                mask = "*",
                severity = Severity.MIDDLE,
            )

        // 2. Create request
        val request =
            blockwordRequestOf(
                text = "This is a test message",
                options = options,
            )

        // 3. Verify request
        request.text shouldBeEqualTo "This is a test message"
        request.options.mask shouldBeEqualTo "*"
        request.options.severity shouldBeEqualTo Severity.MIDDLE

        // 4. Create response
        val response =
            blockwordResponseOf(
                request = request,
                maskedText = "This is a **** message",
                blockWords = listOf("test"),
            )

        // 5. Verify response
        response.request shouldBeEqualTo request
        response.maskedText shouldBeEqualTo "This is a **** message"
        response.blockWords shouldBeEqualTo listOf("test")
        response.blockwordExists.shouldBeTrue()
    }

    @Test
    fun `blockword request with default options`() {
        val request = blockwordRequestOf("test message")

        request.text shouldBeEqualTo "test message"
        request.options shouldBeEqualTo BlockwordOptions.DEFAULT
    }

    @Test
    fun `blockword response with no blocked words`() {
        val request = blockwordRequestOf("clean message")
        val response =
            blockwordResponseOf(
                request = request,
                maskedText = "clean message",
                blockWords = emptyList(),
            )

        response.blockwordExists.shouldBeFalse()
    }
}
