package io.bluetape4k.tokenizer.model

import com.fasterxml.jackson.module.kotlin.readValue
import io.bluetape4k.jackson.writeAsString
import io.bluetape4k.junit5.faker.Fakers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.tokenizer.AbstractCoreTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class BlockMessageTest: AbstractCoreTest() {

    companion object: KLogging()

    private fun newRequest(severity: Severity = Severity.MIDDLE): BlockwordRequest {
        return blockwordRequestOf(
            Fakers.randomString(16, 1024),
            BlockwordOptions(severity = severity)
        )
    }

    @Test
    fun `create request with empty text`() {
        assertFailsWith<IllegalArgumentException> {
            blockwordRequestOf("")
        }
        assertFailsWith<IllegalArgumentException> {
            blockwordRequestOf(" ")
        }
        assertFailsWith<IllegalArgumentException> {
            blockwordRequestOf("\t")
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert request to json`() {
        val expected = newRequest()
        val jsonText = mapper.writeAsString(expected)!!
        val actual = mapper.readValue<BlockwordRequest>(jsonText)

        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `convert response to json`() {
        val request = newRequest()
        val expected = BlockwordResponse(request, "Masked 문자열", listOf("욕설", "비속어"))

        val jsonText = mapper.writeAsString(expected)!!
        val actual = mapper.readValue<BlockwordResponse>(jsonText)

        actual shouldBeEqualTo expected
    }
}
